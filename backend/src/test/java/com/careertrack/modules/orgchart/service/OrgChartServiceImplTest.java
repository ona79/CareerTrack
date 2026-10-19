package com.careertrack.modules.orgchart.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Department;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.orgchart.dto.OrgNodeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgChartServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private OrgChartServiceImpl orgChartService;

    @BeforeEach
    void setUp() {
        orgChartService = new OrgChartServiceImpl(employeeRepository);
    }

    @Test
    @DisplayName("Construire l'organigramme complet de l'entreprise à partir des racines")
    void shouldGetCompanyOrgTree() {
        UUID rootId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();

        Position posDG = Position.builder().title("Directeur Général").build();
        Department depDG = Department.builder().name("Direction").build();

        Employee root = Employee.builder()
                .id(rootId)
                .firstName("Cheikh")
                .lastName("Fall")
                .employeeNumber("EMP-001")
                .position(posDG)
                .department(depDG)
                .email("direction@careertrack.sn")
                .manager(null)
                .build();

        Position posDev = Position.builder().title("Développeur").build();
        Department depIT = Department.builder().name("Ingénierie").build();

        Employee sub = Employee.builder()
                .id(subId)
                .firstName("Ousmane")
                .lastName("Diallo")
                .employeeNumber("EMP-002")
                .position(posDev)
                .department(depIT)
                .email("ousmane@careertrack.sn")
                .manager(root)
                .build();

        // Le service fait un findAllActiveWithDetails et traite en mémoire
        when(employeeRepository.findAllActiveWithDetails()).thenReturn(List.of(root, sub));

        List<OrgNodeDto> result = orgChartService.getCompanyOrgTree();

        assertThat(result).hasSize(1);
        OrgNodeDto rootNode = result.get(0);
        assertThat(rootNode.getName()).isEqualTo("Cheikh Fall");
        assertThat(rootNode.getSubordinates()).hasSize(1);
        assertThat(rootNode.getSubordinates().get(0).getName()).isEqualTo("Ousmane Diallo");
    }

    @Test
    @DisplayName("Obtenir le sous-arbre pour un collaborateur spécifique")
    void shouldGetSubtreeForEmployee() {
        UUID empId = UUID.randomUUID();
        Employee emp = Employee.builder()
                .id(empId)
                .firstName("Aminata")
                .lastName("Ndiaye")
                .employeeNumber("EMP-003")
                .email("drh@careertrack.sn")
                .manager(null)
                .build();

        when(employeeRepository.findAllActiveWithDetails()).thenReturn(List.of(emp));

        OrgNodeDto node = orgChartService.getSubtreeForEmployee(empId);

        assertThat(node.getId()).isEqualTo(empId);
        assertThat(node.getName()).isEqualTo("Aminata Ndiaye");
    }

    @Test
    @DisplayName("Lever une exception si le collaborateur est introuvable")
    void shouldThrowWhenEmployeeNotFound() {
        UUID missingId = UUID.randomUUID();
        
        // Le service cherche dans la liste, on renvoie une liste vide
        when(employeeRepository.findAllActiveWithDetails()).thenReturn(List.of());

        assertThatThrownBy(() -> orgChartService.getSubtreeForEmployee(missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}