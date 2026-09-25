package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.model.Lead;
import com.realestate.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:lead-payment-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class LeadPaymentFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LeadRepository leads;
    Long id;
    @BeforeEach void setup() {
        Lead lead = new Lead(); lead.setCustomerName("Payment test"); id = leads.save(lead).getId();
    }
    Map<String, Object> installment(String amount) {
        return Map.of("amount", amount, "paymentDate", "2026-09-25", "notes", "Test reference");
    }
    org.springframework.test.web.servlet.ResultActions save(String asset, String advance, List<?> installments, long revision) throws Exception {
        return mvc.perform(put("/api/leads/" + id + "/payments").contentType("application/json")
            .content(json.writeValueAsString(Map.of("totalAssetValue", asset, "advanceAmount", advance, "installments", installments, "revision", revision))));
    }
    @Test void examplePersistsAndSupportsEditDeleteAndFullPayment() throws Exception {
        save("1000000", "200000", List.of(installment("100000"), installment("50000")), 0)
            .andExpect(status().isOk()).andExpect(jsonPath("totalPaid").value(350000))
            .andExpect(jsonPath("remainingAmount").value(650000));
        mvc.perform(get("/api/leads/" + id + "/payments")).andExpect(status().isOk())
            .andExpect(jsonPath("installments.length()").value(2)).andExpect(jsonPath("advanceAmount").value(200000));
        save("1000000", "200000", List.of(installment("150000")), 1).andExpect(status().isOk())
            .andExpect(jsonPath("installments.length()").value(1)).andExpect(jsonPath("totalInstallments").value(150000));
        save("1000000", "200000", List.of(installment("800000")), 2).andExpect(status().isOk())
            .andExpect(jsonPath("status").value("Fully Paid")).andExpect(jsonPath("remainingAmount").value(0));
    }
    @Test void rejectsInvalidAmountsWithoutChangingSavedPayments() throws Exception {
        save("1000", "100", List.of(), 0).andExpect(status().isOk());
        save("1000", "1001", List.of(), 1).andExpect(status().isBadRequest());
        save("1000", "100", List.of(installment("901")), 1).andExpect(status().isBadRequest());
        save("1000", "-1", List.of(), 1).andExpect(status().isBadRequest());
        save("1000", "100", List.of(installment("-1")), 1).andExpect(status().isBadRequest());
        save("1000", "100", List.of(installment("0")), 1).andExpect(status().isBadRequest());
        save("1000", "0.001", List.of(), 1).andExpect(status().isBadRequest());
        save("1000", "abc", List.of(), 1).andExpect(status().isBadRequest());
        save("0", "0", List.of(), 1).andExpect(status().isBadRequest());
        save("1000", "0", List.of(Map.of("amount", "10")), 1).andExpect(status().isBadRequest());
        mvc.perform(get("/api/leads/" + id + "/payments")).andExpect(jsonPath("totalPaid").value(100))
            .andExpect(jsonPath("remainingAmount").value(900)).andExpect(jsonPath("revision").value(1));
    }
    @Test void staleUpdatesRejectedAndLeadEditsPreserveBalances() throws Exception {
        save("0.30", "0.10", List.of(installment("0.20")), 0).andExpect(status().isOk())
            .andExpect(jsonPath("remainingAmount").value(0));
        save("1000", "0", List.of(), 0).andExpect(status().isConflict());
        mvc.perform(put("/api/leads/" + id).contentType("application/json")
            .content("{\"customerName\":\"Updated name\",\"totalAssetValue\":0,\"paymentRevision\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("totalAssetValue").value(0.3));
        mvc.perform(get("/api/leads/" + id + "/payments")).andExpect(jsonPath("totalPaid").value(0.3))
            .andExpect(jsonPath("revision").value(1));
        mvc.perform(delete("/api/leads/" + id)).andExpect(status().isConflict());
    }
}
