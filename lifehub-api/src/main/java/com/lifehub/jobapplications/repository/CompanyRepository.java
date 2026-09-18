package com.lifehub.jobapplications.repository;

import com.lifehub.jobapplications.entity.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Company> findByIdAndUserId(Long id, Long userId);
}
