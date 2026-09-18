package com.lifehub.jobapplications;

import com.lifehub.jobapplications.dto.request.JobApplicationCreateRequest;
import com.lifehub.jobapplications.dto.request.JobApplicationEventCreateRequest;
import com.lifehub.jobapplications.dto.request.JobApplicationUpdateRequest;
import com.lifehub.jobapplications.entity.JobApplicationEventType;
import com.lifehub.jobapplications.entity.JobApplicationStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobApplicationControllerIntegrationTest extends AbstractJobApplicationsIntegrationTest {

    @Test
    void create_statusOmitted_defaultsToPreparing() throws Exception {
        long companyId = createCompany("회사");
        var request = new JobApplicationCreateRequest(companyId, "백엔드 엔지니어", null, null, null, null);

        mockMvc.perform(postJson("/api/job-applications", request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.positionTitle").value("백엔드 엔지니어"))
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void create_withExplicitStatusAndAppliedAt() throws Exception {
        long companyId = createCompany("회사");
        var request = new JobApplicationCreateRequest(
                companyId, "프론트엔드 엔지니어", "https://example.com/jobs/1",
                JobApplicationStatus.APPLIED, LocalDate.parse("2025-03-01"), "메모");

        mockMvc.perform(postJson("/api/job-applications", request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedAt").value("2025-03-01"));
    }

    @Test
    void create_nonExistentCompany_returns404() throws Exception {
        var request = new JobApplicationCreateRequest(999999L, "포지션", null, null, null, null);

        mockMvc.perform(postJson("/api/job-applications", request))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_blankPositionTitle_returns400() throws Exception {
        long companyId = createCompany("회사");
        var request = new JobApplicationCreateRequest(companyId, "  ", null, null, null, null);

        mockMvc.perform(postJson("/api/job-applications", request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void get_nonExistentJobApplication_returns404() throws Exception {
        mockMvc.perform(get("/api/job-applications/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingJobApplication_updatesFieldsAndRefreshesUpdatedAt() throws Exception {
        long companyId = createCompany("회사");
        long id = createJobApplication(companyId, "백엔드 엔지니어");
        String created = mockMvc.perform(get("/api/job-applications/" + id))
                .andReturn().getResponse().getContentAsString();
        String originalUpdatedAt = toJsonNode(created).get("updatedAt").asText();

        var updateRequest = new JobApplicationUpdateRequest(
                companyId, "백엔드 엔지니어", null, JobApplicationStatus.APPLIED, LocalDate.parse("2025-03-05"), null);

        MvcResult result = mockMvc.perform(putJson("/api/job-applications/" + id, updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andReturn();

        String newUpdatedAt = toJsonNode(result.getResponse().getContentAsString()).get("updatedAt").asText();
        assertThat(newUpdatedAt).isNotEqualTo(originalUpdatedAt);
    }

    @Test
    void update_missingStatus_returns400() throws Exception {
        long companyId = createCompany("회사");
        long id = createJobApplication(companyId, "백엔드 엔지니어");

        String rawJsonWithoutStatus = """
                {
                  "companyId": %d,
                  "positionTitle": "백엔드 엔지니어"
                }
                """.formatted(companyId);

        mockMvc.perform(put("/api/job-applications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJsonWithoutStatus))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void update_nonExistentCompanyId_returns404() throws Exception {
        long companyId = createCompany("회사");
        long id = createJobApplication(companyId, "백엔드 엔지니어");

        var updateRequest = new JobApplicationUpdateRequest(
                999999L, "백엔드 엔지니어", null, JobApplicationStatus.APPLIED, null, null);

        mockMvc.perform(putJson("/api/job-applications/" + id, updateRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_filtersByCompanyAndStatusWithPaging() throws Exception {
        long companyA = createCompany("회사A");
        long companyB = createCompany("회사B");
        createJobApplication(companyA, "포지션1", JobApplicationStatus.PREPARING);
        createJobApplication(companyA, "포지션2", JobApplicationStatus.APPLIED);
        createJobApplication(companyB, "포지션3", JobApplicationStatus.APPLIED);

        mockMvc.perform(get("/api/job-applications").param("companyId", String.valueOf(companyA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/job-applications").param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/job-applications")
                        .param("companyId", String.valueOf(companyA))
                        .param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/job-applications").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void delete_existingJobApplication_returns204AndThenIsGone() throws Exception {
        long companyId = createCompany("회사");
        long id = createJobApplication(companyId, "삭제될지원");

        mockMvc.perform(delete("/api/job-applications/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/job-applications/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_jobApplicationReferencedByEvent_returns409() throws Exception {
        long companyId = createCompany("회사");
        long jobApplicationId = createJobApplication(companyId, "참조지원");
        var eventRequest = new JobApplicationEventCreateRequest(
                JobApplicationEventType.DOCUMENT_SUBMITTED, LocalDate.parse("2025-03-01"), null, null);
        postAndGetId("/api/job-applications/" + jobApplicationId + "/events", eventRequest);

        mockMvc.perform(delete("/api/job-applications/" + jobApplicationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}
