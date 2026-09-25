package com.realestate.service;

import com.realestate.model.Booking;
import com.realestate.model.Property;
import com.realestate.repository.BookingRepository;
import com.realestate.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBooksAvailableProperty() {
        Property property = property(7L);
        Booking booking = booking(property);
        when(propertyRepository.findById(7L)).thenReturn(Optional.of(property));
        when(bookingRepository.existsByPropertyIdAndStatusIn(any(), anyList())).thenReturn(false);
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = bookingService.create(booking);

        assertSame(booking, result);
        assertSame(property, result.getProperty());
        assertEquals("Booked", property.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void createRejectsMissingProperty() {
        Booking booking = booking(property(8L));
        when(propertyRepository.findById(8L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.create(booking));

        assertEquals("Property was not found", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createRejectsPropertyWithActiveBooking() {
        Property property = property(9L);
        Booking booking = booking(property);
        when(propertyRepository.findById(9L)).thenReturn(Optional.of(property));
        when(bookingRepository.existsByPropertyIdAndStatusIn(any(), anyList())).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> bookingService.create(booking));

        assertEquals("This property already has an active booking", exception.getMessage());
        assertEquals("Available", property.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateStatusCancelsBookingAndMakesPropertyAvailable() {
        Property property = property(10L);
        Booking booking = booking(property);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = bookingService.updateStatus(10L, "cancelled");

        assertSame(booking, result);
        assertEquals("cancelled", booking.getStatus());
        assertEquals("Available", property.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void updateStatusMarksActiveBookingAsBooked() {
        Property property = property(11L);
        Booking booking = booking(property);
        when(bookingRepository.findById(11L)).thenReturn(Optional.of(booking));

        bookingService.updateStatus(11L, "Confirmed");

        assertEquals("Confirmed", booking.getStatus());
        assertEquals("Booked", property.getStatus());
    }

    @Test
    void updateStatusRejectsMissingBooking() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateStatus(12L, "Booked"));

        assertEquals("Booking was not found", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    private static Property property(Long id) {
        Property property = new Property();
        try {
            var idField = Property.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(property, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
        return property;
    }

    private static Booking booking(Property property) {
        Booking booking = new Booking();
        booking.setProperty(property);
        return booking;
    }
}
