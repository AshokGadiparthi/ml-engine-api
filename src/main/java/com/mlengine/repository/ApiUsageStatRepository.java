package com.mlengine.repository;

import com.mlengine.model.entity.ApiUsageStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ApiUsageStat entity.
 */
@Repository
public interface ApiUsageStatRepository extends JpaRepository<ApiUsageStat, String> {

    // Find stats for a specific date and hour
    Optional<ApiUsageStat> findByModelIdAndStatDateAndStatHour(
            String modelId, LocalDate statDate, Integer statHour);

    Optional<ApiUsageStat> findByProjectIdAndStatDateAndStatHour(
            String projectId, LocalDate statDate, Integer statHour);

    // Find daily stats (statHour is null)
    Optional<ApiUsageStat> findByModelIdAndStatDateAndStatHourIsNull(
            String modelId, LocalDate statDate);

    Optional<ApiUsageStat> findByProjectIdAndStatDateAndStatHourIsNull(
            String projectId, LocalDate statDate);

    // Sum stats for a date range
    @Query("SELECT COALESCE(SUM(s.requestCount), 0) FROM ApiUsageStat s WHERE s.modelId = :modelId AND s.statDate = :date")
    Long sumRequestsByModelAndDate(@Param("modelId") String modelId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.requestCount), 0) FROM ApiUsageStat s WHERE s.projectId = :projectId AND s.statDate = :date")
    Long sumRequestsByProjectAndDate(@Param("projectId") String projectId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.requestCount), 0) FROM ApiUsageStat s WHERE s.modelId = :modelId AND s.statDate BETWEEN :startDate AND :endDate")
    Long sumRequestsByModelAndDateRange(@Param("modelId") String modelId, 
                                        @Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(s.requestCount), 0) FROM ApiUsageStat s WHERE s.projectId = :projectId AND s.statDate BETWEEN :startDate AND :endDate")
    Long sumRequestsByProjectAndDateRange(@Param("projectId") String projectId, 
                                          @Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);

    // Average latency
    @Query("SELECT AVG(s.avgLatencyMs) FROM ApiUsageStat s WHERE s.modelId = :modelId AND s.statDate BETWEEN :startDate AND :endDate AND s.avgLatencyMs IS NOT NULL")
    Double avgLatencyByModelAndDateRange(@Param("modelId") String modelId, 
                                         @Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(s.avgLatencyMs) FROM ApiUsageStat s WHERE s.projectId = :projectId AND s.statDate BETWEEN :startDate AND :endDate AND s.avgLatencyMs IS NOT NULL")
    Double avgLatencyByProjectAndDateRange(@Param("projectId") String projectId, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);

    // Get hourly stats for a specific day
    @Query("SELECT s FROM ApiUsageStat s WHERE s.modelId = :modelId AND s.statDate = :date AND s.statHour IS NOT NULL ORDER BY s.statHour")
    List<ApiUsageStat> findHourlyStatsByModelAndDate(@Param("modelId") String modelId, @Param("date") LocalDate date);

    @Query("SELECT s FROM ApiUsageStat s WHERE s.projectId = :projectId AND s.statDate = :date AND s.statHour IS NOT NULL ORDER BY s.statHour")
    List<ApiUsageStat> findHourlyStatsByProjectAndDate(@Param("projectId") String projectId, @Param("date") LocalDate date);

    // Get daily stats for a date range
    @Query("SELECT s FROM ApiUsageStat s WHERE s.modelId = :modelId AND s.statDate BETWEEN :startDate AND :endDate AND s.statHour IS NULL ORDER BY s.statDate")
    List<ApiUsageStat> findDailyStatsByModelAndDateRange(@Param("modelId") String modelId, 
                                                         @Param("startDate") LocalDate startDate, 
                                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM ApiUsageStat s WHERE s.projectId = :projectId AND s.statDate BETWEEN :startDate AND :endDate AND s.statHour IS NULL ORDER BY s.statDate")
    List<ApiUsageStat> findDailyStatsByProjectAndDateRange(@Param("projectId") String projectId, 
                                                           @Param("startDate") LocalDate startDate, 
                                                           @Param("endDate") LocalDate endDate);
}
