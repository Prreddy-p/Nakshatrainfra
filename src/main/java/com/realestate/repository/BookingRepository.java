package com.realestate.repository;

import com.realestate.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByPropertyIdAndStatusIn(Long propertyId, List<String> statuses);
    List<Booking> findByPropertyId(Long propertyId);
}
