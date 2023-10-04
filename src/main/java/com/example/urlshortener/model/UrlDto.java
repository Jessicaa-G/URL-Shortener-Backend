package com.example.urlshortener.model;

public class UrlDto {
    private String url;
    private String expireDate;

    public UrlDto(String url, String expireDate) {
        this.url = url;
        this.expireDate = expireDate;
    }

    public UrlDto() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    @Override
    public String toString() {
        return "UrlDto{" +
                "url='" + url + '\'' +
                ", expireDate='" + expireDate + '\'' +
                '}';
    }
}
