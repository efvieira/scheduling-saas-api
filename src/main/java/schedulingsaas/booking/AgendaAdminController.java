package schedulingsaas.booking;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schedulingsaas.auth.CurrentUser;
import schedulingsaas.catalog.Professional;
import schedulingsaas.catalog.ProfessionalRepository;
import schedulingsaas.catalog.ServiceOffering;
import schedulingsaas.catalog.ServiceOfferingRepository;
import schedulingsaas.tenant.BusinessRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/appointments")
@Tag(name = "Painel: agenda")
@SecurityRequirement(name = "bearer")
public class AgendaAdminController {

    public record AgendaItem(UUID id, Instant startAt, Instant endAt, String customerName, String customerPhone,
                             String service, String professional, Appointment.Status status, BigDecimal price) {
    }

    public record StatusChange(@NotNull Appointment.Status status) {
    }

    private record Names(Map<UUID, String> services, Map<UUID, String> professionals) {
        AgendaItem item(Appointment a) {
            return new AgendaItem(a.getId(), a.getStartAt(), a.getEndAt(), a.getCustomerName(), a.getCustomerPhone(),
                    services.getOrDefault(a.getServiceId(), "?"), professionals.getOrDefault(a.getProfessionalId(), "?"),
                    a.getStatus(), a.getPrice());
        }
    }

    private final AppointmentRepository appointments;
    private final BusinessRepository businesses;
    private final ServiceOfferingRepository services;
    private final ProfessionalRepository professionals;
    private final BookingService booking;

    public AgendaAdminController(AppointmentRepository appointments, BusinessRepository businesses,
                                 ServiceOfferingRepository services, ProfessionalRepository professionals,
                                 BookingService booking) {
        this.appointments = appointments;
        this.businesses = businesses;
        this.services = services;
        this.professionals = professionals;
        this.booking = booking;
    }

    @GetMapping
    public List<AgendaItem> day(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var businessId = CurrentUser.businessId();
        var zone = businesses.findById(businessId).orElseThrow().getZone();
        var names = names(businessId);
        return appointments.findByBusinessIdAndStartAtBetweenOrderByStartAt(businessId,
                        date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant())
                .stream().map(names::item).toList();
    }

    @PatchMapping("/{id}")
    public AgendaItem changeStatus(@PathVariable UUID id, @RequestBody StatusChange change) {
        var businessId = CurrentUser.businessId();
        return names(businessId).item(booking.changeStatus(businessId, id, change.status()));
    }

    private Names names(UUID businessId) {
        return new Names(
                services.findByBusinessIdAndActiveTrueOrderByName(businessId).stream()
                        .collect(Collectors.toMap(ServiceOffering::getId, ServiceOffering::getName)),
                professionals.findByBusinessIdAndActiveTrueOrderByName(businessId).stream()
                        .collect(Collectors.toMap(Professional::getId, Professional::getName)));
    }
}
