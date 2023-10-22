package com.example.urlshortener.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;
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

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public void setTier(String tier) {
        this.tier = tier;
    }

    public void setTierExpire(LocalDateTime tierExpire) {
        this.tierExpire = tierExpire;
    }

    public void setArea(String area) {
        this.area = area;
    }
}
