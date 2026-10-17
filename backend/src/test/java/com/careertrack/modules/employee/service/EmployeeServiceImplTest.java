package com.careertrack.modules.employee.service;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.dto.EmployeeCreateDto;
import com.careertrack.modules.employee.dto.EmployeeResponseDto;
import com.careertrack.modules.employee.entity.Department;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.mapper.EmployeeMapper;
import com.careertrack.modules.employee.repository.DepartmentRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.employee.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private UUID employeeId;
    private Employee employee;
    private EmployeeResponseDto responseDto;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        employee = Employee.builder()
                .id(employeeId)
                .employeeNumber("EMP-001")
                .firstName("Fatou")
                .lastName("Ndiaye")
                .email("fatou.ndiaye@careertrack.sn")
                .build();

        responseDto = new EmployeeResponseDto();
        responseDto.setId(employeeId);
        responseDto.setEmployeeNumber("EMP-001");
        responseDto.setFullName("Fatou Ndiaye");
    }

    @Test
    @DisplayName("Création réussie d'un collaborateur")
    void shouldCreateEmployeeSuccessfully() {
        UUID deptId = UUID.randomUUID();
        UUID posId = UUID.randomUUID();

        EmployeeCreateDto dto = new EmployeeCreateDto();
        dto.setEmployeeNumber("EMP-002");
        dto.setEmail("nouveau@careertrack.sn");
        dto.setDepartmentId(deptId);
        dto.setPositionId(posId);

        Department dept = Department.builder().id(deptId).name("Direction RH").build();
        Position pos = Position.builder().id(posId).title("Chargée RH").build();

        when(employeeRepository.findByEmployeeNumber("EMP-002")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmail("nouveau@careertrack.sn")).thenReturn(Optional.empty());
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(positionRepository.findById(posId)).thenReturn(Optional.of(pos));
        when(employeeMapper.toEntity(dto)).thenReturn(employee);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(employeeMapper.toDto(employee)).thenReturn(responseDto);

        EmployeeResponseDto result = employeeService.createEmployee(dto);

        assertThat(result).isNotNull();
        assertThat(result.getEmployeeNumber()).isEqualTo("EMP-001");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Échec si le matricule employé existe déjà")
    void shouldThrowBadRequestWhenEmployeeNumberExists() {
        EmployeeCreateDto dto = new EmployeeCreateDto();
        dto.setEmployeeNumber("EMP-001");

        when(employeeRepository.findByEmployeeNumber("EMP-001")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.createEmployee(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    @DisplayName("Récupération d'un collaborateur par ID")
    void shouldGetEmployeeById() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeMapper.toDto(employee)).thenReturn(responseDto);

        EmployeeResponseDto result = employeeService.getEmployeeById(employeeId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("Échec si le collaborateur n'existe pas")
    void shouldThrowResourceNotFoundWhenEmployeeMissing() {
        UUID missingId = UUID.randomUUID();
        when(employeeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employé");
    }
}
