package com.lifehub.jobapplications;

import com.lifehub.jobapplications.dto.request.CompanyCreateRequest;
import com.lifehub.jobapplications.dto.request.CompanyUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompanyControllerIntegrationTest extends AbstractJobApplicationsIntegrationTest {

    @Test
    void create_returnsCreatedCompany() throws Exception {
        var request = new CompanyCreateRequest("테크스타트업", "IT", "https://example.com", "관심 회사");

        mockMvc.perform(postJson("/api/companies", request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("테크스타트업"))
                .andExpect(jsonPath("$.industry").value("IT"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        var request = new CompanyCreateRequest("", null, null, null);

        mockMvc.perform(postJson("/api/companies", request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void list_returnsAllCreatedCompanies() throws Exception {
        createCompany("회사1");
        createCompany("회사2");

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void get_nonExistentCompany_returns404() throws Exception {
        mockMvc.perform(get("/api/companies/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingCompany_updatesFieldsAndRefreshesUpdatedAt() throws Exception {
        long id = createCompany("이전이름");
        String created = mockMvc.perform(get("/api/companies/" + id))
                .andReturn().getResponse().getContentAsString();
        String originalUpdatedAt = toJsonNode(created).get("updatedAt").asText();

        var updateRequest = new CompanyUpdateRequest("새이름", "금융", null, null);

        MvcResult result = mockMvc.perform(putJson("/api/companies/" + id, updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("새이름"))
                .andExpect(jsonPath("$.industry").value("금융"))
                .andReturn();

        String newUpdatedAt = toJsonNode(result.getResponse().getContentAsString()).get("updatedAt").asText();
        assertThat(newUpdatedAt).isNotEqualTo(originalUpdatedAt);
    }

    @Test
    void update_nonExistentCompany_returns404() throws Exception {
        var updateRequest = new CompanyUpdateRequest("이름", null, null, null);

        mockMvc.perform(putJson("/api/companies/999999", updateRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingCompany_returns204AndThenIsGone() throws Exception {
        long id = createCompany("삭제될회사");

        mockMvc.perform(delete("/api/companies/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/companies/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_companyReferencedByJobApplication_returns409() throws Exception {
        long companyId = createCompany("참조회사");
        createJobApplication(companyId, "백엔드 엔지니어");

        mockMvc.perform(delete("/api/companies/" + companyId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}
