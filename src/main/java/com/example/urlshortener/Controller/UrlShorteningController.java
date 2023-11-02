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
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UrlShorteningController {
    @Autowired
    private UrlService urlService;
    private final String BASE_ADDR = "http://localhost:8080";

    @PostMapping("shortenurl")
    public ResponseEntity<?> generateShortUrl(@RequestBody UrlDto urlDto) {
        Url shortenedUrl = urlService.generateShortUrl(urlDto);

        if (shortenedUrl != null) {
            UrlResponse res = new UrlResponse();
            res.setLongUrl(shortenedUrl.getLongUrl());
            res.setExpireDate(shortenedUrl.getExpireDate());
            res.setShortUrl(BASE_ADDR + "/shortenurl/" + shortenedUrl.getShortUrl());
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

    @GetMapping("shortenurl/{urlParam}")
    public Object redirect(@PathVariable String urlParam) {
        String longUrl = urlService.redirect(urlParam);
        if (longUrl != null) {
            // Redirect to long url
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(longUrl);
            return redirectView;
        }

        UrlErrorResponse error = new UrlErrorResponse("404", "Error occurred when shortening url");
        return new ResponseEntity<UrlErrorResponse>(error, HttpStatus.OK);
    }

    @GetMapping("stats/{urlParam}")
    public ResponseEntity<?> getStats(@PathVariable String urlParam) {
        Map<String, Long> clicks = urlService.getStats(urlParam);
        if (clicks != null) {
            return ResponseEntity.ok(clicks);
        }

        UrlErrorResponse error = new UrlErrorResponse("404", "Error occurred when shortening url");
        return new ResponseEntity<UrlErrorResponse>(error, HttpStatus.OK);
    }
}
