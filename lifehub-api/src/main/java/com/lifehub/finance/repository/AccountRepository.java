package com.lifehub.finance.repository;

import com.lifehub.finance.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);
}
