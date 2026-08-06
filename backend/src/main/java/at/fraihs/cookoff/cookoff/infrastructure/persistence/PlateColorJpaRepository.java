package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.PlateColorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PlateColorJpaRepository extends JpaRepository<PlateColorJpaEntity, Long> {

    List<PlateColorJpaEntity> findByActiveTrueOrderBySortOrderAsc();
}
