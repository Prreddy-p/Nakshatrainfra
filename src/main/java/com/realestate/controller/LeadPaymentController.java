package com.realestate.controller;

import com.realestate.service.LeadPaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leads/{id}/payments")
public class LeadPaymentController {
    private final LeadPaymentService service;
    public LeadPaymentController(LeadPaymentService service) { this.service = service; }
    @GetMapping public LeadPaymentService.Details get(@PathVariable Long id) { return service.get(id); }
    @PutMapping public LeadPaymentService.Details save(@PathVariable Long id, @RequestBody LeadPaymentService.Request request) { return service.save(id, request); }
}
