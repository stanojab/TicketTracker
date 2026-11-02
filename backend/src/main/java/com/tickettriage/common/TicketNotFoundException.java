package com.tickettriage.common;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(Long id) {
        super("Ticket " + id + " not found");
    }
}
