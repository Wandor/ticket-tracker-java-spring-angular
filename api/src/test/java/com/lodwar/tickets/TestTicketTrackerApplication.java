package com.lodwar.tickets;

import org.springframework.boot.SpringApplication;

public class TestTicketTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.from(TicketTrackerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
