package org.awsp.springbootpostgredocker.controller;

import java.util.List;

import org.awsp.springbootpostgredocker.domain.Employee;
import org.awsp.springbootpostgredocker.domain.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employee")
public class EmployeeController {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<Employee> getAllEmployees() {
        logger.info("Get all the employees...");
        return employeeRepository.findAll();
    }
    
    @PostMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Employee createEmployee(@RequestBody Employee employee) {
        logger.info("Inserting employee...");
        return employeeRepository.save(employee);
    }

}
