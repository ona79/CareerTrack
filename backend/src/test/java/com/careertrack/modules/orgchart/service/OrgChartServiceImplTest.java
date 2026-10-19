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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("Construire l'organigramme complet en une requête avec calcul des subordonnés")
    void shouldGetCompanyOrgTree() {
        UUID rootId = UUID.randomUUID();
        UUID mgrId = UUID.randomUUID();
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
                .phone("770000001")
                .build();

        Position posLead = Position.builder().title("Lead Tech").build();
        Department depIT = Department.builder().name("Ingénierie").build();

        Employee mgr = Employee.builder()
                .id(mgrId)
                .firstName("Aminata")
                .lastName("Sow")
                .employeeNumber("EMP-002")
                .position(posLead)
                .department(depIT)
                .manager(root)
                .email("aminata@careertrack.sn")
                .phone("770000002")
                .build();

        Position posDev = Position.builder().title("Développeur").build();

        Employee sub = Employee.builder()
                .id(subId)
                .firstName("Ousmane")
                .lastName("Diallo")
                .employeeNumber("EMP-003")
                .position(posDev)
                .department(depIT)
                .manager(mgr)
                .email("ousmane@careertrack.sn")
                .phone("770000003")
                .build();

        when(employeeRepository.findAllActiveWithDetails()).thenReturn(List.of(root, mgr, sub));

        List<OrgNodeDto> result = orgChartService.getCompanyOrgTree();

        assertThat(result).hasSize(1);
        OrgNodeDto rootNode = result.get(0);
        assertThat(rootNode.getName()).isEqualTo("Cheikh Fall");
        assertThat(rootNode.getEmail()).isEqualTo("direction@careertrack.sn");
        assertThat(rootNode.getPhone()).isNull(); // Téléphone personnel masqué
        assertThat(rootNode.getSubordinateCount()).isEqualTo(2); // mgr + sub

        assertThat(rootNode.getSubordinates()).hasSize(1);
        OrgNodeDto mgrNode = rootNode.getSubordinates().get(0);
        assertThat(mgrNode.getName()).isEqualTo("Aminata Sow");
        assertThat(mgrNode.getSubordinateCount()).isEqualTo(1); // sub
        assertThat(mgrNode.getPhone()).isNull();

        assertThat(mgrNode.getSubordinates()).hasSize(1);
        OrgNodeDto subNode = mgrNode.getSubordinates().get(0);
        assertThat(subNode.getName()).isEqualTo("Ousmane Diallo");
        assertThat(subNode.getSubordinateCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Protection anti-cycle : arbre résolu sans boucle infinie ni exception")
    void shouldHandleCycleWithoutInfiniteLoop() {
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();

        Employee empA = Employee.builder()
                .id(aId)
                .firstName("A")
                .lastName("A")
                .employeeNumber("EMP-A")
                .email("a@careertrack.sn")
                .build();

        Employee empB = Employee.builder()
                .id(bId)
                .firstName("B")
                .lastName("B")
                .employeeNumber("EMP-B")
                .email("b@careertrack.sn")
                .build();

        // Cycle A -> B -> A
        empA.setManager(empB);
        empB.setManager(empA);

        when(employeeRepository.findAllActiveWithDetails()).thenReturn(List.of(empA, empB));

        List<OrgNodeDto> result = orgChartService.getCompanyOrgTree();

        assertThat(result).isNotEmpty();
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
                .phone("771234567")
                .build();

        when(employeeRepository.findAllActiveWithDetails()).thenReturn(List.of(emp));

        OrgNodeDto node = orgChartService.getSubtreeForEmployee(empId);

        assertThat(node.getId()).isEqualTo(empId);
        assertThat(node.getName()).isEqualTo("Aminata Ndiaye");
        assertThat(node.getEmail()).isEqualTo("drh@careertrack.sn");
        assertThat(node.getPhone()).isNull();
    }
}
