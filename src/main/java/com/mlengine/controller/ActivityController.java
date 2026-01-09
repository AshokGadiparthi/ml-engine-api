package com.mlengine.controller;

import com.mlengine.model.dto.ActivityDTO;
import com.mlengine.model.entity.Activity.ActivityType;
import com.mlengine.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Activity operations.
 * Provides activity feed for dashboard.
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "Activities", description = "Activity feed and tracking endpoints")
@CrossOrigin
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "Get activities",
               description = "Returns paginated list of activities")
    public ResponseEntity<ActivityDTO.ListResponse> getActivities(
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) String projectId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(activityService.getActivities(projectId, page, pageSize));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent activities",
               description = "Returns most recent activities for dashboard")
    public ResponseEntity<List<ActivityDTO.Response>> getRecentActivities(
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) String projectId,
            @Parameter(description = "Number of activities to return")
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(activityService.getRecentActivities(projectId, limit));
    }

    @GetMapping("/by-type/{type}")
    @Operation(summary = "Get activities by type",
               description = "Returns activities filtered by type")
    public ResponseEntity<List<ActivityDTO.Response>> getActivitiesByType(
            @PathVariable ActivityType type,
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(activityService.getActivitiesByType(projectId, type));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get activity stats",
               description = "Returns activity statistics for dashboard")
    public ResponseEntity<ActivityDTO.Stats> getActivityStats(
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(activityService.getActivityStats(projectId));
    }
}
