package com.lifehub.jobapplications.repository;

import com.lifehub.jobapplications.entity.JobApplication;
import com.lifehub.jobapplications.entity.JobApplicationStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Optional-filter predicates built only for non-null inputs — see
 * finance/repository/TransactionSpecifications and TROUBLESHOOTING.md for why a single JPQL query
 * with "(:param IS NULL OR field = :param)" is avoided (PostgreSQL can't infer that parameter's
 * type from an IS NULL-only comparison).
 */
public final class JobApplicationSpecifications {

    private JobApplicationSpecifications() {
    }

    public static Specification<JobApplication> userIdEquals(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<JobApplication> companyIdEquals(Long companyId) {
        return companyId == null ? null : (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<JobApplication> statusEquals(JobApplicationStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
