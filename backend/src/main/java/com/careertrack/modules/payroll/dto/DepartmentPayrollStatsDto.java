package com.careertrack.modules.payroll.dto;

import java.math.BigDecimal;

public class DepartmentPayrollStatsDto {
    private String departmentName;
    private int employeeCount;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private BigDecimal employerCost;
    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;
    private BigDecimal totalEmployerContributions;

    public DepartmentPayrollStatsDto() {
    }

    public DepartmentPayrollStatsDto(String departmentName, int employeeCount, BigDecimal grossSalary,
                                     BigDecimal netSalary, BigDecimal employerCost, BigDecimal totalGrossSalary,
                                     BigDecimal totalNetSalary, BigDecimal totalEmployerContributions) {
        this.departmentName = departmentName;
        this.employeeCount = employeeCount;
        this.grossSalary = grossSalary;
        this.netSalary = netSalary;
        this.employerCost = employerCost;
        this.totalGrossSalary = totalGrossSalary;
        this.totalNetSalary = totalNetSalary;
        this.totalEmployerContributions = totalEmployerContributions;
    }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }

    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }

    public BigDecimal getEmployerCost() { return employerCost; }
    public void setEmployerCost(BigDecimal employerCost) { this.employerCost = employerCost; }

    public BigDecimal getTotalGrossSalary() { return totalGrossSalary; }
    public void setTotalGrossSalary(BigDecimal totalGrossSalary) { this.totalGrossSalary = totalGrossSalary; }

    public BigDecimal getTotalNetSalary() { return totalNetSalary; }
    public void setTotalNetSalary(BigDecimal totalNetSalary) { this.totalNetSalary = totalNetSalary; }

    public BigDecimal getTotalEmployerContributions() { return totalEmployerContributions; }
    public void setTotalEmployerContributions(BigDecimal totalEmployerContributions) { this.totalEmployerContributions = totalEmployerContributions; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String departmentName;
        private int employeeCount;
        private BigDecimal grossSalary;
        private BigDecimal netSalary;
        private BigDecimal employerCost;
        private BigDecimal totalGrossSalary;
        private BigDecimal totalNetSalary;
        private BigDecimal totalEmployerContributions;

        public Builder departmentName(String departmentName) { this.departmentName = departmentName; return this; }
        public Builder employeeCount(int employeeCount) { this.employeeCount = employeeCount; return this; }
        public Builder grossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; return this; }
        public Builder netSalary(BigDecimal netSalary) { this.netSalary = netSalary; return this; }
        public Builder employerCost(BigDecimal employerCost) { this.employerCost = employerCost; return this; }
        public Builder totalGrossSalary(BigDecimal totalGrossSalary) { this.totalGrossSalary = totalGrossSalary; return this; }
        public Builder totalNetSalary(BigDecimal totalNetSalary) { this.totalNetSalary = totalNetSalary; return this; }
        public Builder totalEmployerContributions(BigDecimal totalEmployerContributions) { this.totalEmployerContributions = totalEmployerContributions; return this; }

        public DepartmentPayrollStatsDto build() {
            return new DepartmentPayrollStatsDto(departmentName, employeeCount, grossSalary, netSalary, employerCost,
                    totalGrossSalary, totalNetSalary, totalEmployerContributions);
        }
    }
}
