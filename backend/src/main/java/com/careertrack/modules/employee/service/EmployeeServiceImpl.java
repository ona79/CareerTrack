package com.careertrack.modules.employee.service;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.exception.ConflictException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.common.payload.PageResponse;
import com.careertrack.common.security.EmployeeSecurityService;
import com.careertrack.modules.document.dto.DocumentResponseDto;
import com.careertrack.modules.document.service.EmployeeDocumentService;
import com.careertrack.modules.employee.dto.*;
import com.careertrack.modules.employee.entity.*;
import com.careertrack.modules.employee.mapper.EmployeeMapper;
import com.careertrack.modules.employee.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "lastName", "firstName", "employeeNumber", "hireDate", "department"
    );

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final ContractRepository contractRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final EmployeeDocumentService employeeDocumentService;
    private final EmployeeSecurityService employeeSecurity;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional
    public EmployeeResponseDto createEmployee(EmployeeCreateDto createDto) {
        if (employeeRepository.findByEmployeeNumber(createDto.getEmployeeNumber()).isPresent()) {
            throw new BadRequestException("Un employé avec le matricule " + createDto.getEmployeeNumber() + " existe déjà");
        }
        if (employeeRepository.findByEmail(createDto.getEmail()).isPresent()) {
            throw new BadRequestException("Un employé avec l'email " + createDto.getEmail() + " existe déjà");
        }

        Employee employee = employeeMapper.toEntity(createDto);

        if (createDto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(createDto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Département", "id", createDto.getDepartmentId()));
            employee.setDepartment(department);
        }

        if (createDto.getPositionId() != null) {
            Position position = positionRepository.findById(createDto.getPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Poste", "id", createDto.getPositionId()));
            employee.setPosition(position);
        }

        if (createDto.getManagerId() != null) {
            Employee manager = employeeRepository.findById(createDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", createDto.getManagerId()));
            employee.setManager(manager);
        }

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto getEmployeeById(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", id));
        return employeeMapper.toDto(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailDto getEmployeeDetail(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", id));

        boolean isDrh = employeeSecurity.hasRole("DRH");
        Optional<Employee> currentEmpOpt = employeeSecurity.getCurrentEmployee();
        boolean isSelf = currentEmpOpt.isPresent() && currentEmpOpt.get().getId().equals(id);
        boolean isManager = !isSelf && employeeSecurity.hasRole("MANAGER");

        // 1. Bloc Identité pro (visible par DRH, Manager, Employé lui-même)
        EmployeeDetailDto.IdentityBlock identity = EmployeeDetailDto.IdentityBlock.builder()
                .id(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .hireDate(employee.getHireDate())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .positionId(employee.getPosition() != null ? employee.getPosition().getId() : null)
                .positionTitle(employee.getPosition() != null ? employee.getPosition().getTitle() : null)
                .managerId(employee.getManager() != null ? employee.getManager().getId() : null)
                .managerName(employee.getManager() != null ? employee.getManager().getFullName() : null)
                .status(employee.getStatus())
                .build();

        // 2. Bloc Données personnelles (DRH et soi-même uniquement ; null pour manager)
        EmployeeDetailDto.PersonalDataBlock personalData = null;
        if (isDrh || isSelf) {
            personalData = EmployeeDetailDto.PersonalDataBlock.builder()
                    .nationalId(employee.getNationalId())
                    .birthDate(employee.getBirthDate())
                    .phone(employee.getPhone())
                    .address(employee.getAddress())
                    .maritalStatus(employee.getMaritalStatus())
                    .dependentsCount(employee.getDependentsCount())
                    .isCadre(employee.isCadre())
                    .build();
        }

        // 3. Bloc Contrat actif
        EmployeeDetailDto.ContractBlock contractBlock = null;
        Optional<Contract> activeContractOpt = contractRepository.findByEmployeeIdAndActiveTrue(id);
        if (activeContractOpt.isPresent()) {
            Contract c = activeContractOpt.get();
            contractBlock = EmployeeDetailDto.ContractBlock.builder()
                    .id(c.getId())
                    .contractType(c.getContractType())
                    .startDate(c.getStartDate())
                    .endDate(c.getEndDate())
                    .active(c.isActive())
                    .grossSalary((isDrh || isSelf) ? c.getGrossSalary() : null)
                    .build();
        }

        // 4. Historique salarial (DRH et soi-même uniquement ; null pour manager)
        List<SalaryHistoryDto> salaryHistory = null;
        if (isDrh || isSelf) {
            salaryHistory = salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(id).stream()
                    .map(this::mapSalaryHistoryToDto)
                    .collect(Collectors.toList());
        }

        // 5. Historique des postes (DRH, soi-même et manager)
        List<PositionHistoryDto> positionHistory = positionHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(id).stream()
                .map(this::mapPositionHistoryToDto)
                .collect(Collectors.toList());

        // 6. Documents liés (DRH et soi-même uniquement ; null pour manager)
        List<DocumentResponseDto> documents = null;
        if (isDrh || isSelf) {
            documents = employeeDocumentService.getDocumentsByEmployee(id);
        }

        // 7. Permissions pour l'interface
        EmployeeDetailDto.PermissionsBlock permissions = EmployeeDetailDto.PermissionsBlock.builder()
                .canEdit(isDrh)
                .canDeactivate(isDrh && employee.getStatus() != EmployeeStatus.TERMINATED)
                .canEditContact(isDrh || isSelf)
                .build();

        return EmployeeDetailDto.builder()
                .identity(identity)
                .personalData(personalData)
                .contract(contractBlock)
                .salaryHistory(salaryHistory)
                .positionHistory(positionHistory)
                .documents(documents)
                .permissions(permissions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto getEmployeeByNumber(String employeeNumber) {
        Employee employee = employeeRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "matricule", employeeNumber));
        return employeeMapper.toDto(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummaryDto> getAllEmployees(
            int page,
            int size,
            String search,
            UUID departmentId,
            EmployeeStatus status,
            String sortBy,
            String sortDir
    ) {
        // Validation tri liste blanche
        String effectiveSortBy = (sortBy == null || sortBy.trim().isEmpty()) ? "lastName" : sortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(effectiveSortBy)) {
            throw new BadRequestException("Champ de tri non autorisé : " + effectiveSortBy);
        }

        // Plafonnement taille
        int effectiveSize = Math.min(Math.max(size, 1), 100);
        int effectivePage = Math.max(page, 0);

        // Mapping propriété JPA pour le tri
        String jpaProperty = effectiveSortBy.equals("department") ? "department.name" : effectiveSortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, jpaProperty);
        Pageable pageable = PageRequest.of(effectivePage, effectiveSize, sort);

        // Périmètre selon le rôle
        UUID managerEmployeeId = null;
        boolean isDrh = employeeSecurity.hasRole("DRH");
        boolean isManager = employeeSecurity.hasRole("MANAGER");

        if (!isDrh && isManager) {
            Employee currentEmp = employeeSecurity.getCurrentEmployee()
                    .orElseThrow(() -> new AccessDeniedException("Profil employé introuvable pour le manager connecté"));
            managerEmployeeId = currentEmp.getId();

            if (status == EmployeeStatus.TERMINATED) {
                throw new BadRequestException("Seule la DRH est autorisée à consulter les collaborateurs inactifs");
            }
        }

        Specification<Employee> spec = EmployeeSpecification.withFilters(search, departmentId, status, managerEmployeeId);
        Page<Employee> employeePage = employeeRepository.findAll(spec, pageable);

        List<EmployeeSummaryDto> summaryList = employeePage.getContent().stream()
                .map(e -> EmployeeSummaryDto.builder()
                        .id(e.getId())
                        .employeeNumber(e.getEmployeeNumber())
                        .fullName(e.getFullName())
                        .positionTitle(e.getPosition() != null ? e.getPosition().getTitle() : null)
                        .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                        .status(e.getStatus())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.<EmployeeSummaryDto>builder()
                .content(summaryList)
                .pageNumber(employeePage.getNumber())
                .pageSize(employeePage.getSize())
                .totalElements(employeePage.getTotalElements())
                .totalPages(employeePage.getTotalPages())
                .last(employeePage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> getEmployeesByDepartment(UUID departmentId) {
        return employeeMapper.toDtoList(employeeRepository.findByDepartmentId(departmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> getSubordinates(UUID managerId) {
        return employeeMapper.toDtoList(employeeRepository.findByManagerId(managerId));
    }

    @Override
    @Transactional
    public EmployeeDetailDto updateEmployee(UUID id, EmployeeUpdateDto updateDto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", id));

        // 0. Mise à jour éventuelle du matricule
        if (updateDto.getEmployeeNumber() != null && !updateDto.getEmployeeNumber().isBlank()
                && !updateDto.getEmployeeNumber().trim().equals(employee.getEmployeeNumber())) {
            Optional<Employee> existing = employeeRepository.findByEmployeeNumber(updateDto.getEmployeeNumber().trim());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new ConflictException("Un employé avec le matricule " + updateDto.getEmployeeNumber() + " existe déjà");
            }
            employee.setEmployeeNumber(updateDto.getEmployeeNumber().trim());
        }

        // 1. Contrôle anti-cycle hiérarchique et manager
        if (updateDto.getManagerId() != null) {
            if (updateDto.getManagerId().equals(id)) {
                throw new BadRequestException("Un employé ne peut pas être son propre responsable hiérarchique");
            }

            Employee newManager = employeeRepository.findById(updateDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", updateDto.getManagerId()));

            if (newManager.getStatus() == EmployeeStatus.TERMINATED) {
                throw new BadRequestException("Le manager sélectionné est inactif");
            }

            // Détection de cycle : remonter la chaîne hiérarchique du nouveau manager
            Employee currentAncestor = newManager;
            Set<UUID> visited = new HashSet<>();
            visited.add(currentAncestor.getId());
            while (currentAncestor.getManager() != null) {
                UUID ancestorManagerId = currentAncestor.getManager().getId();
                if (ancestorManagerId.equals(id)) {
                    throw new BadRequestException("Affectation impossible : cela créerait un cycle hiérarchique");
                }
                if (!visited.add(ancestorManagerId)) {
                    break;
                }
                currentAncestor = currentAncestor.getManager();
            }

            employee.setManager(newManager);
        } else {
            // managerId == null -> retrait explicite du manager
            employee.setManager(null);
        }

        // 2. Gestion et historique des postes et départements
        Department previousDept = employee.getDepartment();
        Position previousPos = employee.getPosition();

        Department newDept = null;
        if (updateDto.getDepartmentId() != null) {
            newDept = departmentRepository.findById(updateDto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Département", "id", updateDto.getDepartmentId()));
        }
        employee.setDepartment(newDept);

        Position newPos = null;
        if (updateDto.getPositionId() != null) {
            newPos = positionRepository.findById(updateDto.getPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Poste", "id", updateDto.getPositionId()));
        }
        employee.setPosition(newPos);

        boolean deptChanged = (previousDept == null && newDept != null) ||
                (previousDept != null && (newDept == null || !previousDept.getId().equals(newDept.getId())));
        boolean posChanged = (previousPos == null && newPos != null) ||
                (previousPos != null && (newPos == null || !previousPos.getId().equals(newPos.getId())));

        Employee currentDrhEmp = employeeSecurity.getCurrentEmployee().orElse(null);

        if (deptChanged || posChanged) {
            PositionHistory positionHistory = PositionHistory.builder()
                    .employee(employee)
                    .previousPosition(previousPos)
                    .newPosition(newPos)
                    .previousDepartment(previousDept)
                    .newDepartment(newDept)
                    .effectiveDate(LocalDate.now())
                    .changeReason(updateDto.getChangeReason() != null ? updateDto.getChangeReason() : "Mise à jour administrative du dossier collaborateur")
                    .changedBy(currentDrhEmp)
                    .build();
            positionHistoryRepository.save(positionHistory);
        }

        // 3. Gestion et historique du salaire
        if (updateDto.getGrossSalary() != null) {
            Optional<Contract> contractOpt = contractRepository.findByEmployeeIdAndActiveTrue(id);
            if (contractOpt.isPresent()) {
                Contract contract = contractOpt.get();
                if (contract.getGrossSalary() == null || contract.getGrossSalary().compareTo(updateDto.getGrossSalary()) != 0) {
                    SalaryHistory salaryHistory = SalaryHistory.builder()
                            .employee(employee)
                            .previousSalary(contract.getGrossSalary() != null ? contract.getGrossSalary() : java.math.BigDecimal.ZERO)
                            .newSalary(updateDto.getGrossSalary())
                            .effectiveDate(LocalDate.now())
                            .changeReason(updateDto.getChangeReason() != null ? updateDto.getChangeReason() : "Ajustement salarial via mise à jour du dossier")
                            .approvedBy(currentDrhEmp)
                            .build();
                    salaryHistoryRepository.save(salaryHistory);

                    contract.setGrossSalary(updateDto.getGrossSalary());
                    contractRepository.save(contract);
                }
            }
        }

        // 4. Mise à jour des autres champs administratifs
        employee.setFirstName(updateDto.getFirstName());
        employee.setLastName(updateDto.getLastName());
        employee.setNationalId(updateDto.getNationalId());
        employee.setEmail(updateDto.getEmail());
        employee.setPhone(updateDto.getPhone());
        employee.setAddress(updateDto.getAddress());
        employee.setBirthDate(updateDto.getBirthDate());
        employee.setHireDate(updateDto.getHireDate());

        if (updateDto.getStatus() != null) {
            employee.setStatus(updateDto.getStatus());
        }
        if (updateDto.getIsCadre() != null) {
            employee.setCadre(updateDto.getIsCadre());
        }
        if (updateDto.getMaritalStatus() != null) {
            employee.setMaritalStatus(updateDto.getMaritalStatus());
        }
        if (updateDto.getDependentsCount() != null) {
            employee.setDependentsCount(updateDto.getDependentsCount());
        }

        employeeRepository.save(employee);
        return getEmployeeDetail(id);
    }

    @Override
    @Transactional
    public EmployeeDetailDto updateSelfContact(UUID id, EmployeeSelfUpdateDto contactDto) {
        boolean isDrh = employeeSecurity.hasRole("DRH");
        Optional<Employee> currentEmpOpt = employeeSecurity.getCurrentEmployee();
        boolean isSelf = currentEmpOpt.isPresent() && currentEmpOpt.get().getId().equals(id);

        if (!isDrh && !isSelf) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier les coordonnées de cet employé");
        }

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", id));

        if (contactDto.getPhone() != null) {
            employee.setPhone(contactDto.getPhone().trim());
        }
        if (contactDto.getAddress() != null) {
            employee.setAddress(contactDto.getAddress().trim());
        }

        employeeRepository.save(employee);
        return getEmployeeDetail(id);
    }

    @Override
    @Transactional
    public void deactivateEmployee(UUID id, EmployeeDeactivateDto deactivateDto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", id));

        if (employee.getStatus() == EmployeeStatus.TERMINATED) {
            throw new BadRequestException("Cet employé est déjà désactivé");
        }

        // Vérification des subordonnés actifs
        long activeSubordinatesCount = employeeRepository.countByManagerIdAndStatusNot(id, EmployeeStatus.TERMINATED);
        if (activeSubordinatesCount > 0) {
            throw new ConflictException("Impossible de désactiver cet employé car il a encore "
                    + activeSubordinatesCount
                    + " collaborateur(s) actif(s) sous sa responsabilité. Veuillez réaffecter ses subordonnés avant de le désactiver.");
        }

        // Désactivation du statut
        employee.setStatus(EmployeeStatus.TERMINATED);

        // Clôture du contrat actif
        contractRepository.findByEmployeeIdAndActiveTrue(id).ifPresent(c -> {
            c.setActive(false);
            c.setEndDate(deactivateDto.getDepartureDate() != null ? deactivateDto.getDepartureDate() : LocalDate.now());
            contractRepository.save(c);
        });

        // Désactivation du compte utilisateur lié
        if (employee.getUser() != null) {
            employee.getUser().setEnabled(false);
        }

        employeeRepository.save(employee);
        log.info("Employé {} désactivé avec succès. Motif: {}", id, deactivateDto.getReason());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeHistoryDto getEmployeeHistory(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employé", "id", id);
        }

        boolean isDrh = employeeSecurity.hasRole("DRH");
        Optional<Employee> currentEmpOpt = employeeSecurity.getCurrentEmployee();
        boolean isSelf = currentEmpOpt.isPresent() && currentEmpOpt.get().getId().equals(id);

        List<SalaryHistoryDto> salaryHistories = null;
        if (isDrh || isSelf) {
            salaryHistories = salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(id).stream()
                    .map(this::mapSalaryHistoryToDto)
                    .collect(Collectors.toList());
        }

        List<PositionHistoryDto> positionHistories = positionHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(id).stream()
                .map(this::mapPositionHistoryToDto)
                .collect(Collectors.toList());

        return EmployeeHistoryDto.builder()
                .salaryHistories(salaryHistories)
                .positionHistories(positionHistories)
                .build();
    }

    @Override
    @Transactional
    public void deleteEmployee(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employé", "id", id);
        }
        employeeRepository.deleteById(id);
    }

    private SalaryHistoryDto mapSalaryHistoryToDto(SalaryHistory sh) {
        return SalaryHistoryDto.builder()
                .id(sh.getId())
                .employeeId(sh.getEmployee() != null ? sh.getEmployee().getId() : null)
                .previousSalary(sh.getPreviousSalary())
                .newSalary(sh.getNewSalary())
                .changeReason(sh.getChangeReason())
                .effectiveDate(sh.getEffectiveDate())
                .approvedById(sh.getApprovedBy() != null ? sh.getApprovedBy().getId() : null)
                .approvedByName(sh.getApprovedBy() != null ? sh.getApprovedBy().getFullName() : null)
                .createdAt(sh.getCreatedAt())
                .build();
    }

    private PositionHistoryDto mapPositionHistoryToDto(PositionHistory ph) {
        return PositionHistoryDto.builder()
                .id(ph.getId())
                .employeeId(ph.getEmployee() != null ? ph.getEmployee().getId() : null)
                .previousPositionId(ph.getPreviousPosition() != null ? ph.getPreviousPosition().getId() : null)
                .previousPositionTitle(ph.getPreviousPosition() != null ? ph.getPreviousPosition().getTitle() : null)
                .newPositionId(ph.getNewPosition() != null ? ph.getNewPosition().getId() : null)
                .newPositionTitle(ph.getNewPosition() != null ? ph.getNewPosition().getTitle() : null)
                .previousDepartmentId(ph.getPreviousDepartment() != null ? ph.getPreviousDepartment().getId() : null)
                .previousDepartmentName(ph.getPreviousDepartment() != null ? ph.getPreviousDepartment().getName() : null)
                .newDepartmentId(ph.getNewDepartment() != null ? ph.getNewDepartment().getId() : null)
                .newDepartmentName(ph.getNewDepartment() != null ? ph.getNewDepartment().getName() : null)
                .effectiveDate(ph.getEffectiveDate())
                .changeReason(ph.getChangeReason())
                .changedById(ph.getChangedBy() != null ? ph.getChangedBy().getId() : null)
                .changedByName(ph.getChangedBy() != null ? ph.getChangedBy().getFullName() : null)
                .createdAt(ph.getCreatedAt())
                .build();
    }
}
