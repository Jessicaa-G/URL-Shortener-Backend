package com.example.urlshortener.Controller;

import com.example.urlshortener.model.SubscriptionDto;
import com.example.urlshortener.model.UrlErrorResponse;
import com.example.urlshortener.model.User;
import com.example.urlshortener.service.JwtService;
import com.example.urlshortener.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.common.hash.Hashing;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;

@RestController
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    @PostMapping("user")
    public ResponseEntity<?> addUser(@RequestBody User user) {
        User res = userService.createUser(user);

        if (res != null) {
            return new ResponseEntity<>(res, HttpStatus.OK);
        }

        UrlErrorResponse error = new UrlErrorResponse("404", "Error occurred when creating user");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @PutMapping("subscribe")
    public ResponseEntity<?> updateSubscription(@RequestBody SubscriptionDto dto) {
        User user = userService.updateSubscription(dto.getUserId(), dto.getTier());

        if (user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        }

        UrlErrorResponse error = new UrlErrorResponse("404", "User not found");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @PostMapping("login")
    public ResponseEntity<?> loginUser(@RequestBody User user, HttpServletRequest request,
            HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName())) {
                    try {
                        jwtService.verifyToken(cookie.getValue());
                        return new ResponseEntity<>("User already logged in", HttpStatus.OK);
                    } catch (Exception e) {
                        // Token is invalid or expired, proceed with login logic
                    }
                }
            }
        }
        User storedUser = userService.getUserByEmail(user.getEmail());
        if (storedUser != null && storedUser.getPassword()
                .equals(Hashing.sha256().hashString(user.getPassword(), StandardCharsets.UTF_8).toString())) {
            String token = jwtService.generateToken(user);
            Cookie authCookie = new Cookie("auth_token", token);
            authCookie.setHttpOnly(true);
            authCookie.setPath("/");
            response.addCookie(authCookie);
            return new ResponseEntity<>("Login successful", HttpStatus.OK);
        }
        UrlErrorResponse error = new UrlErrorResponse("401", "Invalid email or password");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @PutMapping("logout")
    public ResponseEntity<?> logoutUser(HttpServletResponse response) {
        Cookie authCookie = new Cookie("auth_token", null);
        authCookie.setMaxAge(0);
        authCookie.setHttpOnly(true);
        authCookie.setPath("/");
        response.addCookie(authCookie);
        return new ResponseEntity<>("Logged out successfully", HttpStatus.OK);
    }
}
