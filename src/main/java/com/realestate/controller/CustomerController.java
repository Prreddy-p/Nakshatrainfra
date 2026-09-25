package com.realestate.controller;

import com.realestate.model.Customer;
import com.realestate.repository.CustomerRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerRepository customers;
    public CustomerController(CustomerRepository customers) { this.customers = customers; }
    @GetMapping public List<Customer> list() { return customers.findAll(); }
}
