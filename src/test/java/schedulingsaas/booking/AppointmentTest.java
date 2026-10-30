package schedulingsaas.booking;

import org.junit.jupiter.api.Test;
import schedulingsaas.shared.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    private Appointment appointment(boolean prepaid) {
        var start = Instant.parse("2026-03-16T13:00:00Z");
        return new Appointment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Joana", "5511988887777",
                start, start.plusSeconds(3600), BigDecimal.TEN, prepaid, Instant.EPOCH);
    }

    @Test
    void prepaidStartsAwaitingPaymentAndConfirmsOnPix() {
        var a = appointment(true);
        assertThat(a.getStatus()).isEqualTo(Appointment.Status.AWAITING_PAYMENT);
        a.paymentConfirmed();
        assertThat(a.getStatus()).isEqualTo(Appointment.Status.CONFIRMED);
    }

    @Test
    void finalStatusesCannotChange() {
        var a = appointment(false);
        a.moveTo(Appointment.Status.COMPLETED);
        assertThatThrownBy(() -> a.moveTo(Appointment.Status.CANCELLED)).isInstanceOf(BusinessException.class);
        a.paymentConfirmed();
        assertThat(a.getStatus()).isEqualTo(Appointment.Status.COMPLETED);
    }

    @Test
    void awaitingPaymentCannotBeMarkedAsNoShow() {
        assertThatThrownBy(() -> appointment(true).moveTo(Appointment.Status.NO_SHOW)).hasMessageContaining("Transição");
    }
}
