package com.careertrack.modules.leave.mapper;

import com.careertrack.modules.leave.dto.LeaveBalanceDto;
import com.careertrack.modules.leave.dto.LeaveRequestCreateDto;
import com.careertrack.modules.leave.dto.LeaveRequestResponseDto;
import com.careertrack.modules.leave.entity.LeaveBalance;
import com.careertrack.modules.leave.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LeaveMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "leaveType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "managerComment", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    LeaveRequest toEntity(LeaveRequestCreateDto dto);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeName", expression = "java(request.getEmployee() != null ? request.getEmployee().getFullName() : null)")
    @Mapping(source = "employee.employeeNumber", target = "employeeNumber")
    @Mapping(target = "departmentName", expression = "java(request.getEmployee() != null && request.getEmployee().getDepartment() != null ? request.getEmployee().getDepartment().getName() : null)")
    @Mapping(source = "leaveType.id", target = "leaveTypeId")
    @Mapping(source = "leaveType.name", target = "leaveTypeName")
    @Mapping(source = "leaveType.code", target = "leaveTypeCode")
    @Mapping(source = "approvedBy.id", target = "approvedById")
    @Mapping(target = "approvedByName", expression = "java(request.getApprovedBy() != null ? request.getApprovedBy().getFullName() : null)")
    LeaveRequestResponseDto toDto(LeaveRequest request);

    List<LeaveRequestResponseDto> toDtoList(List<LeaveRequest> requests);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "leaveType.id", target = "leaveTypeId")
    @Mapping(source = "leaveType.name", target = "leaveTypeName")
    @Mapping(source = "leaveType.code", target = "leaveTypeCode")
    LeaveBalanceDto toDto(LeaveBalance balance);

    List<LeaveBalanceDto> toBalanceDtoList(List<LeaveBalance> balances);
}
