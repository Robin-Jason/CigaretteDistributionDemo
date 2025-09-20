package org.example.repository;

import org.example.entity.DemoTestData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface DemoTestDataRepository extends JpaRepository<DemoTestData, Integer> {
    
    @Query("SELECT d FROM DemoTestData d WHERE d.year = :year AND d.month = :month AND d.weekSeq = :weekSeq")
    List<DemoTestData> findByYearAndMonthAndWeekSeq(@Param("year") Integer year, 
                                                   @Param("month") Integer month, 
                                                   @Param("weekSeq") Integer weekSeq);
    
    @Query("SELECT d FROM DemoTestData d WHERE d.cigCode = :cigCode AND d.cigName = :cigName")
    List<DemoTestData> findByCigCodeAndCigName(@Param("cigCode") String cigCode, 
                                              @Param("cigName") String cigName);
    
    @Query("SELECT d FROM DemoTestData d WHERE d.cigCode = :cigCode AND d.cigName = :cigName AND d.deliveryArea = :deliveryArea")
    DemoTestData findByCigCodeAndCigNameAndDeliveryArea(@Param("cigCode") String cigCode, 
                                                       @Param("cigName") String cigName, 
                                                       @Param("deliveryArea") String deliveryArea);
    
    @Query("SELECT d FROM DemoTestData d WHERE d.cigCode = :cigCode AND d.cigName = :cigName AND d.year = :year AND d.month = :month AND d.weekSeq = :weekSeq")
    List<DemoTestData> findByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(@Param("cigCode") String cigCode, 
                                                                       @Param("cigName") String cigName, 
                                                                       @Param("year") Integer year, 
                                                                       @Param("month") Integer month, 
                                                                       @Param("weekSeq") Integer weekSeq);
    
    @Query("SELECT d FROM DemoTestData d WHERE d.cigCode = :cigCode AND d.cigName = :cigName AND d.deliveryArea = :deliveryArea AND d.year = :year AND d.month = :month AND d.weekSeq = :weekSeq")
    DemoTestData findByCigCodeAndCigNameAndDeliveryAreaAndYearAndMonthAndWeekSeq(@Param("cigCode") String cigCode, 
                                                                                @Param("cigName") String cigName, 
                                                                                @Param("deliveryArea") String deliveryArea,
                                                                                @Param("year") Integer year, 
                                                                                @Param("month") Integer month, 
                                                                                @Param("weekSeq") Integer weekSeq);
    
    @Query("SELECT d FROM DemoTestData d WHERE d.year = :year AND d.month = :month AND d.weekSeq = :weekSeq AND d.cigCode IN :cigCodes")
    List<DemoTestData> findByYearAndMonthAndWeekSeqAndCigCodeIn(@Param("year") Integer year, 
                                                               @Param("month") Integer month, 
                                                               @Param("weekSeq") Integer weekSeq, 
                                                               @Param("cigCodes") List<String> cigCodes);
    
    @Query("SELECT d FROM DemoTestData d WHERE d.year = :year AND d.month = :month AND d.weekSeq = :weekSeq AND d.cigCode = :cigCode AND d.deliveryArea IN :deliveryAreas")
    List<DemoTestData> findByYearAndMonthAndWeekSeqAndCigCodeAndDeliveryAreaIn(@Param("year") Integer year,
                                                                              @Param("month") Integer month,
                                                                              @Param("weekSeq") Integer weekSeq,
                                                                              @Param("cigCode") String cigCode,
                                                                              @Param("deliveryAreas") List<String> deliveryAreas);
    
    // 删除指定卷烟指定日期的所有投放记录
    @Modifying
    @Transactional
    @Query("DELETE FROM DemoTestData d WHERE d.cigCode = :cigCode AND d.cigName = :cigName AND d.year = :year AND d.month = :month AND d.weekSeq = :weekSeq")
    int deleteByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(@Param("cigCode") String cigCode, 
                                                          @Param("cigName") String cigName, 
                                                          @Param("year") Integer year, 
                                                          @Param("month") Integer month, 
                                                          @Param("weekSeq") Integer weekSeq);
    
    // 删除指定日期的所有投放记录
    @Modifying
    @Transactional
    @Query("DELETE FROM DemoTestData d WHERE d.year = :year AND d.month = :month AND d.weekSeq = :weekSeq")
    int deleteByYearAndMonthAndWeekSeq(@Param("year") Integer year, 
                                      @Param("month") Integer month, 
                                      @Param("weekSeq") Integer weekSeq);
}
