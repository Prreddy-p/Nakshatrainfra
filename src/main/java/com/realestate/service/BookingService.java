package com.realestate.service;

import com.realestate.model.Booking;
import com.realestate.model.Property;
import com.realestate.repository.BookingRepository;
import com.realestate.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingService {
    private static final List<String> ACTIVE_STATUSES = List.of("Hold", "Booked", "Confirmed");
    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;

    public BookingService(BookingRepository bookingRepository, PropertyRepository propertyRepository) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
    }

    @Transactional
    public Booking create(Booking booking) {
        Property property = propertyRepository.findById(booking.getProperty().getId())
                .orElseThrow(() -> new IllegalArgumentException("Property was not found"));
        if (bookingRepository.existsByPropertyIdAndStatusIn(property.getId(), ACTIVE_STATUSES)) {
            throw new IllegalStateException("This property already has an active booking");
        }
        property.setStatus("Booked");
        booking.setProperty(property);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking updateStatus(Long id, String status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking was not found"));
        booking.setStatus(status);
        if ("Cancelled".equalsIgnoreCase(status)) {
            booking.getProperty().setStatus("Available");
        } else if (ACTIVE_STATUSES.contains(status)) {
            booking.getProperty().setStatus("Booked");
        }
        return bookingRepository.save(booking);
    }
}
