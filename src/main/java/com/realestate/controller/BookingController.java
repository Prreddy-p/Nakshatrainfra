package com.realestate.controller;

import com.realestate.model.Booking;
import com.realestate.repository.BookingRepository;
import com.realestate.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingRepository repository;
    private final BookingService service;
    public BookingController(BookingRepository repository, BookingService service) { this.repository = repository; this.service = service; }
    @GetMapping public List<Booking> list() { return repository.findAll(); }
    @GetMapping("/{id}") public ResponseEntity<Booking> get(@PathVariable Long id) { return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping public Booking create(@RequestBody Booking booking) { return service.create(booking); }
    @PatchMapping("/{id}/status") public Booking updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) { return service.updateStatus(id, payload.getOrDefault("status", "Booked")); }
}
