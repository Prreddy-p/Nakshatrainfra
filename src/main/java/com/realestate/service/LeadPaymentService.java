package com.realestate.service;

import com.realestate.model.*;
import com.realestate.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Service
public class LeadPaymentService {
    public record Installment(BigDecimal amount, LocalDate paymentDate, String notes) {}
    public record Request(BigDecimal totalAssetValue, BigDecimal advanceAmount, List<Installment> installments, Long revision) {}
    public record Details(BigDecimal totalAssetValue, BigDecimal advanceAmount, List<Installment> installments,
                          BigDecimal totalInstallments, BigDecimal totalPaid, BigDecimal remainingAmount, String status, Long revision) {}
    private final LeadRepository leads;
    private final PaymentRepository payments;
    public LeadPaymentService(LeadRepository leads, PaymentRepository payments) { this.leads = leads; this.payments = payments; }

    private Lead lock(Long id) {
        return leads.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
    }

    @Transactional
    public Details get(Long id) { return details(lock(id)); }

    @Transactional
    public Details save(Long id, Request request) {
        Lead lead = lock(id);
        if (request.revision() == null || !request.revision().equals(lead.getPaymentRevision())) {
            throw new IllegalStateException("Payments changed in another window. Reopen the record before saving.");
        }
        BigDecimal asset = money(request.totalAssetValue(), "Total Asset Value", true);
        BigDecimal advance = money(request.advanceAmount(), "Advance Amount", false);
        if (request.installments() == null || request.installments().size() > 500) {
            throw new IllegalArgumentException("Provide an installment list with no more than 500 payments.");
        }
        BigDecimal total = advance;
        for (Installment item : request.installments()) {
            if (item == null) throw new IllegalArgumentException("Invalid installment");
            total = total.add(money(item.amount(), "Installment amount", true));
            if (item.paymentDate() == null) throw new IllegalArgumentException("Each installment requires a payment date.");
            if (item.notes() != null && item.notes().length() > 1000) throw new IllegalArgumentException("Notes must be at most 1000 characters.");
        }
        if (total.compareTo(asset) > 0) throw new IllegalArgumentException("Advance and installments cannot exceed Total Asset Value.");
        payments.deleteAll(payments.findByLead_IdOrderByIdAsc(id));
        if (advance.signum() > 0) add(lead, "Advance", advance, null, null);
        for (Installment item : request.installments()) add(lead, "Installment", item.amount(), item.paymentDate(), item.notes());
        lead.setTotalAssetValue(asset);
        lead.setPaymentRevision(lead.getPaymentRevision() + 1);
        leads.saveAndFlush(lead);
        return details(lead);
    }

    private void add(Lead lead, String type, BigDecimal amount, LocalDate date, String notes) {
        Payment payment = new Payment();
        payment.setLead(lead); payment.setCustomer(lead.getCustomerName()); payment.setProperty(lead.getInterestedProperty());
        payment.setPaymentType(type); payment.setAmount(amount); payment.setPaidDate(date); payment.setNotes(notes); payment.setStatus("Paid");
        payments.save(payment);
    }

    private Details details(Lead lead) {
        BigDecimal advance = BigDecimal.ZERO;
        BigDecimal installmentTotal = BigDecimal.ZERO;
        List<Installment> installments = new ArrayList<>();
        for (Payment payment : payments.findByLead_IdOrderByIdAsc(lead.getId())) {
            if ("Advance".equals(payment.getPaymentType())) advance = advance.add(payment.getAmount());
            else {
                installmentTotal = installmentTotal.add(payment.getAmount());
                installments.add(new Installment(payment.getAmount(), payment.getPaidDate(), payment.getNotes()));
            }
        }
        BigDecimal paid = advance.add(installmentTotal);
        BigDecimal remaining = lead.getTotalAssetValue() == null ? null : lead.getTotalAssetValue().subtract(paid);
        String status = remaining == null ? "Asset value required" : remaining.signum() == 0 ? "Fully Paid" : "Balance Due";
        return new Details(lead.getTotalAssetValue(), advance, installments, installmentTotal, paid, remaining, status, lead.getPaymentRevision());
    }

    private BigDecimal money(BigDecimal value, String label, boolean positive) {
        if (value == null || value.signum() < 0 || (positive && value.signum() == 0) || value.compareTo(new BigDecimal("9999999999999.99")) > 0) {
            throw new IllegalArgumentException(label + (positive ? " must be greater than zero." : " must be zero or greater."));
        }
        try { return value.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException(label + " must have at most two decimal places."); }
    }
}
