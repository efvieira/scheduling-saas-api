package schedulingsaas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import schedulingsaas.booking.ReminderJob;
import schedulingsaas.notification.LoggingWhatsAppGateway;
import schedulingsaas.tenant.DemoDataSeeder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchedulingIntegrationTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final String PUBLIC = "/api/public/" + DemoDataSeeder.SLUG;

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired LoggingWhatsAppGateway whatsApp;
    @Autowired ReminderJob reminders;

    private JsonNode page;

    @BeforeEach
    void loadPage() throws Exception {
        page = read(mvc.perform(get(PUBLIC)).andExpect(status().isOk()));
    }

    private JsonNode read(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private String id(String collection, String name) {
        for (var node : page.get(collection)) {
            if (node.get("name").asString().equals(name)) {
                return node.get("id").asString();
            }
        }
        throw new IllegalArgumentException(name);
    }

    private String token() throws Exception {
        var body = "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(DemoDataSeeder.OWNER_EMAIL, DemoDataSeeder.OWNER_PASSWORD);
        return read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())).get("accessToken").asString();
    }

    private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) throws Exception {
        return request.header("Authorization", "Bearer " + token()).contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode availability(String service, String professional, LocalDate date) throws Exception {
        return read(mvc.perform(get(PUBLIC + "/availability").param("serviceId", service)
                .param("professionalId", professional).param("date", date.toString())).andExpect(status().isOk()));
    }

    private org.springframework.test.web.servlet.ResultActions book(String service, String professional, String startAt, String phone) throws Exception {
        return mvc.perform(post(PUBLIC + "/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                {"serviceId": "%s", "professionalId": "%s", "startAt": "%s",
                 "customerName": "Cliente Teste", "customerPhone": "%s"}""".formatted(service, professional, startAt, phone)));
    }

    @Test
    void customerBooksFreeSlotAndSameSlotCannotBeTakenTwice() throws Exception {
        var corte = id("services", "Corte feminino");
        var ana = id("professionals", "Ana");
        var monday = LocalDate.now(SP).with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);

        var slots = availability(corte, ana, monday);
        assertThat(slots.size()).isGreaterThan(0);
        var first = slots.get(0).asString();

        var booked = read(book(corte, ana, first, "5511988887777").andExpect(status().isCreated()));
        assertThat(booked.get("status").asString()).isEqualTo("BOOKED");
        assertThat(whatsApp.sent()).anyMatch(m -> m.phone().equals("5511988887777") && m.text().contains("agendado"));

        book(corte, ana, first, "5511977776666").andExpect(status().isConflict());
        assertThat(availability(corte, ana, monday).toString()).doesNotContain(first);

        mvc.perform(post(PUBLIC + "/appointments/{id}/cancel", booked.get("id").asString()).param("phone", "000"))
                .andExpect(status().isNotFound());
        mvc.perform(post(PUBLIC + "/appointments/{id}/cancel", booked.get("id").asString()).param("phone", "5511988887777"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void prepaidServiceWaitsForPixWebhook() throws Exception {
        var coloracao = id("services", "Coloração");
        var carla = id("professionals", "Carla");
        var tuesday = LocalDate.now(SP).with(TemporalAdjusters.next(DayOfWeek.TUESDAY)).plusWeeks(1);
        var slot = availability(coloracao, carla, tuesday).get(0).asString();

        var booked = read(book(coloracao, carla, slot, "5511955554444").andExpect(status().isCreated()));
        assertThat(booked.get("status").asString()).isEqualTo("AWAITING_PAYMENT");
        assertThat(booked.get("pixCopiaECola").asString()).startsWith("000201");
        var txid = booked.get("pixCopiaECola").asString().substring("00020101021226...SIMULADO...".length());

        mvc.perform(post("/api/webhooks/pix").header("X-Webhook-Token", "errado")
                .contentType(MediaType.APPLICATION_JSON).content("{\"txid\": \"%s\"}".formatted(txid)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/webhooks/pix").header("X-Webhook-Token", "dev-only-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"txid\": \"%s\"}".formatted(txid)))
                .andExpect(status().isOk());

        mvc.perform(admin(get("/api/admin/appointments").param("date", tuesday.toString())))
                .andExpect(jsonPath("$[?(@.customerPhone == '5511955554444')].status").value("CONFIRMED"));
    }

    @Test
    void ownerManagesCatalogAndAgendaWithJwt() throws Exception {
        mvc.perform(get("/api/admin/services")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"errada\"}".formatted(DemoDataSeeder.OWNER_EMAIL)))
                .andExpect(status().isUnauthorized());

        mvc.perform(admin(post("/api/admin/services")).content(
                        "{\"name\": \"Manicure\", \"durationMinutes\": 40, \"price\": 45.00, \"prepaid\": false}"))
                .andExpect(status().isCreated());
        mvc.perform(admin(get("/api/admin/services"))).andExpect(jsonPath("$[?(@.name == 'Manicure')]").exists());

        var created = read(mvc.perform(admin(post("/api/admin/professionals")).content("""
                {"name": "Duda", "workingHours": [{"dayOfWeek": "WEDNESDAY", "start": "08:00", "end": "12:00"}]}"""))
                .andExpect(status().isCreated()));
        mvc.perform(admin(put("/api/admin/professionals/{id}/working-hours", created.get("id").asString())).content(
                        "[{\"dayOfWeek\": \"THURSDAY\", \"start\": \"12:00\", \"end\": \"08:00\"}]"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(admin(get("/api/admin/professionals"))).andExpect(jsonPath("$[?(@.name == 'Duda')]").exists());
    }

    @Test
    void remindersAreSentOnceForAppointmentsInTheNext24Hours() throws Exception {
        var token = token();
        var everyDay = new StringBuilder("[");
        for (var day : DayOfWeek.values()) {
            everyDay.append("{\"dayOfWeek\": \"%s\", \"start\": \"00:00\", \"end\": \"23:45\"},".formatted(day));
        }
        everyDay.setCharAt(everyDay.length() - 1, ']');
        var professional = read(mvc.perform(post("/api/admin/professionals").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Plantão\", \"workingHours\": %s}".formatted(everyDay)))
                .andExpect(status().isCreated())).get("id").asString();
        page = read(mvc.perform(get(PUBLIC)));
        var escova = id("services", "Escova");

        var today = LocalDate.now(SP);
        var slots = availability(escova, professional, today);
        var slot = (slots.isEmpty() ? availability(escova, professional, today.plusDays(1)) : slots).get(0).asString();
        var booked = read(book(escova, professional, slot, "5511933332222").andExpect(status().isCreated()));

        reminders.sendReminders();
        reminders.sendReminders();
        assertThat(whatsApp.sent().stream().filter(m -> m.phone().equals("5511933332222") && m.text().startsWith("Lembrete")))
                .hasSize(1);

        mvc.perform(patch("/api/admin/appointments/{id}", booked.get("id").asString()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"COMPLETED\"}"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(patch("/api/admin/appointments/{id}", booked.get("id").asString()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"CANCELLED\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void validatesPublicInput() throws Exception {
        mvc.perform(get("/api/public/nao-existe")).andExpect(status().isNotFound());
        book(id("services", "Escova"), id("professionals", "Ana"), "2020-01-01T10:00:00Z", "123")
                .andExpect(status().isBadRequest());
        book(id("services", "Escova"), id("professionals", "Ana"), "2020-01-06T13:00:00Z", "5511911112222")
                .andExpect(status().isConflict());
    }
}
