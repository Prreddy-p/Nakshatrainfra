package com.realestate.controller;

import static com.realestate.security.RecordAccess.*;

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
        if (status != null && !status.isBlank()) return visible(repository.findByStatusIgnoreCase(status));
        if (search != null && !search.isBlank()) return visible(repository.findByPropertyNameContainingIgnoreCaseOrCityContainingIgnoreCase(search, search));
        return visible(repository.findAll());
    }
    @GetMapping("/{id}") public ResponseEntity<Property> get(@PathVariable Long id) { return repository.findById(id).map(record -> ResponseEntity.ok(require(record))).orElse(ResponseEntity.notFound().build()); }
    @PostMapping public Property create(@RequestBody Property property) {
        validate(property);
        property.setId(null);
        if (property.getPropertyId() == null || property.getPropertyId().isBlank()) {
            property.setPropertyId("PROP-" + java.util.UUID.randomUUID());
        }
        return repository.save(property);
    }
    @PutMapping("/{id}") public ResponseEntity<Property> update(@PathVariable Long id, @RequestBody Property input) {
        validate(input);
        return repository.findById(id).map(existing -> {
            require(existing);
            input.preserveOwner(existing);
            input.setId(existing.getId());
            input.setPropertyId(existing.getPropertyId());
            return ResponseEntity.ok(repository.save(input));
        }).orElse(ResponseEntity.notFound().build());
    }
    private void validate(Property property) {
        if (property.getPropertyName() == null || property.getPropertyName().isBlank()) {
            throw new IllegalArgumentException("Property name is required");
        }
        if (property.getPrice() != null && property.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (property.getStatus() == null || property.getStatus().isBlank()) {
            throw new IllegalArgumentException("Property status is required");
        }
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { if (!repository.existsById(id)) return ResponseEntity.notFound().build(); require(repository.findById(id).orElseThrow()); repository.deleteById(id); return ResponseEntity.noContent().build(); }
}
