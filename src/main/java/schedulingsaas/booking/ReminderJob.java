package schedulingsaas.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import schedulingsaas.notification.WhatsAppGateway;
import schedulingsaas.tenant.BusinessRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;

/** Envia lembrete por WhatsApp para agendamentos das próximas 24 horas, uma única vez. */
@Component
public class ReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final AppointmentRepository appointments;
    private final BusinessRepository businesses;
    private final WhatsAppGateway whatsApp;
    private final Clock clock;

    public ReminderJob(AppointmentRepository appointments, BusinessRepository businesses,
                       WhatsAppGateway whatsApp, Clock clock) {
        this.appointments = appointments;
        this.businesses = businesses;
        this.whatsApp = whatsApp;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${reminders.interval:PT5M}")
    @Transactional
    public int sendReminders() {
        var now = clock.instant();
        var due = appointments.findByStatusInAndReminderSentAtIsNullAndStartAtBetween(
                EnumSet.of(Appointment.Status.BOOKED, Appointment.Status.CONFIRMED), now, now.plus(Duration.ofHours(24)));
        for (var appointment : due) {
            var business = businesses.findById(appointment.getBusinessId()).orElseThrow();
            try {
                whatsApp.send(appointment.getCustomerPhone(), "Lembrete %s: seu horário é %s. Para cancelar, responda CANCELAR."
                        .formatted(business.getName(), TIME.format(appointment.getStartAt().atZone(business.getZone()))));
                appointment.reminderSent(now);
            } catch (RuntimeException e) {
                // sem marcar como enviado: a próxima execução tenta de novo
                log.warn("Falha ao enviar lembrete do agendamento {}: {}", appointment.getId(), e.getMessage());
            }
        }
        return due.size();
    }
}
