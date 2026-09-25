package com.realestate.controller;

import com.realestate.model.UserAccount;
import com.realestate.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserAccountController {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    public UserAccountController(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping public List<UserAccount> list() { return repository.findAll(); }
    @PostMapping public UserAccount create(@RequestBody UserAccount user) {
        if (repository.existsByEmailIdIgnoreCase(user.getEmailId())) throw new IllegalStateException("A user with this email already exists");
        if (user.getPassword() == null || user.getPassword().length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
        validateRole(user.getRole());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return repository.save(user);
    }
    @PutMapping("/{id}") public ResponseEntity<UserAccount> update(@PathVariable Long id, @RequestBody UserAccount input) {
        return repository.findById(id).map(existing -> {
            validateRole(input.getRole());
            input.setId(id);
            if (input.getPassword() != null && !input.getPassword().isBlank()) {
                input.setPassword(passwordEncoder.encode(input.getPassword()));
            } else {
                input.setPassword(existing.getPassword());
            }
            return ResponseEntity.ok(repository.save(input));
        }).orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    private void validateRole(String role) {
        if (!"Manager".equals(role) && !"Associate".equals(role)) throw new IllegalArgumentException("Role must be Manager or Associate");
    }
}
