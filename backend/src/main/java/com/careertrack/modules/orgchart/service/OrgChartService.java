package com.careertrack.modules.orgchart.service;

import com.careertrack.modules.orgchart.dto.OrgNodeDto;

import java.util.List;
import java.util.UUID;

public interface OrgChartService {
    List<OrgNodeDto> getCompanyOrgTree();
    OrgNodeDto getSubtreeForEmployee(UUID employeeId);
}
