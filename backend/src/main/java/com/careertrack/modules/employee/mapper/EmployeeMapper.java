package com.careertrack.modules.employee.mapper;

import com.careertrack.modules.employee.dto.EmployeeCreateDto;
import com.careertrack.modules.employee.dto.EmployeeResponseDto;
import com.careertrack.modules.employee.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "subordinates", ignore = true)
    @Mapping(target = "isCadre", ignore = true)
    @Mapping(target = "maritalStatus", ignore = true)
    @Mapping(target = "dependentsCount", ignore = true)
    Employee toEntity(EmployeeCreateDto dto);

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "position.id", target = "positionId")
    @Mapping(source = "position.title", target = "positionTitle")
    @Mapping(source = "manager.id", target = "managerId")
    @Mapping(target = "managerName", expression = "java(employee.getManager() != null ? employee.getManager().getFullName() : null)")
    @Mapping(target = "fullName", expression = "java(employee.getFullName())")
    EmployeeResponseDto toDto(Employee employee);

    List<EmployeeResponseDto> toDtoList(List<Employee> employees);
}
