package com.example.graphqlexample.order.domain;

public class InvalidOrderArgumentException extends RuntimeException {

    public InvalidOrderArgumentException(String message) {
        super(message);
    }
}
