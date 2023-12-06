package com.example.urlshortener.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Entity
public class SubscriptionDto {

    @Getter
    @Setter
    private String tier;

    public SubscriptionDto() {}

    public SubscriptionDto(String tier) {
        this.tier = tier;
    }
}
