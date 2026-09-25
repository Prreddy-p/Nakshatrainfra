package com.realestate.config;

import com.realestate.model.Lead;
import com.realestate.model.Property;
import com.realestate.model.UserAccount;
import com.realestate.repository.LeadRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DemoDataConfig {
    @Bean
        CommandLineRunner seedDemoData(
            PropertyRepository properties,
            LeadRepository leads,
            UserAccountRepository users,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (properties.count() == 0) {
                Property first = new Property();
                first.setPropertyId("PROP-001");
                first.setPropertyName("18 Willow Lane");
                first.setPropertyType("Villa");
                first.setCity("Brookline");
                first.setState("MA");
                first.setPrice(new BigDecimal("1240000"));
                first.setStatus("Available");
                properties.save(first);

                Property second = new Property();
                second.setPropertyId("PROP-002");
                second.setPropertyName("38 Eastwood Ave");
                second.setPropertyType("Independent House");
                second.setCity("Newton");
                second.setState("MA");
                second.setPrice(new BigDecimal("895000"));
                second.setStatus("Pending");
                properties.save(second);
            }
            if (leads.count() == 0) {
                Lead lead = new Lead();
                lead.setCustomerName("James Wilson");
                lead.setMobileNumber("+1 617 555 0182");
                lead.setInterestedProperty("18 Willow Lane");
                lead.setPreferredLocation("Brookline");
                lead.setLeadSource("Website");
                lead.setLeadStatus("Follow-up");
                lead.setCategory("Hot");
                leads.save(lead);
            }
            if (users.count() == 0) {
                UserAccount user = new UserAccount();
                user.setName("Demo User");
                user.setEmailId("user@example.com");
                user.setPassword(passwordEncoder.encode("DemoPass@123"));
                user.setRole("Manager");
                users.save(user);
            }
        };
    }
}
