package schedulingsaas.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    List<ServiceOffering> findByBusinessIdAndActiveTrueOrderByName(UUID businessId);

    Optional<ServiceOffering> findByIdAndBusinessId(UUID id, UUID businessId);
}
