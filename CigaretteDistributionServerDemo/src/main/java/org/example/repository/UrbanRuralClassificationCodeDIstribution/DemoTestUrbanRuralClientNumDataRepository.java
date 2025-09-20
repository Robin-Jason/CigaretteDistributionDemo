package org.example.repository.UrbanRuralClassificationCodeDIstribution;

import org.example.entity.UrbanRuralClassificationCodeDIstribution.DemoTestUrbanRuralClientNumData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DemoTestUrbanRuralClientNumDataRepository extends JpaRepository<DemoTestUrbanRuralClientNumData, Integer> {
    
    List<DemoTestUrbanRuralClientNumData> findAllByOrderByIdAsc();
}
