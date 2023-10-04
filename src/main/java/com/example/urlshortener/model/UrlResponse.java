package com.example.urlshortener.model;

import java.time.LocalDateTime;

public class UrlResponse {
    private String longUrl;
    private String shortUrl;
    private LocalDateTime expireDate;

    public UrlResponse() {
    }

    public UrlResponse(String longUrl, String shortUrl, LocalDateTime expireDate) {
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
        this.expireDate = expireDate;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public LocalDateTime getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDateTime expireDate) {
        this.expireDate = expireDate;
    }

    @Override
    public String toString() {
        return "UrlResponse{" +
                "longUrl='" + longUrl + '\'' +
                ", shortUrl='" + shortUrl + '\'' +
                ", expireDate=" + expireDate +
                '}';
    }
}

