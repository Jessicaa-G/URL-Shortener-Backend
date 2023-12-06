package com.example.urlshortener.Controller;

import com.example.urlshortener.model.*;
import com.example.urlshortener.model.exception.BadRequestException;
import com.example.urlshortener.model.exception.NotFoundException;
import com.example.urlshortener.service.UrlService;
import com.example.urlshortener.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class UrlShorteningController {
    @Autowired
    private UrlService urlService;

    @Autowired
    UserService userService;

    @PostMapping("shortenurl")
    public ResponseEntity<?> generateShortUrl(@RequestBody UrlDto urlDto, HttpServletRequest request) {
        Url shortenedUrl = null;
        String userId = userService.userLoggedIn(request.getCookies());
        if(userId!=""){
            shortenedUrl = urlService.generateShortUrl(urlDto, userId);
        }else{
            shortenedUrl = urlService.generateShortUrl(urlDto);
        }

        String BASE_ADDR = request.getRequestURL().toString().replace(request.getRequestURI(),"");

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
    public ResponseEntity<?> deleteUrl(@RequestParam String url, HttpServletRequest request) {
        String userId = userService.userLoggedIn(request.getCookies());
        try {
            if (userId == "") {
                throw new BadRequestException("Access error, you need to login first");
            }
            urlService.deleteUrlById(url, userId);
            return ResponseEntity.ok("Url successfully deleted");
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (BadRequestException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("shortenurl/{urlParam}")
    public Object redirect(@PathVariable String urlParam) {
        try {
            String longUrl = urlService.resolve(urlParam, true);
            if (longUrl != null) {
                // Redirect to long url
                RedirectView redirectView = new RedirectView();
                redirectView.setUrl(longUrl);
                return redirectView;
            }

            UrlErrorResponse error = new UrlErrorResponse("404", "Error occurred when shortening url");
            return new ResponseEntity<UrlErrorResponse>(error, HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("stats/{urlParam}")
    public ResponseEntity<?> getStats(@PathVariable String urlParam) {
        try {
            Map<String, Long> clicks = urlService.getStats(urlParam);
            return ResponseEntity.ok(clicks);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (BadRequestException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("shortenurls")
    public ResponseEntity<?> getUrlsByUser(HttpServletRequest request) {
        try {
            String userId = userService.userLoggedIn(request.getCookies());
            if (userId == "") {
                throw new BadRequestException("Access error, you need to login first");
            }
            List<Url> urls = urlService.getUrlsByUser(userId);
            return ResponseEntity.ok(urls);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (BadRequestException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("bulk/shortenurl")
    public ResponseEntity<?> generateBulkShortUrls(@RequestBody List<UrlDto> urls, HttpServletRequest request) {
        String userId = userService.userLoggedIn(request.getCookies());

        List<UrlResponse> urlResponses = new ArrayList<>();

        String BASE_ADDR = request.getRequestURL().toString().replace(request.getRequestURI(),"");

        for (UrlDto url : urls) {
            Url shortenedUrl = null;

            if (userId != "") {
                shortenedUrl = urlService.generateShortUrl(url, userId);
            } else {
                shortenedUrl = urlService.generateShortUrl(url);
            }

            if (shortenedUrl != null) {
                UrlResponse res = new UrlResponse();
                res.setLongUrl(shortenedUrl.getLongUrl());
                res.setExpireDate(shortenedUrl.getExpireDate());
                res.setShortUrl(BASE_ADDR + "/shortenurl/" + shortenedUrl.getShortUrl());
                urlResponses.add(res);
            } else {
                // Handle the case where shortening fails for a URL
                UrlErrorResponse error = new UrlErrorResponse("404", "Error occurred when shortening url");
                return new ResponseEntity<UrlErrorResponse>(error, HttpStatus.OK);
            }
        }

        return new ResponseEntity<List<UrlResponse>>(urlResponses, HttpStatus.OK);
    }

    @PostMapping("bulk/resolveurl")
    public ResponseEntity<?> resolveBulkUrls(@RequestBody List<String> shortUrls) {
        List<String> originalUrls = new ArrayList<>();

        for (String shortUrl : shortUrls) {
            try {
                String originalUrl = urlService.resolve(shortUrl, false);
                originalUrls.add(originalUrl);
            } catch (NotFoundException e) {
                // Handle the case where expansion fails for a URL
                UrlErrorResponse error = new UrlErrorResponse("404", "Error occurred when expanding url");
                return new ResponseEntity<UrlErrorResponse>(error, HttpStatus.OK);
            }
        }

        return new ResponseEntity<List<String>>(originalUrls, HttpStatus.OK);
    }

    @DeleteMapping("bulk/url")
    public ResponseEntity<?> deleteBulkUrls(@RequestBody List<String> shortUrls, HttpServletRequest request) {
        try {
            String userId = userService.userLoggedIn(request.getCookies());
            if (userId == "") {
                throw new BadRequestException("Access error, you need to login first");
            }

            for (String shortUrl : shortUrls) {
                try {
                    urlService.deleteUrlById(shortUrl, userId);
                } catch(NotFoundException e) {

                }
            }

            List<String> updatedUrls = userService.getUserById(userId).getUrls();

            return new ResponseEntity<>(updatedUrls, HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (BadRequestException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
