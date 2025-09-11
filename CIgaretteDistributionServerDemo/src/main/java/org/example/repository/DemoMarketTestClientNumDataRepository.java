package org.example.repository;

import org.example.entity.DemoMarketTestClientNumData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemoMarketTestClientNumDataRepository extends JpaRepository<DemoMarketTestClientNumData, Integer> {

    List<DemoMarketTestClientNumData> findAllByOrderByUrbanRuralCodeAsc();
}