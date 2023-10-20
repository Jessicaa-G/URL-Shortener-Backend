package com.example.urlshortener.Controller;

import com.example.urlshortener.model.Url;
import com.example.urlshortener.model.UrlDto;
import com.example.urlshortener.model.UrlErrorResponse;
import com.example.urlshortener.model.UrlResponse;
import com.example.urlshortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UrlShorteningController {
    @Autowired
    private UrlService urlService;

    @PostMapping("shortenurl")
    public ResponseEntity<?> generateShortUrl(@RequestBody UrlDto urlDto) {
        Url shortenedUrl = urlService.generateShortUrl(urlDto);

        if (shortenedUrl != null) {
            UrlResponse res = new UrlResponse();
            res.setLongUrl(shortenedUrl.getLongUrl());
            res.setExpireDate(shortenedUrl.getExpireDate());
            res.setShortUrl(shortenedUrl.getShortUrl());
            return new ResponseEntity<UrlResponse>(res, HttpStatus.OK);
        }

        UrlErrorResponse error = new UrlErrorResponse("404", "Error occurred when shortening url");
        return new ResponseEntity<UrlErrorResponse>(error, HttpStatus.OK);

    }

    @DeleteMapping("shortenurl")
    public ResponseEntity<?> deleteUrl(@RequestParam String url) {
        urlService.deleteUrlById(url);
        return new ResponseEntity<UrlResponse>(HttpStatus.OK);
    }
}
