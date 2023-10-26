package com.example.urlshortener.model;

import javax.persistence.Entity;
import javax.persistence.Lob;
import java.time.LocalDateTime;

@Entity
public class Url {

    @Lob
    private String longUrl;
    private String shortUrl;
    private LocalDateTime createDate;
    private LocalDateTime expireDate;
    private int clicksNAM = 0;
    private int clicksEMEA = 0;
    private int clicksAPAC = 0;

    public Url(String longUrl, String shortUrl, LocalDateTime createDate, LocalDateTime expireDate, int clicksNAM, int clicksEMEA, int clicksAPAC) {
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
        this.createDate = createDate;
        this.expireDate = expireDate;
        this.clicksNAM = clicksNAM;
        this.clicksEMEA = clicksEMEA;
        this.clicksAPAC = clicksAPAC;
    }

    public Url() {
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

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public LocalDateTime getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDateTime expireDate) {
        this.expireDate = expireDate;
    }

    public int getClicksNAM() {
        return clicksNAM;
    }

    public void setClicksNAM(int clicksNAM) {
        this.clicksNAM = clicksNAM;
    }

    public int getClicksEMEA() {
        return clicksEMEA;
    }

    public void setClicksEMEA(int clicksEMEA) {
        this.clicksEMEA = clicksEMEA;
    }

    public int getClicksAPAC() {
        return clicksAPAC;
    }

    public void setClicksAPAC(int clicksAPAC) {
        this.clicksAPAC = clicksAPAC;
    }

    @Override
    public String toString() {
        return "Url{" +
                "longUrl='" + longUrl + '\'' +
                ", shortUrl='" + shortUrl + '\'' +
                ", createDate=" + createDate +
                ", expireDate=" + expireDate +
                ", clicksNAM=" + clicksNAM +
                ", clicksEMEA=" + clicksEMEA +
                ", clicksAPAC=" + clicksAPAC +
                '}';
    }
}
