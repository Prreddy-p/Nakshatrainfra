package com.realestate.controller;

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
    @GetMapping("/payments") public List<Payment> payments() { return payments.findAll(); }
    @PostMapping("/payments") public Payment createPayment(@RequestBody Payment payment) { return payments.save(payment); }
    @GetMapping("/tasks") public List<Task> tasks(@RequestParam(required = false) String status) { return status == null ? tasks.findAll() : tasks.findByStatusIgnoreCase(status); }
    @PostMapping("/tasks") public Task createTask(@RequestBody Task task) { return tasks.save(task); }
    @PatchMapping("/tasks/{id}") public Task updateTask(@PathVariable Long id, @RequestBody Task task) { task.setStatus(task.getStatus()); task.setTaskName(task.getTaskName()); return tasks.save(task); }
    @GetMapping("/documents") public List<DocumentRecord> documents() { return documents.findAll(); }
    @PostMapping("/documents") public DocumentRecord createDocument(@RequestBody DocumentRecord document) { return documents.save(document); }
}
