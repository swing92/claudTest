package com.lifehub.jobapplications.repository;

import com.lifehub.jobapplications.entity.JobApplicationEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationEventRepository extends JpaRepository<JobApplicationEvent, Long> {

    List<JobApplicationEvent> findAllByJobApplicationIdAndUserIdOrderByEventDateAsc(
            Long jobApplicationId, Long userId);

    Optional<JobApplicationEvent> findByIdAndJobApplicationIdAndUserId(
            Long id, Long jobApplicationId, Long userId);

    boolean existsByJobApplicationId(Long jobApplicationId);
}
