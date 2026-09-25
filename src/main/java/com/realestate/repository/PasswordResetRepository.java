package com.realestate.repository;

import com.realestate.model.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    @org.springframework.data.jpa.repository.Query("select p.user.emailId from PasswordReset p where p.resetTokenHash = :hash and p.consumedAt is null")
    Optional<String> findEmailByTokenHash(@org.springframework.data.repository.query.Param("hash") String hash);
    java.util.List<PasswordReset> findByUserAndConsumedAtIsNull(com.realestate.model.UserAccount user);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordReset> findTopByUser_EmailIdIgnoreCaseOrderByCreatedAtDesc(String emailId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordReset> findByResetTokenHashAndConsumedAtIsNull(String resetTokenHash);
}
