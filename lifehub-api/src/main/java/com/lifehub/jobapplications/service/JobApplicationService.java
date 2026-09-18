package com.lifehub.jobapplications.service;

import com.lifehub.common.exception.ConflictException;
import com.lifehub.common.exception.ResourceNotFoundException;
import com.lifehub.jobapplications.dto.request.JobApplicationCreateRequest;
import com.lifehub.jobapplications.dto.request.JobApplicationUpdateRequest;
import com.lifehub.jobapplications.dto.response.JobApplicationResponse;
import com.lifehub.jobapplications.entity.JobApplication;
import com.lifehub.jobapplications.entity.JobApplicationStatus;
import com.lifehub.jobapplications.repository.CompanyRepository;
import com.lifehub.jobapplications.repository.JobApplicationEventRepository;
import com.lifehub.jobapplications.repository.JobApplicationRepository;
import com.lifehub.jobapplications.repository.JobApplicationSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobApplicationService {

    private static final JobApplicationStatus DEFAULT_STATUS = JobApplicationStatus.PREPARING;

    private final JobApplicationRepository jobApplicationRepository;
    private final CompanyRepository companyRepository;
    private final JobApplicationEventRepository jobApplicationEventRepository;

    public JobApplicationService(JobApplicationRepository jobApplicationRepository,
                                  CompanyRepository companyRepository,
                                  JobApplicationEventRepository jobApplicationEventRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.companyRepository = companyRepository;
        this.jobApplicationEventRepository = jobApplicationEventRepository;
    }

    @Transactional
    public JobApplicationResponse create(Long userId, JobApplicationCreateRequest request) {
        requireOwnedCompany(userId, request.companyId());
        JobApplication jobApplication = new JobApplication(
                userId, request.companyId(), request.positionTitle(), request.applyUrl(),
                request.status() == null ? DEFAULT_STATUS : request.status(),
                request.appliedAt(), request.notes());
        return JobApplicationResponse.from(jobApplicationRepository.save(jobApplication));
    }

    public Page<JobApplicationResponse> search(Long userId, Long companyId, JobApplicationStatus status,
                                                Pageable pageable) {
        Specification<JobApplication> spec = Specification
                .where(JobApplicationSpecifications.userIdEquals(userId))
                .and(JobApplicationSpecifications.companyIdEquals(companyId))
                .and(JobApplicationSpecifications.statusEquals(status));
        return jobApplicationRepository.findAll(spec, pageable).map(JobApplicationResponse::from);
    }

    public JobApplicationResponse get(Long userId, Long jobApplicationId) {
        return JobApplicationResponse.from(findOwnedOrThrow(userId, jobApplicationId));
    }

    @Transactional
    public JobApplicationResponse update(Long userId, Long jobApplicationId, JobApplicationUpdateRequest request) {
        JobApplication jobApplication = findOwnedOrThrow(userId, jobApplicationId);
        requireOwnedCompany(userId, request.companyId());
        jobApplication.update(request.companyId(), request.positionTitle(), request.applyUrl(),
                request.status(), request.appliedAt(), request.notes());
        // See TROUBLESHOOTING.md ("updatedAt이 flush 이전 값"): flush now so the response reflects
        // the refreshed @LastModifiedDate instead of the pre-flush in-memory value.
        jobApplicationRepository.flush();
        return JobApplicationResponse.from(jobApplication);
    }

    @Transactional
    public void delete(Long userId, Long jobApplicationId) {
        JobApplication jobApplication = findOwnedOrThrow(userId, jobApplicationId);
        if (jobApplicationEventRepository.existsByJobApplicationId(jobApplicationId)) {
            throw new ConflictException("이 지원 건을 참조하는 전형 이력이 있어 삭제할 수 없습니다.");
        }
        jobApplicationRepository.delete(jobApplication);
    }

    private JobApplication findOwnedOrThrow(Long userId, Long jobApplicationId) {
        return jobApplicationRepository.findByIdAndUserId(jobApplicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication not found: " + jobApplicationId));
    }

    private void requireOwnedCompany(Long userId, Long companyId) {
        if (companyRepository.findByIdAndUserId(companyId, userId).isEmpty()) {
            throw new ResourceNotFoundException("Company not found: " + companyId);
        }
    }
}
