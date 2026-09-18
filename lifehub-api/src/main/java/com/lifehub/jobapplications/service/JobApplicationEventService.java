package com.lifehub.jobapplications.service;

import com.lifehub.common.exception.ResourceNotFoundException;
import com.lifehub.jobapplications.dto.request.JobApplicationEventCreateRequest;
import com.lifehub.jobapplications.dto.request.JobApplicationEventUpdateRequest;
import com.lifehub.jobapplications.dto.response.JobApplicationEventResponse;
import com.lifehub.jobapplications.entity.JobApplication;
import com.lifehub.jobapplications.entity.JobApplicationEvent;
import com.lifehub.jobapplications.entity.JobApplicationEventResult;
import com.lifehub.jobapplications.repository.JobApplicationEventRepository;
import com.lifehub.jobapplications.repository.JobApplicationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobApplicationEventService {

    private static final JobApplicationEventResult DEFAULT_RESULT = JobApplicationEventResult.PENDING;

    private final JobApplicationEventRepository jobApplicationEventRepository;
    private final JobApplicationRepository jobApplicationRepository;

    public JobApplicationEventService(JobApplicationEventRepository jobApplicationEventRepository,
                                       JobApplicationRepository jobApplicationRepository) {
        this.jobApplicationEventRepository = jobApplicationEventRepository;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @Transactional
    public JobApplicationEventResponse create(Long userId, Long jobApplicationId,
                                               JobApplicationEventCreateRequest request) {
        JobApplication jobApplication = requireOwnedJobApplication(userId, jobApplicationId);
        // event.userId is derived from the verified parent (not from the `userId` parameter directly)
        // so it is structurally impossible for the denormalized column to diverge from its parent's.
        JobApplicationEvent event = new JobApplicationEvent(
                jobApplication.getUserId(), jobApplication.getId(), request.eventType(), request.eventDate(),
                request.result() == null ? DEFAULT_RESULT : request.result(), request.memo());
        return JobApplicationEventResponse.from(jobApplicationEventRepository.save(event));
    }

    public List<JobApplicationEventResponse> list(Long userId, Long jobApplicationId) {
        requireOwnedJobApplication(userId, jobApplicationId);
        return jobApplicationEventRepository
                .findAllByJobApplicationIdAndUserIdOrderByEventDateAsc(jobApplicationId, userId).stream()
                .map(JobApplicationEventResponse::from)
                .toList();
    }

    public JobApplicationEventResponse get(Long userId, Long jobApplicationId, Long eventId) {
        return JobApplicationEventResponse.from(findOwnedOrThrow(userId, jobApplicationId, eventId));
    }

    @Transactional
    public JobApplicationEventResponse update(Long userId, Long jobApplicationId, Long eventId,
                                               JobApplicationEventUpdateRequest request) {
        JobApplicationEvent event = findOwnedOrThrow(userId, jobApplicationId, eventId);
        event.update(request.eventType(), request.eventDate(), request.result(), request.memo());
        // See TROUBLESHOOTING.md ("updatedAt이 flush 이전 값"): flush now so the response reflects
        // the refreshed @LastModifiedDate instead of the pre-flush in-memory value.
        jobApplicationEventRepository.flush();
        return JobApplicationEventResponse.from(event);
    }

    @Transactional
    public void delete(Long userId, Long jobApplicationId, Long eventId) {
        JobApplicationEvent event = findOwnedOrThrow(userId, jobApplicationId, eventId);
        jobApplicationEventRepository.delete(event);
    }

    private JobApplicationEvent findOwnedOrThrow(Long userId, Long jobApplicationId, Long eventId) {
        return jobApplicationEventRepository.findByIdAndJobApplicationIdAndUserId(eventId, jobApplicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplicationEvent not found: " + eventId));
    }

    /**
     * Looked up by id alone (not id+userId) so the ownership comparison below is a real check,
     * not folded invisibly into the query filter. This is the explicit guard for the
     * job_application_event.user_id == job_application.user_id invariant described in
     * docs/table-definition.md — Postgres can't express it as a CHECK constraint since that
     * would need to reference another table's row.
     */
    private JobApplication requireOwnedJobApplication(Long userId, Long jobApplicationId) {
        JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication not found: " + jobApplicationId));
        if (!jobApplication.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("JobApplication not found: " + jobApplicationId);
        }
        return jobApplication;
    }
}
