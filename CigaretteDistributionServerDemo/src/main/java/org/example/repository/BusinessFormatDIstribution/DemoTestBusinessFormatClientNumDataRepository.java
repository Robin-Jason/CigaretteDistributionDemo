package org.example.repository.BusinessFormatDIstribution;

import org.example.entity.BusinessFormatDIstribution.DemoTestBusinessFormatClientNumData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DemoTestBusinessFormatClientNumDataRepository extends JpaRepository<DemoTestBusinessFormatClientNumData, Integer> {
    
    List<DemoTestBusinessFormatClientNumData> findAllByOrderByIdAsc();
}
