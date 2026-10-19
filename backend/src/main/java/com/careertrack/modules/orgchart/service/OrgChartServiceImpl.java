package com.careertrack.modules.orgchart.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.orgchart.dto.OrgNodeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrgChartServiceImpl implements OrgChartService {

    private static final Comparator<Employee> BY_NAME =
            Comparator.comparing(Employee::getFullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final EmployeeRepository employeeRepository;

    public OrgChartServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrgNodeDto> getCompanyOrgTree() {
        List<Employee> all = employeeRepository.findAllActiveWithDetails();
        Map<UUID, List<Employee>> children = groupByManager(all);
        Set<UUID> activeIds = all.stream().map(Employee::getId).collect(Collectors.toSet());
        Set<UUID> visited = new HashSet<>();

        List<OrgNodeDto> roots = all.stream()
                .filter(e -> e.getManager() == null || !activeIds.contains(e.getManager().getId()))
                .sorted(BY_NAME)
                .map(e -> buildNode(e, children, visited))
                .collect(Collectors.toList());

        if (visited.size() < all.size()) {
            log.warn("Organigramme : {} employé(s) non affiché(s), boucle probable dans la hiérarchie",
                    all.size() - visited.size());
        }
        return roots;
    }

    @Override
    @Transactional(readOnly = true)
    public OrgNodeDto getSubtreeForEmployee(UUID employeeId) {
        List<Employee> all = employeeRepository.findAllActiveWithDetails();
        Employee target = all.stream()
                .filter(e -> e.getId().equals(employeeId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", employeeId));
        return buildNode(target, groupByManager(all), new HashSet<>());
    }

    private Map<UUID, List<Employee>> groupByManager(List<Employee> employees) {
        Map<UUID, List<Employee>> map = new HashMap<>();
        for (Employee e : employees) {
            if (e.getManager() != null) {
                map.computeIfAbsent(e.getManager().getId(), k -> new ArrayList<>()).add(e);
            }
        }
        return map;
    }

    private OrgNodeDto buildNode(Employee employee, Map<UUID, List<Employee>> children, Set<UUID> visited) {
        visited.add(employee.getId());

        List<OrgNodeDto> subordinateNodes = children.getOrDefault(employee.getId(), List.of()).stream()
                .filter(c -> !visited.contains(c.getId()))
                .sorted(BY_NAME)
                .map(c -> buildNode(c, children, visited))
                .collect(Collectors.toList());

        return OrgNodeDto.builder()
                .id(employee.getId())
                .name(employee.getFullName())
                .employeeNumber(employee.getEmployeeNumber())
                .title(employee.getPosition() != null ? employee.getPosition().getTitle() : "Poste non défini")
                .department(employee.getDepartment() != null ? employee.getDepartment().getName() : "Département non défini")
                .subordinates(subordinateNodes)
                .build();
    }
}