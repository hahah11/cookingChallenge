package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PlateColorJpaRepository extends JpaRepository<PlateColorJpaEntity, Long> {

    List<PlateColorJpaEntity> findByActiveTrueOrderBySortOrderAsc();
}
