package schedulingsaas.booking;

import org.junit.jupiter.api.Test;
import schedulingsaas.booking.Availability.Interval;
import schedulingsaas.catalog.WorkingHours;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate MONDAY = LocalDate.of(2026, 3, 16);
    private final List<WorkingHours> nineToTwelve = List.of(new WorkingHours(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

    private Instant at(int hour, int minute) {
        return MONDAY.atTime(hour, minute).atZone(SP).toInstant();
    }

    @Test
    void slotsMustFitEntirelyInsideWorkingHours() {
        var slots = Availability.freeSlots(MONDAY, SP, nineToTwelve, List.of(), Duration.ofMinutes(60),
                Duration.ofMinutes(30), Instant.EPOCH);

        assertThat(slots).extracting(Interval::start)
                .containsExactly(at(9, 0), at(9, 30), at(10, 0), at(10, 30), at(11, 0));
    }

    @Test
    void skipsSlotsOverlappingExistingAppointments() {
        var busy = List.of(new Interval(at(10, 0), at(10, 45)));
        var slots = Availability.freeSlots(MONDAY, SP, nineToTwelve, busy, Duration.ofMinutes(30),
                Duration.ofMinutes(15), Instant.EPOCH);

        assertThat(slots).extracting(Interval::start)
                .contains(at(9, 30), at(10, 45))
                .doesNotContain(at(9, 45), at(10, 0), at(10, 30));
    }

    @Test
    void neverOffersPastSlots() {
        var slots = Availability.freeSlots(MONDAY, SP, nineToTwelve, List.of(), Duration.ofMinutes(60),
                Duration.ofMinutes(60), at(10, 1));
        assertThat(slots).extracting(Interval::start).containsExactly(at(11, 0));
    }

    @Test
    void dayWithoutWorkingHoursHasNoSlots() {
        assertThat(Availability.freeSlots(MONDAY, SP, List.of(), List.of(), Duration.ofMinutes(30),
                Duration.ofMinutes(15), Instant.EPOCH)).isEmpty();
    }
}
