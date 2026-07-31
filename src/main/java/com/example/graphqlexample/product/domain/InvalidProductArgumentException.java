package com.example.graphqlexample.product.domain;

public class InvalidProductArgumentException extends RuntimeException {

    public InvalidProductArgumentException(String message) {
        super(message);
    }
}
