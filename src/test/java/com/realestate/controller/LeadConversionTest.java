package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.model.Lead;
import com.realestate.repository.CustomerRepository;
import com.realestate.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:conversion-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class LeadConversionTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CustomerRepository customers;
    @Autowired LeadRepository leads;

    Lead newLead() {
        Lead lead = new Lead();
        lead.setCustomerName("Conversion test");
        lead.setEmail("customer@example.com");
        lead.setMobileNumber("1234567890");
        lead.setInterestedProperty("Test property");
        return leads.save(lead);
    }

    @Test void confirmedConversionPersistsAndDoesNotDuplicate() throws Exception {
        Lead lead = newLead();
        lead.setAdvancePaidEnabled(true);
        mvc.perform(put("/api/leads/" + lead.getId() + "?conversionConfirmed=true")
                .contentType("application/json").content(json.writeValueAsString(lead)))
                .andExpect(status().isOk()).andExpect(jsonPath("advancePaidEnabled").value(true));
        long count = customers.count();
        assertTrue(customers.existsByLeadId(lead.getId()));
        mvc.perform(get("/api/leads/" + lead.getId())).andExpect(jsonPath("advancePaidEnabled").value(true));
        mvc.perform(get("/api/customers")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.leadId == " + lead.getId() + ")].name").value("Conversion test"));
        mvc.perform(put("/api/leads/" + lead.getId()).contentType("application/json")
                .content(json.writeValueAsString(lead))).andExpect(status().isOk());
        lead.setAdvancePaidEnabled(false);
        mvc.perform(put("/api/leads/" + lead.getId()).contentType("application/json")
                .content(json.writeValueAsString(lead))).andExpect(status().isOk());
        assertEquals(count, customers.count());
        assertFalse(leads.findById(lead.getId()).orElseThrow().getAdvancePaidEnabled());
        lead.setAdvancePaidEnabled(true);
        mvc.perform(put("/api/leads/" + lead.getId() + "?conversionConfirmed=true")
                .contentType("application/json").content(json.writeValueAsString(lead))).andExpect(status().isOk());
        assertEquals(count, customers.count());
        mvc.perform(delete("/api/leads/" + lead.getId())).andExpect(status().isConflict());
    }

    @Test void missingConfirmationDoesNotSaveOrConvert() throws Exception {
        Lead lead = newLead();
        lead.setAdvancePaidEnabled(true);
        mvc.perform(put("/api/leads/" + lead.getId()).contentType("application/json")
                .content(json.writeValueAsString(lead))).andExpect(status().isBadRequest());
        assertFalse(customers.existsByLeadId(lead.getId()));
        assertFalse(leads.findById(lead.getId()).orElseThrow().getAdvancePaidEnabled());
    }

    @Test void newLeadCanBeCreatedAndConvertedTogether() throws Exception {
        Lead lead = new Lead();
        lead.setCustomerName("New customer");
        lead.setAdvancePaidEnabled(true);
        String body = mvc.perform(post("/api/leads?conversionConfirmed=true").contentType("application/json")
                .content(json.writeValueAsString(lead))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(customers.existsByLeadId(json.readTree(body).get("id").asLong()));
    }
}
