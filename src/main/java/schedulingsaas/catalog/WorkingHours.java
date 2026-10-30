package schedulingsaas.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
public class WorkingHours {

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime start;

    @Column(name = "end_time", nullable = false)
    private LocalTime end;

    protected WorkingHours() {
    }

    public WorkingHours(DayOfWeek dayOfWeek, LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Fim do expediente deve ser depois do início");
        }
        this.dayOfWeek = dayOfWeek;
        this.start = start;
        this.end = end;
    }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
}
