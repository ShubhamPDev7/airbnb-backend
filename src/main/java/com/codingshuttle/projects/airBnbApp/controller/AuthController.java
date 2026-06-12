package com.codingshuttle.projects.airBnbApp.controller;

import com.codingshuttle.projects.airBnbApp.dto.*;
import com.codingshuttle.projects.airBnbApp.security.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        return new ResponseEntity<>(authService.signUp(signUpRequestDto), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginDto loginDto,
                                                  HttpServletResponse httpServletResponse) {
        LoginResponseDto loginResponse = authService.login(loginDto);

        Cookie cookie = new Cookie("refreshToken", loginResponse.getAccessToken());
        cookie.setHttpOnly(true);
        httpServletResponse.addCookie(cookie);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(HttpServletRequest request) {
        String refreshToken = Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new AuthenticationServiceException("Refresh token not found inside the Cookies"));

        String accessToken = authService.refreshToken(refreshToken);

        return ResponseEntity.ok(new LoginResponseDto(accessToken, null));
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponseDto> googleLogin(@RequestBody GoogleLoginDto googleLoginDto,
                                                        HttpServletResponse httpServletResponse) {

        LoginResponseDto loginResponse = authService.googleLogin(googleLoginDto);

        // Same cookie logic as your standard login
        Cookie cookie = new Cookie("refreshToken", loginResponse.getAccessToken());
        cookie.setHttpOnly(true);
        httpServletResponse.addCookie(cookie);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<java.util.Map<String, String>> verifyEmail(@RequestBody VerifyEmailDto verifyEmailDto) {

        // 1. Verify the code
        authService.verifyEmail(verifyEmailDto.getEmail(), verifyEmailDto.getCode());

        // 2. Return a JSON object (Map) instead of a plain String
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Email successfully verified!");

        return ResponseEntity.ok(response);
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, String>> forgotPassword(@RequestBody ForgotPasswordDto dto) {
        authService.forgotPassword(dto.getEmail());

        java.util.Map<String, String> response = new java.util.HashMap<>();
        // We return a generic success message even if the email wasn't found to prevent "email enumeration" attacks
        response.put("message", "If an account exists for that email, a reset link has been sent.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<java.util.Map<String, String>> resetPassword(@RequestBody ResetPasswordDto dto) {
        authService.resetPassword(dto.getToken(), dto.getNewPassword());

        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Password has been successfully reset. You can now log in.");

        return ResponseEntity.ok(response);
    }

}
