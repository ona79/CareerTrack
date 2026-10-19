package com.careertrack.modules.dashboard.service;

import com.careertrack.modules.dashboard.dto.DashboardStatsDto;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface DashboardService {
    DashboardStatsDto getDashboardStats(UUID departmentId, UUID positionId, Integer periodYear, Authentication authentication);
}
