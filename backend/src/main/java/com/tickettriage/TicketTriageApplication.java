package com.tickettriage;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "TicketTriage API", version = "v1",
        description = "Create, triage and manage customer support tickets"))
public class TicketTriageApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketTriageApplication.class, args);
    }
}
