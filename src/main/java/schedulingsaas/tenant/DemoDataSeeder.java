package schedulingsaas.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import schedulingsaas.catalog.Professional;
import schedulingsaas.catalog.ProfessionalRepository;
import schedulingsaas.catalog.ServiceOffering;
import schedulingsaas.catalog.ServiceOfferingRepository;
import schedulingsaas.catalog.WorkingHours;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Cria a empresa de demonstração "Studio Bela" na primeira subida (demo.enabled=true). */
@Component
@ConditionalOnProperty(name = "demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String SLUG = "studio-bela";
    public static final String OWNER_EMAIL = "dona@studiobela.demo";
    public static final String OWNER_PASSWORD = "demo1234";

    private final BusinessRepository businesses;
    private final StaffUserRepository users;
    private final ServiceOfferingRepository services;
    private final ProfessionalRepository professionals;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(BusinessRepository businesses, StaffUserRepository users, ServiceOfferingRepository services,
                          ProfessionalRepository professionals, PasswordEncoder passwordEncoder) {
        this.businesses = businesses;
        this.users = users;
        this.services = services;
        this.professionals = professionals;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (businesses.findBySlug(SLUG).isPresent()) {
            return;
        }
        var business = businesses.save(new Business(SLUG, "Studio Bela", "America/Sao_Paulo", "5511999990000"));
        users.save(new StaffUser(business.getId(), "Bela Souza", OWNER_EMAIL, passwordEncoder.encode(OWNER_PASSWORD), StaffUser.Role.OWNER));
        services.saveAll(List.of(
                new ServiceOffering(business.getId(), "Corte feminino", 60, new BigDecimal("90.00"), false),
                new ServiceOffering(business.getId(), "Escova", 45, new BigDecimal("60.00"), false),
                new ServiceOffering(business.getId(), "Coloração", 120, new BigDecimal("220.00"), true)));
        professionals.saveAll(List.of(
                new Professional(business.getId(), "Ana", weekdays(LocalTime.of(9, 0), LocalTime.of(18, 0))),
                new Professional(business.getId(), "Carla", weekdays(LocalTime.of(13, 0), LocalTime.of(21, 0)))));
        log.info("Empresa demo criada: /b/{} (login {})", SLUG, OWNER_EMAIL);
    }

    private static List<WorkingHours> weekdays(LocalTime start, LocalTime end) {
        var hours = new ArrayList<WorkingHours>();
        for (var day : DayOfWeek.values()) {
            if (day != DayOfWeek.SUNDAY) {
                hours.add(new WorkingHours(day, start, end));
            }
        }
        return hours;
    }
}
