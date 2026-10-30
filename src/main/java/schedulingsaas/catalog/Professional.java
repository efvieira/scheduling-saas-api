package schedulingsaas.catalog;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Profissional que atende (cabeleireira, dentista, instrutor), com seu expediente semanal. */
@Entity
@Table(name = "professionals")
public class Professional {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "working_hours", joinColumns = @JoinColumn(name = "professional_id"))
    private List<WorkingHours> workingHours = new ArrayList<>();

    protected Professional() {
    }

    public Professional(UUID businessId, String name, List<WorkingHours> workingHours) {
        this.id = UUID.randomUUID();
        this.businessId = businessId;
        this.name = name;
        this.workingHours.addAll(workingHours);
    }

    public List<WorkingHours> hoursOn(DayOfWeek day) {
        return workingHours.stream().filter(h -> h.getDayOfWeek() == day).toList();
    }

    public void replaceWorkingHours(List<WorkingHours> hours) {
        workingHours.clear();
        workingHours.addAll(hours);
    }

    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public List<WorkingHours> getWorkingHours() { return Collections.unmodifiableList(workingHours); }
}
