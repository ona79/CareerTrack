package com.careertrack.modules.payroll.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PayrollStatsResponseDto {
    private String period;
    private Integer periodYear;
    private Integer periodMonth;
    private int totalEmployeesPaid;
    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;
    private BigDecimal totalDeductions;
    private BigDecimal totalEmployerContributions;
    private BigDecimal totalEmployerCost;
    private BigDecimal totalPayrollCost;
    private Boolean anonymized;
    private List<DepartmentPayrollStatsDto> departmentBreakdown = new ArrayList<>();
    private List<DepartmentPayrollStatsDto> departmentBreakdowns = new ArrayList<>();

    public PayrollStatsResponseDto() {
    }

    public PayrollStatsResponseDto(String period, Integer periodYear, Integer periodMonth, int totalEmployeesPaid,
                                   BigDecimal totalGrossSalary, BigDecimal totalNetSalary, BigDecimal totalDeductions,
                                   BigDecimal totalEmployerContributions, BigDecimal totalEmployerCost,
                                   BigDecimal totalPayrollCost, Boolean anonymized,
                                   List<DepartmentPayrollStatsDto> departmentBreakdown,
                                   List<DepartmentPayrollStatsDto> departmentBreakdowns) {
        this.period = period;
        this.periodYear = periodYear;
        this.periodMonth = periodMonth;
        this.totalEmployeesPaid = totalEmployeesPaid;
        this.totalGrossSalary = totalGrossSalary;
        this.totalNetSalary = totalNetSalary;
        this.totalDeductions = totalDeductions;
        this.totalEmployerContributions = totalEmployerContributions;
        this.totalEmployerCost = totalEmployerCost;
        this.totalPayrollCost = totalPayrollCost;
        this.anonymized = anonymized;
        this.departmentBreakdown = departmentBreakdown != null ? departmentBreakdown : new ArrayList<>();
        this.departmentBreakdowns = departmentBreakdowns != null ? departmentBreakdowns : new ArrayList<>();
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }

    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }

    public int getTotalEmployeesPaid() { return totalEmployeesPaid; }
    public void setTotalEmployeesPaid(int totalEmployeesPaid) { this.totalEmployeesPaid = totalEmployeesPaid; }

    public BigDecimal getTotalGrossSalary() { return totalGrossSalary; }
    public void setTotalGrossSalary(BigDecimal totalGrossSalary) { this.totalGrossSalary = totalGrossSalary; }

    public BigDecimal getTotalNetSalary() { return totalNetSalary; }
    public void setTotalNetSalary(BigDecimal totalNetSalary) { this.totalNetSalary = totalNetSalary; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }

    public BigDecimal getTotalEmployerContributions() { return totalEmployerContributions; }
    public void setTotalEmployerContributions(BigDecimal totalEmployerContributions) { this.totalEmployerContributions = totalEmployerContributions; }

    public BigDecimal getTotalEmployerCost() { return totalEmployerCost; }
    public void setTotalEmployerCost(BigDecimal totalEmployerCost) { this.totalEmployerCost = totalEmployerCost; }

    public BigDecimal getTotalPayrollCost() { return totalPayrollCost; }
    public void setTotalPayrollCost(BigDecimal totalPayrollCost) { this.totalPayrollCost = totalPayrollCost; }

    public Boolean getAnonymized() { return anonymized; }
    public void setAnonymized(Boolean anonymized) { this.anonymized = anonymized; }

    public List<DepartmentPayrollStatsDto> getDepartmentBreakdown() { return departmentBreakdown; }
    public void setDepartmentBreakdown(List<DepartmentPayrollStatsDto> departmentBreakdown) { this.departmentBreakdown = departmentBreakdown; }

    public List<DepartmentPayrollStatsDto> getDepartmentBreakdowns() { return departmentBreakdowns; }
    public void setDepartmentBreakdowns(List<DepartmentPayrollStatsDto> departmentBreakdowns) { this.departmentBreakdowns = departmentBreakdowns; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String period;
        private Integer periodYear;
        private Integer periodMonth;
        private int totalEmployeesPaid;
        private BigDecimal totalGrossSalary;
        private BigDecimal totalNetSalary;
        private BigDecimal totalDeductions;
        private BigDecimal totalEmployerContributions;
        private BigDecimal totalEmployerCost;
        private BigDecimal totalPayrollCost;
        private Boolean anonymized;
        private List<DepartmentPayrollStatsDto> departmentBreakdown = new ArrayList<>();
        private List<DepartmentPayrollStatsDto> departmentBreakdowns = new ArrayList<>();

        public Builder period(String period) { this.period = period; return this; }
        public Builder periodYear(Integer periodYear) { this.periodYear = periodYear; return this; }
        public Builder periodMonth(Integer periodMonth) { this.periodMonth = periodMonth; return this; }
        public Builder totalEmployeesPaid(int totalEmployeesPaid) { this.totalEmployeesPaid = totalEmployeesPaid; return this; }
        public Builder totalGrossSalary(BigDecimal totalGrossSalary) { this.totalGrossSalary = totalGrossSalary; return this; }
        public Builder totalNetSalary(BigDecimal totalNetSalary) { this.totalNetSalary = totalNetSalary; return this; }
        public Builder totalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; return this; }
        public Builder totalEmployerContributions(BigDecimal totalEmployerContributions) { this.totalEmployerContributions = totalEmployerContributions; return this; }
        public Builder totalEmployerCost(BigDecimal totalEmployerCost) { this.totalEmployerCost = totalEmployerCost; return this; }
        public Builder totalPayrollCost(BigDecimal totalPayrollCost) { this.totalPayrollCost = totalPayrollCost; return this; }
        public Builder anonymized(Boolean anonymized) { this.anonymized = anonymized; return this; }
        public Builder departmentBreakdown(List<DepartmentPayrollStatsDto> departmentBreakdown) { this.departmentBreakdown = departmentBreakdown; return this; }
        public Builder departmentBreakdowns(List<DepartmentPayrollStatsDto> departmentBreakdowns) { this.departmentBreakdowns = departmentBreakdowns; return this; }

        public PayrollStatsResponseDto build() {
            return new PayrollStatsResponseDto(period, periodYear, periodMonth, totalEmployeesPaid,
                    totalGrossSalary, totalNetSalary, totalDeductions, totalEmployerContributions,
                    totalEmployerCost, totalPayrollCost, anonymized, departmentBreakdown, departmentBreakdowns);
        }
    }
}
