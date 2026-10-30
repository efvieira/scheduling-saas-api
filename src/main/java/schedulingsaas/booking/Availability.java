package schedulingsaas.booking;

import schedulingsaas.catalog.WorkingHours;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Cálculo puro dos horários livres: percorre o expediente do dia em passos fixos e mantém
 * os intervalos que cabem inteiros no expediente, não colidem com agendamentos e não estão
 * no passado. Sem dependência de Spring, para ser testado isoladamente.
 */
public final class Availability {

    public record Interval(Instant start, Instant end) {
        boolean overlaps(Interval other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }
    }

    private Availability() {
    }

    public static List<Interval> freeSlots(LocalDate date, ZoneId zone, List<WorkingHours> hours, List<Interval> busy,
                                           Duration duration, Duration step, Instant notBefore) {
        var slots = new ArrayList<Interval>();
        for (var window : hours) {
            var windowEnd = date.atTime(window.getEnd()).atZone(zone).toInstant();
            for (var start = date.atTime(window.getStart()).atZone(zone).toInstant();
                 !start.plus(duration).isAfter(windowEnd);
                 start = start.plus(step)) {
                var candidate = new Interval(start, start.plus(duration));
                if (!start.isBefore(notBefore) && busy.stream().noneMatch(candidate::overlaps)) {
                    slots.add(candidate);
                }
            }
        }
        return slots;
    }
}
