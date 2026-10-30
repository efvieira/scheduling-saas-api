package schedulingsaas.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import schedulingsaas.shared.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {

    public enum Status {
        /** Aguardando o Pix de um serviço pré-pago. */
        AWAITING_PAYMENT,
        BOOKED,
        CONFIRMED,
        COMPLETED,
        CANCELLED,
        NO_SHOW;

        /** Status que ocupam a agenda. */
        public static final Set<Status> ACTIVE = EnumSet.of(AWAITING_PAYMENT, BOOKED, CONFIRMED);
    }

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "pix_txid", length = 35)
    private String pixTxid;

    @Column(name = "pix_copia_e_cola", length = 512)
    private String pixCopiaECola;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Appointment() {
    }

    Appointment(UUID businessId, UUID professionalId, UUID serviceId, String customerName, String customerPhone,
                Instant startAt, Instant endAt, BigDecimal price, boolean prepaid, Instant now) {
        this.id = UUID.randomUUID();
        this.businessId = businessId;
        this.professionalId = professionalId;
        this.serviceId = serviceId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.startAt = startAt;
        this.endAt = endAt;
        this.price = price;
        this.status = prepaid ? Status.AWAITING_PAYMENT : Status.BOOKED;
        this.createdAt = now;
    }

    void attachPix(String txid, String copiaECola) {
        this.pixTxid = txid;
        this.pixCopiaECola = copiaECola;
    }

    void paymentConfirmed() {
        if (status == Status.AWAITING_PAYMENT) {
            status = Status.CONFIRMED;
        }
    }

    /** Transições permitidas pelo painel; finais (concluído, cancelado, falta) não mudam mais. */
    void moveTo(Status target) {
        var allowed = switch (status) {
            case AWAITING_PAYMENT -> EnumSet.of(Status.CANCELLED, Status.CONFIRMED);
            case BOOKED -> EnumSet.of(Status.CONFIRMED, Status.CANCELLED, Status.COMPLETED, Status.NO_SHOW);
            case CONFIRMED -> EnumSet.of(Status.CANCELLED, Status.COMPLETED, Status.NO_SHOW);
            case COMPLETED, CANCELLED, NO_SHOW -> EnumSet.noneOf(Status.class);
        };
        if (!allowed.contains(target)) {
            throw BusinessException.rule("Transição inválida: %s → %s".formatted(status, target));
        }
        status = target;
    }

    void reminderSent(Instant at) {
        reminderSentAt = at;
    }

    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public UUID getProfessionalId() { return professionalId; }
    public UUID getServiceId() { return serviceId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public Status getStatus() { return status; }
    public BigDecimal getPrice() { return price; }
    public String getPixTxid() { return pixTxid; }
    public String getPixCopiaECola() { return pixCopiaECola; }
    public Instant getReminderSentAt() { return reminderSentAt; }
}
