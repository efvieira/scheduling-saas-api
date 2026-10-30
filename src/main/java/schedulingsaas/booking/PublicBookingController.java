package schedulingsaas.booking;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import schedulingsaas.booking.BookingService.BookingRequest;
import schedulingsaas.catalog.CatalogAdminController.ServiceView;
import schedulingsaas.catalog.ProfessionalRepository;
import schedulingsaas.catalog.ServiceOfferingRepository;
import schedulingsaas.shared.BusinessException;
import schedulingsaas.tenant.Business;
import schedulingsaas.tenant.BusinessRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Página pública de agendamento: o cliente final não precisa de conta. */
@RestController
@RequestMapping("/api/public/{slug}")
@Tag(name = "Agendamento público")
public class PublicBookingController {

    public record ProfessionalSummary(UUID id, String name) {
    }

    public record BusinessPage(String slug, String name, String timezone, List<ServiceView> services,
                               List<ProfessionalSummary> professionals) {
    }

    public record BookRequest(@NotNull UUID serviceId, @NotNull UUID professionalId, @NotNull Instant startAt,
                              @NotBlank String customerName,
                              @Pattern(regexp = "\\+?\\d{12,13}", message = "use DDI + DDD + número, ex.: 5511999998888") String customerPhone) {
    }

    public record AppointmentView(UUID id, Instant startAt, Instant endAt, Appointment.Status status,
                                  BigDecimal price, String pixCopiaECola) {
        public static AppointmentView from(Appointment a) {
            return new AppointmentView(a.getId(), a.getStartAt(), a.getEndAt(), a.getStatus(), a.getPrice(), a.getPixCopiaECola());
        }
    }

    private final BusinessRepository businesses;
    private final ServiceOfferingRepository services;
    private final ProfessionalRepository professionals;
    private final BookingService booking;

    public PublicBookingController(BusinessRepository businesses, ServiceOfferingRepository services,
                                   ProfessionalRepository professionals, BookingService booking) {
        this.businesses = businesses;
        this.services = services;
        this.professionals = professionals;
        this.booking = booking;
    }

    @GetMapping
    public BusinessPage page(@PathVariable String slug) {
        var business = business(slug);
        return new BusinessPage(business.getSlug(), business.getName(), business.getZone().getId(),
                services.findByBusinessIdAndActiveTrueOrderByName(business.getId()).stream().map(ServiceView::from).toList(),
                professionals.findByBusinessIdAndActiveTrueOrderByName(business.getId()).stream()
                        .map(p -> new ProfessionalSummary(p.getId(), p.getName())).toList());
    }

    @GetMapping("/availability")
    @Operation(summary = "Horários livres de um profissional para um serviço num dia")
    public List<Instant> availability(@PathVariable String slug, @RequestParam UUID serviceId,
                                      @RequestParam UUID professionalId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return booking.availability(business(slug), serviceId, professionalId, date).stream()
                .map(Availability.Interval::start).toList();
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentView book(@PathVariable String slug, @Valid @RequestBody BookRequest r) {
        return AppointmentView.from(booking.book(business(slug),
                new BookingRequest(r.serviceId(), r.professionalId(), r.startAt(), r.customerName(), r.customerPhone())));
    }

    @PostMapping("/appointments/{id}/cancel")
    public AppointmentView cancel(@PathVariable String slug, @PathVariable UUID id, @RequestParam String phone) {
        return AppointmentView.from(booking.cancelByCustomer(business(slug), id, phone));
    }

    private Business business(String slug) {
        return businesses.findBySlug(slug).orElseThrow(() -> BusinessException.notFound("Empresa"));
    }
}
