package schedulingsaas.booking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import schedulingsaas.booking.Availability.Interval;
import schedulingsaas.catalog.Professional;
import schedulingsaas.catalog.ProfessionalRepository;
import schedulingsaas.catalog.ServiceOffering;
import schedulingsaas.catalog.ServiceOfferingRepository;
import schedulingsaas.notification.WhatsAppGateway;
import schedulingsaas.payment.PixGateway;
import schedulingsaas.shared.BusinessException;
import schedulingsaas.tenant.Business;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    static final Duration SLOT_STEP = Duration.ofMinutes(15);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    public record BookingRequest(UUID serviceId, UUID professionalId, Instant startAt, String customerName, String customerPhone) {
    }

    private final AppointmentRepository appointments;
    private final ProfessionalRepository professionals;
    private final ServiceOfferingRepository services;
    private final PixGateway pix;
    private final WhatsAppGateway whatsApp;
    private final Clock clock;

    public BookingService(AppointmentRepository appointments, ProfessionalRepository professionals,
                          ServiceOfferingRepository services, PixGateway pix, WhatsAppGateway whatsApp, Clock clock) {
        this.appointments = appointments;
        this.professionals = professionals;
        this.services = services;
        this.pix = pix;
        this.whatsApp = whatsApp;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Interval> availability(Business business, UUID serviceId, UUID professionalId, LocalDate date) {
        var service = service(business, serviceId);
        var professional = professionals.findByIdAndBusinessId(professionalId, business.getId())
                .orElseThrow(() -> BusinessException.notFound("Profissional"));
        return slots(business, professional, service, date);
    }

    /**
     * Agenda com lock pessimista no profissional: dois clientes disputando o mesmo horário são
     * serializados, e o segundo recebe 409 porque o horário já não aparece como livre.
     */
    @Transactional
    public Appointment book(Business business, BookingRequest request) {
        var service = service(business, request.serviceId());
        var professional = professionals.lockByIdAndBusinessId(request.professionalId(), business.getId())
                .orElseThrow(() -> BusinessException.notFound("Profissional"));
        var date = LocalDate.ofInstant(request.startAt(), business.getZone());
        var requested = new Interval(request.startAt(), request.startAt().plus(service.getDuration()));
        if (!slots(business, professional, service, date).contains(requested)) {
            throw BusinessException.conflict("Horário indisponível; escolha outro");
        }
        var appointment = appointments.save(new Appointment(business.getId(), professional.getId(), service.getId(),
                request.customerName(), request.customerPhone(), requested.start(), requested.end(),
                service.getPrice(), service.isPrepaid(), clock.instant()));

        var when = WHEN.format(requested.start().atZone(business.getZone()));
        if (service.isPrepaid()) {
            var charge = pix.createCharge(appointment.getId().toString(), service.getPrice(),
                    service.getName() + " - " + business.getName());
            appointment.attachPix(charge.txid(), charge.copiaECola());
            whatsApp.send(request.customerPhone(), "%s: reserva de %s em %s. Pague o Pix para confirmar: %s"
                    .formatted(business.getName(), service.getName(), when, charge.copiaECola()));
        } else {
            whatsApp.send(request.customerPhone(), "%s: %s agendado para %s com %s."
                    .formatted(business.getName(), service.getName(), when, professional.getName()));
        }
        return appointment;
    }

    @Transactional
    public Appointment confirmPayment(String txid) {
        var appointment = appointments.findByPixTxid(txid).orElseThrow(() -> BusinessException.notFound("Agendamento"));
        appointment.paymentConfirmed();
        return appointment;
    }

    @Transactional
    public Appointment cancelByCustomer(Business business, UUID appointmentId, String phone) {
        var appointment = appointments.findByIdAndBusinessId(appointmentId, business.getId())
                .filter(a -> a.getCustomerPhone().equals(phone))
                .orElseThrow(() -> BusinessException.notFound("Agendamento"));
        if (Duration.between(clock.instant(), appointment.getStartAt()).toHours() < 2) {
            throw BusinessException.rule("Cancelamento pelo cliente só até 2 horas antes");
        }
        appointment.moveTo(Appointment.Status.CANCELLED);
        return appointment;
    }

    @Transactional
    public Appointment changeStatus(UUID businessId, UUID appointmentId, Appointment.Status status) {
        var appointment = appointments.findByIdAndBusinessId(appointmentId, businessId)
                .orElseThrow(() -> BusinessException.notFound("Agendamento"));
        appointment.moveTo(status);
        return appointment;
    }

    private List<Interval> slots(Business business, Professional professional, ServiceOffering service, LocalDate date) {
        var zone = business.getZone();
        var dayStart = date.atStartOfDay(zone).toInstant();
        var dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        var busy = appointments.findOverlapping(professional.getId(), Appointment.Status.ACTIVE, dayStart, dayEnd).stream()
                .map(a -> new Interval(a.getStartAt(), a.getEndAt()))
                .toList();
        return Availability.freeSlots(date, zone, professional.hoursOn(date.getDayOfWeek()), busy,
                service.getDuration(), SLOT_STEP, clock.instant());
    }

    private ServiceOffering service(Business business, UUID serviceId) {
        return services.findByIdAndBusinessId(serviceId, business.getId())
                .filter(ServiceOffering::isActive)
                .orElseThrow(() -> BusinessException.notFound("Serviço"));
    }
}
