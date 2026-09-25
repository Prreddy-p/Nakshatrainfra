package com.realestate.repository;

import com.realestate.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @org.springframework.data.jpa.repository.Query("select (count(c) > 0) from Customer c where c.lead.id = :leadId")
    boolean existsByLeadId(@org.springframework.data.repository.query.Param("leadId") Long leadId);
}
