package com.lifehub.jobapplications;

import com.lifehub.AbstractIntegrationTest;
import com.lifehub.jobapplications.dto.request.CompanyCreateRequest;
import com.lifehub.jobapplications.dto.request.JobApplicationCreateRequest;
import com.lifehub.jobapplications.entity.JobApplicationStatus;

abstract class AbstractJobApplicationsIntegrationTest extends AbstractIntegrationTest {

    protected long createCompany(String name) throws Exception {
        return postAndGetId("/api/companies", new CompanyCreateRequest(name, null, null, null));
    }

    protected long createJobApplication(long companyId, String positionTitle) throws Exception {
        var request = new JobApplicationCreateRequest(companyId, positionTitle, null, null, null, null);
        return postAndGetId("/api/job-applications", request);
    }

    protected long createJobApplication(long companyId, String positionTitle, JobApplicationStatus status) throws Exception {
        var request = new JobApplicationCreateRequest(companyId, positionTitle, null, status, null, null);
        return postAndGetId("/api/job-applications", request);
    }
}
