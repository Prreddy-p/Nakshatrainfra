package com.realestate.repository;

import com.realestate.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select l from Lead l where l.id = :id")
    java.util.Optional<Lead> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    List<Lead> findByLeadStatusIgnoreCase(String status);
    List<Lead> findByCategoryIgnoreCase(String category);
}
