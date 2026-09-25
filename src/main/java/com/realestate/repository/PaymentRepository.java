package com.realestate.repository;

import com.realestate.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    java.util.List<Payment> findByLead_IdOrderByIdAsc(Long id);
    boolean existsByLead_Id(Long id);
}
