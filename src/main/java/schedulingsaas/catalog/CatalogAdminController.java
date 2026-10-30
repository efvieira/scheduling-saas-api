package schedulingsaas.catalog;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import schedulingsaas.auth.CurrentUser;
import schedulingsaas.shared.BusinessException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Painel: catálogo")
@SecurityRequirement(name = "bearer")
public class CatalogAdminController {

    public record ServiceRequest(@NotBlank String name, @Min(5) @Max(480) int durationMinutes,
                                 @NotNull @DecimalMin("0") BigDecimal price, boolean prepaid) {
    }

    public record HoursRequest(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime start, @NotNull LocalTime end) {
    }

    public record ProfessionalRequest(@NotBlank String name, @NotNull List<@Valid HoursRequest> workingHours) {
    }

    public record ServiceView(UUID id, String name, int durationMinutes, BigDecimal price, boolean prepaid) {
        public static ServiceView from(ServiceOffering s) {
            return new ServiceView(s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice(), s.isPrepaid());
        }
    }

    public record ProfessionalView(UUID id, String name, List<HoursRequest> workingHours) {
        public static ProfessionalView from(Professional p) {
            return new ProfessionalView(p.getId(), p.getName(), p.getWorkingHours().stream()
                    .map(h -> new HoursRequest(h.getDayOfWeek(), h.getStart(), h.getEnd())).toList());
        }
    }

    private final ServiceOfferingRepository services;
    private final ProfessionalRepository professionals;

    public CatalogAdminController(ServiceOfferingRepository services, ProfessionalRepository professionals) {
        this.services = services;
        this.professionals = professionals;
    }

    @GetMapping("/services")
    public List<ServiceView> services() {
        return services.findByBusinessIdAndActiveTrueOrderByName(CurrentUser.businessId()).stream().map(ServiceView::from).toList();
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceView createService(@Valid @RequestBody ServiceRequest r) {
        return ServiceView.from(services.save(new ServiceOffering(CurrentUser.businessId(), r.name(), r.durationMinutes(), r.price(), r.prepaid())));
    }

    @GetMapping("/professionals")
    public List<ProfessionalView> professionals() {
        return professionals.findByBusinessIdAndActiveTrueOrderByName(CurrentUser.businessId()).stream().map(ProfessionalView::from).toList();
    }

    @PostMapping("/professionals")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessionalView createProfessional(@Valid @RequestBody ProfessionalRequest r) {
        return ProfessionalView.from(professionals.save(new Professional(CurrentUser.businessId(), r.name(), toHours(r.workingHours()))));
    }

    @PutMapping("/professionals/{id}/working-hours")
    @Transactional
    public ProfessionalView updateHours(@PathVariable UUID id, @RequestBody List<@Valid HoursRequest> hours) {
        var professional = professionals.findByIdAndBusinessId(id, CurrentUser.businessId())
                .orElseThrow(() -> BusinessException.notFound("Profissional"));
        professional.replaceWorkingHours(toHours(hours));
        return ProfessionalView.from(professional);
    }

    private static List<WorkingHours> toHours(List<HoursRequest> hours) {
        try {
            return hours.stream().map(h -> new WorkingHours(h.dayOfWeek(), h.start(), h.end())).toList();
        } catch (IllegalArgumentException e) {
            throw BusinessException.rule(e.getMessage());
        }
    }
}
