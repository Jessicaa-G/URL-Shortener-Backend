package com.example.urlshortener.model.exception;

public class ConflictException extends RuntimeException{

    private String message;

    public ConflictException(String message){
        super(message);
    }
}
