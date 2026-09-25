package com.realestate.controller;

import com.realestate.model.Lead;
import com.realestate.repository.LeadRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadController {
    private final LeadRepository repository;
    private final com.realestate.service.LeadService service;
    public LeadController(LeadRepository repository, com.realestate.service.LeadService service) { this.repository = repository; this.service = service; }
    @GetMapping public List<Lead> list(@RequestParam(required = false) String status, @RequestParam(required = false) String category) {
        if (status != null && !status.isBlank()) return repository.findByLeadStatusIgnoreCase(status);
        if (category != null && !category.isBlank()) return repository.findByCategoryIgnoreCase(category);
        return repository.findAll();
    }
    @GetMapping("/{id}") public ResponseEntity<Lead> get(@PathVariable Long id) { return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping public Lead create(@RequestBody Lead lead, @RequestParam(defaultValue = "false") boolean conversionConfirmed) { return service.save(null, lead, conversionConfirmed); }
    @PutMapping("/{id}") public ResponseEntity<Lead> update(@PathVariable Long id, @RequestBody Lead input, @RequestParam(defaultValue = "false") boolean conversionConfirmed) { return ResponseEntity.ok(service.save(id, input, conversionConfirmed)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
