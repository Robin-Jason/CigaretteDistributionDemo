package org.example.repository;

import org.example.entity.DemoTestAdvData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemoTestAdvDataRepository extends JpaRepository<DemoTestAdvData, Integer> {
    
    @Query("SELECT a FROM DemoTestAdvData a WHERE a.cigCode = :cigCode AND a.cigName = :cigName")
    DemoTestAdvData findByCigCodeAndCigName(@Param("cigCode") String cigCode, @Param("cigName") String cigName);
    
    @Query("SELECT a FROM DemoTestAdvData a WHERE a.cigCode = :cigCode")
    List<DemoTestAdvData> findByCigCode(@Param("cigCode") String cigCode);
    
    @Query("SELECT a FROM DemoTestAdvData a WHERE a.year = :year AND a.month = :month AND a.weekSeq = :weekSeq")
    List<DemoTestAdvData> findByYearAndMonthAndWeekSeq(@Param("year") Integer year, 
                                                       @Param("month") Integer month, 
                                                       @Param("weekSeq") Integer weekSeq);
}
