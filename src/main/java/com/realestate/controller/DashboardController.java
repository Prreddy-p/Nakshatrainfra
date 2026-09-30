package com.realestate.controller;

import static com.realestate.security.RecordAccess.*;

import com.realestate.repository.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final PropertyRepository properties;
    private final LeadRepository leads;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final TaskRepository tasks;
    public DashboardController(PropertyRepository properties, LeadRepository leads, BookingRepository bookings, PaymentRepository payments, TaskRepository tasks) { this.properties = properties; this.leads = leads; this.bookings = bookings; this.payments = payments; this.tasks = tasks; }
    @GetMapping public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalProperties", visible(properties.findAll()).size());
        result.put("availableProperties", visible(properties.findByStatusIgnoreCase("Available")).size());
        result.put("soldProperties", visible(properties.findByStatusIgnoreCase("Sold")).size());
        result.put("totalLeads", visible(leads.findAll()).size());
        result.put("hotLeads", visible(leads.findByCategoryIgnoreCase("Hot")).size());
        result.put("bookings", visible(bookings.findAll()).size());
        result.put("tasksPending", visible(tasks.findByStatusIgnoreCase("Pending")).size());
        result.put("paymentsRecorded", visible(payments.findAll()).size());
        result.put("currency", "INR");
        result.put("totalSales", BigDecimal.ZERO);
        result.put("amountReceived", BigDecimal.ZERO);
        result.put("outstandingAmount", BigDecimal.ZERO);
        return result;
    }
}
