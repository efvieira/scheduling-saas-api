package schedulingsaas.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.ZoneId;
import java.util.UUID;

/** Empresa cliente do SaaS (tenant): salão, clínica, estúdio. */
@Entity
@Table(name = "businesses")
public class Business {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 40)
    private String timezone;

    @Column(length = 20)
    private String whatsapp;

    protected Business() {
    }

    public Business(String slug, String name, String timezone, String whatsapp) {
        this.id = UUID.randomUUID();
        this.slug = slug;
        this.name = name;
        this.timezone = timezone;
        this.whatsapp = whatsapp;
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public ZoneId getZone() { return ZoneId.of(timezone); }
    public String getWhatsapp() { return whatsapp; }
}
