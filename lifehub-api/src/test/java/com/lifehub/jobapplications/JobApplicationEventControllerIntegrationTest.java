package com.lifehub.jobapplications;

import com.lifehub.jobapplications.dto.request.JobApplicationEventCreateRequest;
import com.lifehub.jobapplications.dto.request.JobApplicationEventUpdateRequest;
import com.lifehub.jobapplications.entity.JobApplication;
import com.lifehub.jobapplications.entity.JobApplicationEventResult;
import com.lifehub.jobapplications.entity.JobApplicationEventType;
import com.lifehub.jobapplications.entity.JobApplicationStatus;
import com.lifehub.jobapplications.repository.JobApplicationRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobApplicationEventControllerIntegrationTest extends AbstractJobApplicationsIntegrationTest {

    private static final Long SEED_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Test
    void create_resultOmitted_defaultsToPending() throws Exception {
        long jobApplicationId = createCompanyAndJobApplication();
        var request = new JobApplicationEventCreateRequest(
                JobApplicationEventType.DOCUMENT_SUBMITTED, LocalDate.parse("2025-03-01"), null, "서류 제출");

        mockMvc.perform(postJson("/api/job-applications/" + jobApplicationId + "/events", request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("DOCUMENT_SUBMITTED"))
                .andExpect(jsonPath("$.result").value("PENDING"))
                .andExpect(jsonPath("$.jobApplicationId").value(jobApplicationId));
    }

    @Test
    void create_withExplicitResult() throws Exception {
        long jobApplicationId = createCompanyAndJobApplication();
        var request = new JobApplicationEventCreateRequest(
                JobApplicationEventType.DOCUMENT_RESULT, LocalDate.parse("2025-03-05"),
                JobApplicationEventResult.PASS, "서류 합격");

        mockMvc.perform(postJson("/api/job-applications/" + jobApplicationId + "/events", request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("PASS"));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        long jobApplicationId = createCompanyAndJobApplication();
        String rawJsonMissingFields = "{\"memo\": \"타입/날짜 없음\"}";

        mockMvc.perform(post("/api/job-applications/" + jobApplicationId + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJsonMissingFields))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void create_forNonExistentJobApplication_returns404() throws Exception {
        var request = new JobApplicationEventCreateRequest(
                JobApplicationEventType.DOCUMENT_SUBMITTED, LocalDate.parse("2025-03-01"), null, null);

        mockMvc.perform(postJson("/api/job-applications/999999/events", request))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_returnsEventsOrderedByEventDateAscending() throws Exception {
        long jobApplicationId = createCompanyAndJobApplication();
        createEvent(jobApplicationId, JobApplicationEventType.DOCUMENT_RESULT, "2025-03-15", null);
        createEvent(jobApplicationId, JobApplicationEventType.DOCUMENT_SUBMITTED, "2025-03-01", null);

        mockMvc.perform(get("/api/job-applications/" + jobApplicationId + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].eventDate").value("2025-03-01"))
                .andExpect(jsonPath("$[1].eventDate").value("2025-03-15"));
    }

    @Test
    void list_forNonExistentJobApplication_returns404() throws Exception {
        mockMvc.perform(get("/api/job-applications/999999/events"))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_nonExistentEvent_returns404() throws Exception {
        long jobApplicationId = createCompanyAndJobApplication();

        mockMvc.perform(get("/api/job-applications/" + jobApplicationId + "/events/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingEvent_updatesFieldsAndRefreshesUpdatedAt() throws Exception {
        long jobApplicationId = createCompanyAndJobApplication();
        long eventId = createEvent(jobApplicationId, JobApplicationEventType.DOCUMENT_SUBMITTED, "2025-03-01", null);
        String created = mockMvc.perform(get("/api/job-applications/" + jobApplicationId + "/events/" + eventId))
                .andReturn().getResponse().getContentAsString();
        String originalUpdatedAt = toJsonNode(created).get("updatedAt").asText();

        var updateRequest = new JobApplicationEventUpdateRequest(
                JobApplicationEventType.DOCUMENT_SUBMITTED, LocalDate.parse("2025-03-02"),
                JobApplicationEventResult.PASS, "수정됨");

        MvcResult result = mockMvc.perform(putJson(
                        "/api/job-applications/" + jobApplicationId + "/events/" + eventId, updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("PASS"))
                .andExpect(jsonPath("$.memo").value("수정됨"))
                .andReturn();

        String newUpdatedAt = toJsonNode(result.getResponse().getContentAsString()).get("updatedAt").asText();
        assertThat(newUpdatedAt).isNotEqualTo(originalUpdatedAt);
    }

    @Test
    void update_missingResult_returns400() throws Exception {
        long jobApplicationId = createCompanyAndJobApplication();
        long eventId = createEvent(jobApplicationId, JobApplicationEventType.DOCUMENT_SUBMITTED, "2025-03-01", null);

        String rawJsonWithoutResult = """
                {
                  "eventType": "DOCUMENT_SUBMITTED",
                  "eventDate": "2025-03-02"
                }
                """;

        mockMvc.perform(put("/api/job-applications/" + jobApplicationId + "/events/" + eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJsonWithoutResult))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void delete_existingEvent_returns204AndThenIsGone() throws Exception {
        long jobApplicationId = createCompanyAndJobApplication();
        long eventId = createEvent(jobApplicationId, JobApplicationEventType.DOCUMENT_SUBMITTED, "2025-03-01", null);

        mockMvc.perform(delete("/api/job-applications/" + jobApplicationId + "/events/" + eventId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/job-applications/" + jobApplicationId + "/events/" + eventId))
                .andExpect(status().isNotFound());
    }

    @Test
    void eventId_belongingToDifferentJobApplication_returns404() throws Exception {
        long jobApplicationA = createCompanyAndJobApplication();
        long jobApplicationB = createCompanyAndJobApplication();
        long eventOfA = createEvent(jobApplicationA, JobApplicationEventType.DOCUMENT_SUBMITTED, "2025-03-01", null);

        // The event exists (under jobApplicationA), but is addressed through jobApplicationB's path.
        mockMvc.perform(get("/api/job-applications/" + jobApplicationB + "/events/" + eventOfA))
                .andExpect(status().isNotFound());
    }

    /**
     * The critical case from the design review: creating an event under a job application that
     * belongs to a DIFFERENT user must be blocked, even though every HTTP call in this app is
     * always authenticated as the one seed user (there is no real multi-user login yet — see
     * TROUBLESHOOTING.md / README "아직 구현되지 않은 기능").
     * <p>
     * "Another user's data" is simulated by inserting a JobApplication directly via the repository
     * (bypassing the API, which would never let the seed user create a row owned by someone else),
     * then hitting the real HTTP event-creation endpoint — authenticated as the seed user, as every
     * request in this app is — against that foreign id.
     */
    @Test
    void create_forJobApplicationOwnedByAnotherUser_isBlockedWith404() throws Exception {
        long companyId = createCompany("다른 사용자 소유 회사");
        JobApplication foreignJobApplication = jobApplicationRepository.save(new JobApplication(
                OTHER_USER_ID, companyId, "다른 사용자의 지원건", null,
                JobApplicationStatus.PREPARING, null, null));

        var request = new JobApplicationEventCreateRequest(
                JobApplicationEventType.DOCUMENT_SUBMITTED, LocalDate.parse("2025-03-01"), null, null);

        mockMvc.perform(postJson(
                        "/api/job-applications/" + foreignJobApplication.getId() + "/events", request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        assertThat(jobApplicationRepository.findByIdAndUserId(foreignJobApplication.getId(), SEED_USER_ID))
                .as("seed user must not be considered the owner of another user's job application")
                .isEmpty();
    }

    @Test
    void list_forJobApplicationOwnedByAnotherUser_isBlockedWith404() throws Exception {
        long companyId = createCompany("다른 사용자 소유 회사");
        JobApplication foreignJobApplication = jobApplicationRepository.save(new JobApplication(
                OTHER_USER_ID, companyId, "다른 사용자의 지원건", null,
                JobApplicationStatus.PREPARING, null, null));

        mockMvc.perform(get("/api/job-applications/" + foreignJobApplication.getId() + "/events"))
                .andExpect(status().isNotFound());
    }

    private long createCompanyAndJobApplication() throws Exception {
        long companyId = createCompany("회사");
        return createJobApplication(companyId, "포지션");
    }

    private long createEvent(long jobApplicationId, JobApplicationEventType type, String eventDate,
                              JobApplicationEventResult result) throws Exception {
        var request = new JobApplicationEventCreateRequest(type, LocalDate.parse(eventDate), result, null);
        return postAndGetId("/api/job-applications/" + jobApplicationId + "/events", request);
    }
}
