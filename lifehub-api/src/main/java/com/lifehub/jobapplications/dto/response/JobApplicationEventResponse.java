package com.lifehub.jobapplications.dto.response;

import com.lifehub.jobapplications.entity.JobApplicationEvent;
import com.lifehub.jobapplications.entity.JobApplicationEventResult;
import com.lifehub.jobapplications.entity.JobApplicationEventType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobApplicationEventResponse(
        Long id,
        Long jobApplicationId,
        JobApplicationEventType eventType,
        LocalDate eventDate,
        JobApplicationEventResult result,
        String memo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static JobApplicationEventResponse from(JobApplicationEvent event) {
        return new JobApplicationEventResponse(
                event.getId(),
                event.getJobApplicationId(),
                event.getEventType(),
                event.getEventDate(),
                event.getResult(),
                event.getMemo(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
