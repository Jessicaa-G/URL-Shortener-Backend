package com.example.urlshortener.Controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.urlshortener.model.SubscriptionDto;
import com.example.urlshortener.model.UrlErrorResponse;
import com.example.urlshortener.model.User;
import com.example.urlshortener.model.exception.BadRequestException;
import com.example.urlshortener.model.exception.ConflictException;
import com.example.urlshortener.model.exception.NotFoundException;
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
import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    @PostMapping("user")
    public ResponseEntity<?> addUser(@RequestBody User user) {
        try {
            User res = userService.createUser(user);
            return new ResponseEntity<>(res, HttpStatus.OK);

        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("user")
    public ResponseEntity<?> deleteUrl(@RequestParam String userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok("User successfully deleted");
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("subscribe")
    public ResponseEntity<?> updateSubscription(@RequestBody SubscriptionDto dto) {
        try {
            User user = userService.updateSubscription(dto.getUserId(), dto.getTier());
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (BadRequestException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("login")
    public ResponseEntity<?> loginUser(@RequestBody User user, HttpServletRequest request,
            HttpServletResponse response) {
        String userId = userService.userLoggedIn(request.getCookies());
        if (userId != "")
            return new ResponseEntity<>("User " + userId + " already logged in", HttpStatus.OK);

        User storedUser = userService.getUserByEmail(user.getEmail());
        if (storedUser != null && storedUser.getPassword()
                .equals(Hashing.sha256().hashString(user.getPassword(), StandardCharsets.UTF_8).toString())) {
            user.setId(storedUser.getId());
            String token = jwtService.generateToken(user);
            Cookie authCookie = new Cookie("auth_token", token);
            authCookie.setHttpOnly(true);
            authCookie.setPath("/");
            response.addCookie(authCookie);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "Login successful");
            responseData.put("email", storedUser.getEmail()); // Using email as a username
            responseData.put("tier", storedUser.getTier()); // Retrieved from User class
            return new ResponseEntity<>(responseData, HttpStatus.OK);
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

    @GetMapping("user")
    public ResponseEntity<?> getUserInfo(HttpServletRequest request) {
        String userId = userService.userLoggedIn(request.getCookies());
        if (userId == "") return new ResponseEntity<>("Error, token not valid (user not logged in)", HttpStatus.BAD_REQUEST);

        User storedUser = userService.getUserById(userId);
        if(storedUser == null) return new ResponseEntity<>("Error, user not found", HttpStatus.NOT_FOUND);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("email", storedUser.getEmail()); // Using email as a username
        responseData.put("tier", storedUser.getTier()); // Retrieved from User class
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }
}
