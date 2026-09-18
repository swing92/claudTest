package com.lifehub.tasks;

import com.lifehub.AbstractIntegrationTest;
import com.lifehub.tasks.dto.request.TaskCreateRequest;
import com.lifehub.tasks.dto.request.TaskUpdateRequest;
import com.lifehub.tasks.entity.TaskPriority;
import com.lifehub.tasks.entity.TaskType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void create_todoWithDueAt_returnsCreatedTaskWithDefaults() throws Exception {
        var request = new TaskCreateRequest(
                "보고서 작성", "분기 보고서", TaskType.TODO,
                LocalDateTime.parse("2025-09-25T18:00:00"), null, null, null, null);

        mockMvc.perform(postJson("/api/tasks", request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("보고서 작성"))
                .andExpect(jsonPath("$.type").value("TODO"))
                .andExpect(jsonPath("$.isAllDay").value(false))
                .andExpect(jsonPath("$.isCompleted").value(false))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        var request = new TaskCreateRequest(
                "  ", null, TaskType.TODO, null, null, null, null, null);

        mockMvc.perform(postJson("/api/tasks", request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void create_endAtBeforeStartAt_returns400() throws Exception {
        var request = new TaskCreateRequest(
                "잘못된 일정", null, TaskType.EVENT,
                null, LocalDateTime.parse("2025-09-20T11:00:00"), LocalDateTime.parse("2025-09-20T10:00:00"),
                null, null);

        mockMvc.perform(postJson("/api/tasks", request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void get_nonExistentTask_returns404() throws Exception {
        mockMvc.perform(get("/api/tasks/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingTask_updatesFieldsAndRefreshesUpdatedAt() throws Exception {
        long id = createTask("팀 회의", TaskType.EVENT,
                LocalDateTime.parse("2025-09-20T10:00:00"), LocalDateTime.parse("2025-09-20T11:00:00"));
        String created = mockMvc.perform(get("/api/tasks/" + id))
                .andReturn().getResponse().getContentAsString();
        String originalUpdatedAt = toJsonNode(created).get("updatedAt").asText();

        var updateRequest = new TaskUpdateRequest(
                "팀 회의(변경)", null, TaskType.EVENT,
                null, LocalDateTime.parse("2025-09-20T14:00:00"), LocalDateTime.parse("2025-09-20T15:00:00"),
                false, TaskPriority.LOW);

        MvcResult result = mockMvc.perform(putJson("/api/tasks/" + id, updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("팀 회의(변경)"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andReturn();

        String newUpdatedAt = toJsonNode(result.getResponse().getContentAsString()).get("updatedAt").asText();
        assertThat(newUpdatedAt).isNotEqualTo(originalUpdatedAt);
    }

    @Test
    void update_endAtBeforeStartAt_returns400() throws Exception {
        long id = createTask("일정", TaskType.EVENT,
                LocalDateTime.parse("2025-09-20T10:00:00"), LocalDateTime.parse("2025-09-20T11:00:00"));

        var updateRequest = new TaskUpdateRequest(
                "일정", null, TaskType.EVENT,
                null, LocalDateTime.parse("2025-09-20T12:00:00"), LocalDateTime.parse("2025-09-20T09:00:00"),
                null, null);

        mockMvc.perform(putJson("/api/tasks/" + id, updateRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_nonExistentTask_returns404() throws Exception {
        var updateRequest = new TaskUpdateRequest("제목", null, TaskType.TODO, null, null, null, null, null);

        mockMvc.perform(putJson("/api/tasks/999999", updateRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    void complete_setsIsCompletedTrueAndCompletedAt() throws Exception {
        long id = createTask("할일", TaskType.TODO, null, null);

        mockMvc.perform(patch("/api/tasks/" + id + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    void incomplete_afterComplete_clearsCompletedAt() throws Exception {
        long id = createTask("할일", TaskType.TODO, null, null);
        mockMvc.perform(patch("/api/tasks/" + id + "/complete")).andExpect(status().isOk());

        mockMvc.perform(patch("/api/tasks/" + id + "/incomplete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(false))
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    @Test
    void update_doesNotAffectCompletionState() throws Exception {
        long id = createTask("할일", TaskType.TODO, null, null);
        mockMvc.perform(patch("/api/tasks/" + id + "/complete")).andExpect(status().isOk());

        var updateRequest = new TaskUpdateRequest("할일(제목변경)", null, TaskType.TODO, null, null, null, null, null);

        mockMvc.perform(putJson("/api/tasks/" + id, updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("할일(제목변경)"))
                // TaskUpdateRequest has no isCompleted/completedAt field at all, so completing earlier
                // must survive a plain content update untouched.
                .andExpect(jsonPath("$.isCompleted").value(true));
    }

    @Test
    void delete_existingTask_returns204AndThenIsGone() throws Exception {
        long id = createTask("삭제될할일", TaskType.TODO, null, null);

        mockMvc.perform(delete("/api/tasks/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_nonExistentTask_returns404() throws Exception {
        mockMvc.perform(delete("/api/tasks/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_filtersByTypeCompletionAndDueRangeWithPaging() throws Exception {
        long todo1 = createTask("할일1", TaskType.TODO, LocalDateTime.parse("2025-10-01T09:00:00"), null);
        long todo2 = createTask("할일2", TaskType.TODO, LocalDateTime.parse("2025-10-15T09:00:00"), null);
        createTask("할일3(다음달)", TaskType.TODO, LocalDateTime.parse("2025-11-01T09:00:00"), null);
        createTask("일정1", TaskType.EVENT,
                LocalDateTime.parse("2025-10-05T09:00:00"), LocalDateTime.parse("2025-10-05T10:00:00"));
        mockMvc.perform(patch("/api/tasks/" + todo1 + "/complete")).andExpect(status().isOk());

        // type filter
        mockMvc.perform(get("/api/tasks").param("type", "EVENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // isCompleted filter
        mockMvc.perform(get("/api/tasks").param("isCompleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(todo1));

        // dueAt range filter (October TODOs only)
        mockMvc.perform(get("/api/tasks")
                        .param("type", "TODO")
                        .param("dueFrom", "2025-10-01T00:00:00")
                        .param("dueTo", "2025-10-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // paging
        mockMvc.perform(get("/api/tasks").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(4));

        assertThat(todo2).isPositive();
    }

    /** For TaskType.TODO, {@code startAt} is used as the task's dueAt; {@code endAt} is ignored. */
    private long createTask(String title, TaskType type, LocalDateTime startAt, LocalDateTime endAt) throws Exception {
        TaskCreateRequest request = type == TaskType.TODO
                ? new TaskCreateRequest(title, null, TaskType.TODO, startAt, null, null, null, null)
                : new TaskCreateRequest(title, null, TaskType.EVENT, null, startAt, endAt, null, null);

        MvcResult result = mockMvc.perform(postJson("/api/tasks", request))
                .andExpect(status().isCreated())
                .andReturn();
        return toJsonNode(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
