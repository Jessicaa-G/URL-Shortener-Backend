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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class UrlShorteningController {
    @Autowired
    private UrlService urlService;

    @Autowired
    UserService userService;
    private final String BASE_ADDR = "http://localhost:8080";

    @PostMapping("shortenurl")
    public ResponseEntity<?> generateShortUrl(@RequestBody UrlDto urlDto, HttpServletRequest request) {
        Url shortenedUrl = null;
        String userId = userService.userLoggedIn(request.getCookies());
        if(userId!=""){
            shortenedUrl = urlService.generateShortUrl(urlDto, userId);
        }else{
            shortenedUrl = urlService.generateShortUrl(urlDto);
        }

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
            String longUrl = urlService.redirect(urlParam);
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
}
