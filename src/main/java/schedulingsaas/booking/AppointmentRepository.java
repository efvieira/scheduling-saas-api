package schedulingsaas.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("""
            select a from Appointment a
            where a.professionalId = :professionalId and a.status in :statuses
              and a.startAt < :end and a.endAt > :start""")
    List<Appointment> findOverlapping(UUID professionalId, Collection<Appointment.Status> statuses, Instant start, Instant end);

    List<Appointment> findByBusinessIdAndStartAtBetweenOrderByStartAt(UUID businessId, Instant start, Instant end);

    Optional<Appointment> findByIdAndBusinessId(UUID id, UUID businessId);

    Optional<Appointment> findByPixTxid(String txid);

    List<Appointment> findByStatusInAndReminderSentAtIsNullAndStartAtBetween(Collection<Appointment.Status> statuses,
                                                                            Instant from, Instant to);
}
