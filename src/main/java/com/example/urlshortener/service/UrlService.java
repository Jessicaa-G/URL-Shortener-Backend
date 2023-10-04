package com.example.urlshortener.service;

import com.example.urlshortener.model.Url;
import com.example.urlshortener.model.UrlDto;
import com.google.common.hash.Hashing;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class UrlService {

    public Url generateShortUrl(UrlDto url) {
        if (StringUtils.isNotEmpty(url.getUrl())) {
            String shortenedStr = shortenUrl(url.getUrl());
            Url shortenedUrl = new Url();
            shortenedUrl.setCreateDate(LocalDateTime.now());
            shortenedUrl.setLongUrl(url.getUrl());
            shortenedUrl.setShortUrl(shortenedStr);
            shortenedUrl.setExpireDate(getExpireDate(url.getExpireDate(), shortenedUrl.getCreateDate()));
            return shortenedUrl;
        }
        return null;
    }

    private String shortenUrl(String url) {
        String shortenedUrl = "";
        LocalDateTime time = LocalDateTime.now();
        shortenedUrl = Hashing.murmur3_32()
                .hashString(url.concat(time.toString()), StandardCharsets.UTF_8)
                .toString();
        return shortenedUrl;
    }

    private LocalDateTime getExpireDate(String expireDate, LocalDateTime createDate) {
        if (StringUtils.isBlank(expireDate)) {
            // TODO: set expiration date according to user tier level
            return createDate.plusDays(30);
        }
        return LocalDateTime.parse(expireDate);
    }

}
