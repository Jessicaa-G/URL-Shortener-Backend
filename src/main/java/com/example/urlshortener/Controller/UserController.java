package com.example.urlshortener.Controller;

import com.example.urlshortener.model.SubscriptionDto;
import com.example.urlshortener.model.UrlErrorResponse;
import com.example.urlshortener.model.User;
import com.example.urlshortener.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("user")
    public ResponseEntity<?> addUser(@RequestBody User user) {
        User res = userService.createUser(user);

        if(res!=null){
            return new ResponseEntity<>(res, HttpStatus.OK);
        }

        UrlErrorResponse error = new UrlErrorResponse("404", "Error occurred when creating user");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @PutMapping("subscribe")
    public ResponseEntity<?> updateSubscription(@RequestBody SubscriptionDto dto) {
        User user = userService.updateSubscription(dto.getUserId(), dto.getTier());

        if(user!=null){
            return new ResponseEntity<>(user, HttpStatus.OK);
        }

        UrlErrorResponse error = new UrlErrorResponse("404", "User not found");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}
