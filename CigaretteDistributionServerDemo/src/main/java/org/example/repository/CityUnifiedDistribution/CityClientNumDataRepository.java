package org.example.repository.CityUnifiedDistribution;

import org.example.entity.CityUnifiedDistribution.CityClientNumData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CityClientNumDataRepository extends JpaRepository<CityClientNumData, Integer> {
    Optional<CityClientNumData> findByUrbanRuralCode(String urbanRuralCode);
}