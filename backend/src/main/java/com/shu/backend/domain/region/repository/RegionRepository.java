package com.shu.backend.domain.region.repository;

import com.shu.backend.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    boolean existsByName(String name);

    Optional<Region> findByName(String name);
}
