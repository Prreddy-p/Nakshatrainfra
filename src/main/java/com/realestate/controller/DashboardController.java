package com.realestate.controller;

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
        result.put("totalProperties", properties.count());
        result.put("availableProperties", properties.findByStatusIgnoreCase("Available").size());
        result.put("soldProperties", properties.findByStatusIgnoreCase("Sold").size());
        result.put("totalLeads", leads.count());
        result.put("hotLeads", leads.findByCategoryIgnoreCase("Hot").size());
        result.put("bookings", bookings.count());
        result.put("tasksPending", tasks.findByStatusIgnoreCase("Pending").size());
        result.put("paymentsRecorded", payments.count());
        result.put("currency", "INR");
        result.put("totalSales", BigDecimal.ZERO);
        result.put("amountReceived", BigDecimal.ZERO);
        result.put("outstandingAmount", BigDecimal.ZERO);
        return result;
    }
}
