package schedulingsaas.catalog;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {

    List<Professional> findByBusinessIdAndActiveTrueOrderByName(UUID businessId);

    Optional<Professional> findByIdAndBusinessId(UUID id, UUID businessId);

    /** Serializa agendamentos do mesmo profissional para evitar dois clientes no mesmo horário. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Professional p where p.id = :id and p.businessId = :businessId")
    Optional<Professional> lockByIdAndBusinessId(UUID id, UUID businessId);
}
