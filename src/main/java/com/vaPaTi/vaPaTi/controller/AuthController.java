package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.AuthRequest;
import com.vaPaTi.vaPaTi.dtos.AuthResponse;
import com.vaPaTi.vaPaTi.dtos.RefreshTokenRequest;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse authResponse = authenticationService.authenticate(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse authResponse = authenticationService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(authResponse);
        } catch (MessageException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}