package com.careertrack.modules.orgchart.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.orgchart.dto.OrgNodeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgChartServiceImpl implements OrgChartService {

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrgNodeDto> getCompanyOrgTree() {
        // Une seule requête avec fetch join pour tous les employés actifs
        List<Employee> allEmployees = employeeRepository.findAllActiveWithDetails();

        Map<UUID, Employee> employeeMap = allEmployees.stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        Map<UUID, List<Employee>> childrenByManager = new HashMap<>();
        List<Employee> roots = new ArrayList<>();

        for (Employee e : allEmployees) {
            if (e.getManager() == null || !employeeMap.containsKey(e.getManager().getId())) {
                // Racine : manager nul ou manager absent/inactif
                roots.add(e);
            } else {
                childrenByManager.computeIfAbsent(e.getManager().getId(), k -> new ArrayList<>()).add(e);
            }
        }

        Set<UUID> visited = new HashSet<>();
        List<OrgNodeDto> tree = new ArrayList<>();

        for (Employee root : roots) {
            OrgNodeDto rootNode = buildNode(root, childrenByManager, visited);
            if (rootNode != null) {
                tree.add(rootNode);
            }
        }

        // Si des employés restent non visités à cause de cycles complets isolés, les rattacher au sommet
        for (Employee e : allEmployees) {
            if (!visited.contains(e.getId())) {
                log.warn("Employé {} non rattaché détecté (possible cycle isolé), rattachement au niveau racine", e.getId());
                OrgNodeDto orphanNode = buildNode(e, childrenByManager, visited);
                if (orphanNode != null) {
                    tree.add(orphanNode);
                }
            }
        }

        return tree;
    }

    @Override
    @Transactional(readOnly = true)
    public OrgNodeDto getSubtreeForEmployee(UUID employeeId) {
        List<Employee> allEmployees = employeeRepository.findAllActiveWithDetails();
        Map<UUID, Employee> employeeMap = allEmployees.stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        Employee employee = employeeMap.get(employeeId);
        if (employee == null) {
            // Peut-être inactif ou introuvable
            employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", employeeId));
        }

        Map<UUID, List<Employee>> childrenByManager = new HashMap<>();
        for (Employee e : allEmployees) {
            if (e.getManager() != null && employeeMap.containsKey(e.getManager().getId())) {
                childrenByManager.computeIfAbsent(e.getManager().getId(), k -> new ArrayList<>()).add(e);
            }
        }

        Set<UUID> visited = new HashSet<>();
        return buildNode(employee, childrenByManager, visited);
    }

    private OrgNodeDto buildNode(Employee emp, Map<UUID, List<Employee>> childrenByManager, Set<UUID> visited) {
        if (!visited.add(emp.getId())) {
            log.warn("Cycle hiérarchique détecté pour l'employé {}, nœud ignoré dans la branche", emp.getId());
            return null;
        }

        List<Employee> children = childrenByManager.getOrDefault(emp.getId(), Collections.emptyList());
        List<OrgNodeDto> subordinateNodes = new ArrayList<>();

        for (Employee child : children) {
            OrgNodeDto childNode = buildNode(child, childrenByManager, visited);
            if (childNode != null) {
                subordinateNodes.add(childNode);
            }
        }

        int directAndIndirectCount = subordinateNodes.stream()
                .mapToInt(n -> 1 + (n.getSubordinateCount() != null ? n.getSubordinateCount() : 0))
                .sum();

        return OrgNodeDto.builder()
                .id(emp.getId())
                .name(emp.getFullName())
                .employeeNumber(emp.getEmployeeNumber())
                .title(emp.getPosition() != null ? emp.getPosition().getTitle() : "Poste non défini")
                .department(emp.getDepartment() != null ? emp.getDepartment().getName() : "Département non défini")
                .email(emp.getEmail())
                .phone(null) // Téléphone personnel exclu conformément à la décision de sécurité Lot 2
                .subordinateCount(directAndIndirectCount)
                .subordinates(subordinateNodes)
                .build();
    }
}
