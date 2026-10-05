package com.smarthr.smarthr.company.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthr.smarthr.company.entity.CompanyEntity;
import com.smarthr.smarthr.company.entity.WorkSchedule;
import com.smarthr.smarthr.company.repository.CompanyRepository;
import com.smarthr.smarthr.employee.entity.EmployeeDetails;

import lombok.RequiredArgsConstructor;

/**
 * Resolves which company work schedule applies to an employee.
 */
@Service
@RequiredArgsConstructor
public class WorkScheduleService {

    private final CompanyRepository companyRepository;

    /**
     * The employee's company is taken from their department, falling back to
     * {@code employee.companyId}; with neither, the default schedule applies.
     */
    @Transactional(readOnly = true)
    public WorkSchedule forEmployee(EmployeeDetails employee) {
        if (employee.getDepartment() != null && employee.getDepartment().getCompany() != null) {
            return WorkSchedule.of(employee.getDepartment().getCompany());
        }
        if (employee.getCompanyId() != null) {
            CompanyEntity company = companyRepository.findById(employee.getCompanyId().longValue()).orElse(null);
            return WorkSchedule.of(company);
        }
        return WorkSchedule.defaults();
    }
}
