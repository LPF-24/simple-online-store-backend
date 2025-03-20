package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.service.PeopleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/people")
@RequiredArgsConstructor
public class PeopleController {
    private final PeopleService peopleService;

    //TODO
    @PatchMapping("/{id}/deactivate-account")
    public ResponseEntity<HttpStatus> deactivateAccount(@PathVariable("id") int userId) {
        System.out.println("Method deactivateAccount started");
        peopleService.deactivateUserAccount(userId);
        System.out.println("Method deactivateAccount ended");

        return ResponseEntity.ok(HttpStatus.OK);
    }
}
