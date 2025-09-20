package org.example.repository.CountyDistribution;

import org.example.entity.CountyDistribution.DemoTestCountyClientNumData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DemoTestCountyClientNumDataRepository extends JpaRepository<DemoTestCountyClientNumData, Integer> {

    List<DemoTestCountyClientNumData> findAllByOrderByIdAsc();
}