package com.realestate.controller;

import com.realestate.model.Property;
import com.realestate.repository.PropertyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {
    private final PropertyRepository repository;
    public PropertyController(PropertyRepository repository) { this.repository = repository; }

    @GetMapping public List<Property> list(@RequestParam(required = false) String status, @RequestParam(required = false) String search) {
        if (status != null && !status.isBlank()) return repository.findByStatusIgnoreCase(status);
        if (search != null && !search.isBlank()) return repository.findByPropertyNameContainingIgnoreCaseOrCityContainingIgnoreCase(search, search);
        return repository.findAll();
    }
    @GetMapping("/{id}") public ResponseEntity<Property> get(@PathVariable Long id) { return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping public Property create(@RequestBody Property property) { return repository.save(property); }
    @PutMapping("/{id}") public ResponseEntity<Property> update(@PathVariable Long id, @RequestBody Property input) { return repository.findById(id).map(existing -> { input.setPropertyId(existing.getPropertyId()); return ResponseEntity.ok(repository.save(input)); }).orElse(ResponseEntity.notFound().build()); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { if (!repository.existsById(id)) return ResponseEntity.notFound().build(); repository.deleteById(id); return ResponseEntity.noContent().build(); }
}
