package schedulingsaas.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

/** Serviço oferecido (corte, consulta, aula), com duração e preço. */
@Entity
@Table(name = "services")
public class ServiceOffering {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Exige pagamento via Pix no ato do agendamento (reduz faltas). */
    @Column(name = "prepaid", nullable = false)
    private boolean prepaid;

    @Column(nullable = false)
    private boolean active = true;

    protected ServiceOffering() {
    }

    public ServiceOffering(UUID businessId, String name, int durationMinutes, BigDecimal price, boolean prepaid) {
        this.id = UUID.randomUUID();
        this.businessId = businessId;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.prepaid = prepaid;
    }

    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public String getName() { return name; }
    public Duration getDuration() { return Duration.ofMinutes(durationMinutes); }
    public int getDurationMinutes() { return durationMinutes; }
    public BigDecimal getPrice() { return price; }
    public boolean isPrepaid() { return prepaid; }
    public boolean isActive() { return active; }
}
