package com.realestate.repository;

import com.realestate.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserAccount u where lower(u.emailId) = lower(:email)")
    Optional<UserAccount> lockByEmail(@org.springframework.data.repository.query.Param("email") String email);
    boolean existsByEmailIdIgnoreCase(String emailId);
    Optional<UserAccount> findByEmailIdIgnoreCase(String emailId);
    Optional<UserAccount> findByEmailIdIgnoreCaseAndRole(String emailId, String role);
    Optional<UserAccount> findByEmailIdIgnoreCaseAndPasswordAndRole(String emailId, String password, String role);
}
