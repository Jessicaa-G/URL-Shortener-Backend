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
    private long clicksNAM = 0;
    private long clicksEMEA = 0;
    private long clicksAPAC = 0;

    public Url(String longUrl, String shortUrl, LocalDateTime createDate, LocalDateTime expireDate, long clicksNAM, long clicksEMEA, long clicksAPAC) {
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

    public long getClicksNAM() {
        return clicksNAM;
    }

    public void setClicksNAM(long clicksNAM) {
        this.clicksNAM = clicksNAM;
    }

    public long getClicksEMEA() {
        return clicksEMEA;
    }

    public void setClicksEMEA(long clicksEMEA) {
        this.clicksEMEA = clicksEMEA;
    }

    public long getClicksAPAC() {
        return clicksAPAC;
    }

    public void setClicksAPAC(long clicksAPAC) {
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
