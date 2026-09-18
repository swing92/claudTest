package com.lifehub.jobapplications.service;

import com.lifehub.common.exception.ConflictException;
import com.lifehub.common.exception.ResourceNotFoundException;
import com.lifehub.jobapplications.dto.request.CompanyCreateRequest;
import com.lifehub.jobapplications.dto.request.CompanyUpdateRequest;
import com.lifehub.jobapplications.dto.response.CompanyResponse;
import com.lifehub.jobapplications.entity.Company;
import com.lifehub.jobapplications.repository.CompanyRepository;
import com.lifehub.jobapplications.repository.JobApplicationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final JobApplicationRepository jobApplicationRepository;

    public CompanyService(CompanyRepository companyRepository, JobApplicationRepository jobApplicationRepository) {
        this.companyRepository = companyRepository;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @Transactional
    public CompanyResponse create(Long userId, CompanyCreateRequest request) {
        Company company = new Company(userId, request.name(), request.industry(), request.url(), request.notes());
        return CompanyResponse.from(companyRepository.save(company));
    }

    public List<CompanyResponse> list(Long userId) {
        return companyRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CompanyResponse::from)
                .toList();
    }

    public CompanyResponse get(Long userId, Long companyId) {
        return CompanyResponse.from(findOwnedOrThrow(userId, companyId));
    }

    @Transactional
    public CompanyResponse update(Long userId, Long companyId, CompanyUpdateRequest request) {
        Company company = findOwnedOrThrow(userId, companyId);
        company.update(request.name(), request.industry(), request.url(), request.notes());
        // See TROUBLESHOOTING.md ("updatedAt이 flush 이전 값"): flush now so the response reflects
        // the refreshed @LastModifiedDate instead of the pre-flush in-memory value.
        companyRepository.flush();
        return CompanyResponse.from(company);
    }

    @Transactional
    public void delete(Long userId, Long companyId) {
        Company company = findOwnedOrThrow(userId, companyId);
        if (jobApplicationRepository.existsByCompanyId(companyId)) {
            throw new ConflictException("이 회사를 참조하는 지원 현황이 있어 삭제할 수 없습니다.");
        }
        companyRepository.delete(company);
    }

    private Company findOwnedOrThrow(Long userId, Long companyId) {
        return companyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
    }
}
