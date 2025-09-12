package org.example.repository;

import org.example.entity.DemoTestClientNumData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DemoTestClientNumDataRepository extends JpaRepository<DemoTestClientNumData, Integer> {
    
    List<DemoTestClientNumData> findAllByOrderByIdAsc();
}
