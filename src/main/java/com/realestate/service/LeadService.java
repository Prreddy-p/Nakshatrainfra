package com.realestate.service;

import com.realestate.model.Customer;
import com.realestate.model.Lead;
import com.realestate.repository.CustomerRepository;
import com.realestate.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class LeadService {
    private final LeadRepository leads;
    private final CustomerRepository customers;
    private final com.realestate.repository.PaymentRepository payments;
    public LeadService(LeadRepository leads, CustomerRepository customers, com.realestate.repository.PaymentRepository payments) {
        this.leads = leads;
        this.customers = customers;
        this.payments = payments;
    }

    @Transactional
    public Lead save(Long id, Lead input, boolean conversionConfirmed) {
        if (input.getCustomerName() == null || input.getCustomerName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        boolean converted = false;
        if (id != null) {
            Lead existing = leads.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            input.setCreatedDate(existing.getCreatedDate());
            input.setTotalAssetValue(existing.getTotalAssetValue());
            input.setPaymentRevision(existing.getPaymentRevision());
            converted = customers.existsByLeadId(id);
        }
        input.setId(id);
        if (id == null) {
            input.setTotalAssetValue(null);
            input.setPaymentRevision(0L);
        }
        boolean shouldConvert = input.getAdvancePaidEnabled() && !converted;
        if (shouldConvert && !conversionConfirmed) {
            throw new IllegalArgumentException("Confirm conversion to a customer before saving Advance Paid.");
        }
        Lead saved = leads.saveAndFlush(input);
        if (shouldConvert) customers.save(new Customer(saved));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Lead lead = leads.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (customers.existsByLeadId(id)) {
            throw new IllegalStateException("This lead is linked to a customer and cannot be deleted.");
        }
        if (payments.existsByLead_Id(id)) throw new IllegalStateException("This lead has saved payments and cannot be deleted.");
        leads.delete(lead);
    }
}
