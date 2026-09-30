package com.realestate.controller;

import static com.realestate.security.RecordAccess.*;

import com.realestate.model.DocumentRecord;
import com.realestate.model.Payment;
import com.realestate.model.Task;
import com.realestate.repository.DocumentRepository;
import com.realestate.repository.PaymentRepository;
import com.realestate.repository.TaskRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class OperationsController {
    private final PaymentRepository payments;
    private final TaskRepository tasks;
    private final DocumentRepository documents;
    public OperationsController(PaymentRepository payments, TaskRepository tasks, DocumentRepository documents) { this.payments = payments; this.tasks = tasks; this.documents = documents; }
    @GetMapping("/payments") public List<Payment> payments() { return visible(payments.findAll()); }
    @PostMapping("/payments") public Payment createPayment(@RequestBody Payment payment) { if (payment.getId() != null) throw new IllegalArgumentException("New payment must not have an ID"); return payments.save(payment); }
    @GetMapping("/tasks") public List<Task> tasks(@RequestParam(required = false) String status) { return visible(status == null ? tasks.findAll() : tasks.findByStatusIgnoreCase(status)); }
    @PostMapping("/tasks") public Task createTask(@RequestBody Task task) {
        if (task.getTaskName() == null || task.getTaskName().isBlank()) throw new IllegalArgumentException("Task name is required");
        if (task.getId() != null) throw new IllegalArgumentException("New task must not have an ID");
        return tasks.save(task);
    }
    public record TaskUpdate(String taskName, String status) {}
    @PatchMapping("/tasks/{id}")
    public org.springframework.http.ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody TaskUpdate input) {
        return tasks.findById(id).map(task -> {
            require(task);
            if (input.taskName() != null) {
                if (input.taskName().isBlank()) throw new IllegalArgumentException("Task name is required");
                task.setTaskName(input.taskName());
            }
            if (input.status() != null) {
                if (!java.util.Set.of("Pending", "Completed").contains(input.status())) throw new IllegalArgumentException("Status must be Pending or Completed");
                task.setStatus(input.status());
            }
            return org.springframework.http.ResponseEntity.ok(tasks.save(task));
        }).orElse(org.springframework.http.ResponseEntity.notFound().build());
    }
    @GetMapping("/documents") public List<DocumentRecord> documents() { return visible(documents.findAll()); }
    @PostMapping("/documents") public DocumentRecord createDocument(@RequestBody DocumentRecord document) { if (document.getId() != null) throw new IllegalArgumentException("New document must not have an ID"); return documents.save(document); }
}
