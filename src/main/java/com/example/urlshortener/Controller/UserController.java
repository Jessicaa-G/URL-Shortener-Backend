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
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        User storedUser = userService.getUserByEmail(user.getEmail()); // This is a hypothetical method
        System.out.println(storedUser);
        if (storedUser != null && storedUser.getPassword()
                .equals(Hashing.sha256().hashString(user.getPassword(), StandardCharsets.UTF_8).toString())) {
            String token = jwtService.generateToken(user);
            return new ResponseEntity<>(token, HttpStatus.OK);
        }
        UrlErrorResponse error = new UrlErrorResponse("401", "Invalid email or password");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // V1 if we want to invalidate token
    // @PutMapping("logout")
    // public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String
    // token) {
    // userService.invalidateToken(token);
    // return new ResponseEntity<>("Token invalidated. Logout successful.",
    // HttpStatus.OK);
    // }

    // V2 simple logout
    @PutMapping("logout")
    public ResponseEntity<?> logoutUser() {
        return new ResponseEntity<>("Logged out successfully", HttpStatus.OK);
    }
}
