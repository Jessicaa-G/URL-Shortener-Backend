package com.example.urlshortener.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
public class User {

    @Id
    @Getter
    @Setter
    private UUID id;

    @Getter
    @Setter
    @Column(nullable = false)
    private String email;

    @Getter
    @Setter
    @Column(nullable = false)
    private String password;

    @Getter
    @Setter
    private List<String> urls;

    @Getter
    @Setter
    private String tier;

    @Getter
    @Setter
    private LocalDateTime tierExpire;

    @Getter
    @Setter
    private String area;

    public User() {
        this.id = UUID.randomUUID();
    }

    public User(UUID id) {
        this.id = id;
    }
}
