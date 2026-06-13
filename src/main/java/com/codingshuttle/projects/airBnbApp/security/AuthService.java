package com.codingshuttle.projects.airBnbApp.security;

import com.codingshuttle.projects.airBnbApp.dto.GoogleLoginDto;
import com.codingshuttle.projects.airBnbApp.dto.LoginDto;
import com.codingshuttle.projects.airBnbApp.dto.LoginResponseDto;
import com.codingshuttle.projects.airBnbApp.dto.SignUpRequestDto;
import com.codingshuttle.projects.airBnbApp.dto.UserDto;
import com.codingshuttle.projects.airBnbApp.entity.Otp;
import com.codingshuttle.projects.airBnbApp.entity.PasswordResetToken;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.entity.enums.Role;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.repository.OtpRepository;
import com.codingshuttle.projects.airBnbApp.repository.PasswordResetTokenRepository;
import com.codingshuttle.projects.airBnbApp.repository.UserRepository;

import com.codingshuttle.projects.airBnbApp.service.EmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.google.client-id}")
    private String googleClientId;

    @Value("${frontend.url}")
    private String frontendUrl;

    private void generateAndSendOtp(String email) {
        otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .ifPresent(otpRepository::delete);

        String code = String.format("%06d", new java.util.Random().nextInt(999999));

        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));
        otpRepository.save(otp);

        String subject = "Verify your StayLux account";
        String body = "<h3>Welcome to StayLux!</h3>" +
                "<p>Your 6-digit verification code is: <strong>" + code + "</strong></p>" +
                "<p>This code will expire in 10 minutes.</p>";
        // Assuming your EmailService has a method like sendEmail(to, subject, body)
        emailService.sendEmail(email, subject, body);
    }

    public void verifyEmail(String email, String code) {
        Otp otp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("No verification code found for this email."));

        if (otp.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Verification code has expired. Please request a new one.");
        }

        if (!otp.getCode().equals(code)) {
            throw new RuntimeException("Invalid verification code.");
        }


        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsVerified(true);
        userRepository.save(user);


        otpRepository.delete(otp);
    }

    public UserDto signUp(SignUpRequestDto signUpRequestDto){

        User user = userRepository.findByEmail(signUpRequestDto.getEmail()).orElse(null);

        if (user != null) {
            throw new RuntimeException("User is already present with same email id");
        }

        User newUser = modelMapper.map(signUpRequestDto, User.class);
        newUser.setRoles(Set.of(Role.GUEST));
        newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));


        newUser.setIsVerified(false);
        log.info("Saving new user: {}", newUser.getEmail());
        newUser = userRepository.save(newUser);
        log.info("User saved. Now calling generateAndSendOtp...");


        generateAndSendOtp(newUser.getEmail());
        log.info("generateAndSendOtp finished execution.");

        return modelMapper.map(newUser, UserDto.class);
    }

    public LoginResponseDto login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        // 3. Block login if the email is not verified
        if (user.getIsVerified() == null || !user.getIsVerified()) {
            throw new RuntimeException("Please verify your email before logging in.");
        }

        UserDto userDto = modelMapper.map(user, UserDto.class);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new LoginResponseDto(accessToken, userDto, refreshToken);
    }

    public String refreshToken(String refreshToken){
        Long id = jwtService.getUserIdFromToken(refreshToken);

        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: "+id));
        return jwtService.generateAccessToken(user);
    }

    public LoginResponseDto googleLogin(GoogleLoginDto googleLoginDto) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleLoginDto.getIdToken());

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                // Check if the user already exists in our database
                User user = userRepository.findByEmail(email).orElse(null);

                if (user == null) {
                    user = new User();
                    user.setEmail(email);
                    user.setName(name);
                    user.setRoles(Set.of(Role.GUEST));
                    // Generate a random secure password since they use Google to auth
                    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    user.setIsVerified(true);
                    user = userRepository.save(user);
                }

                UserDto userDto = modelMapper.map(user, UserDto.class);
                String accessToken = jwtService.generateAccessToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);

                return new LoginResponseDto(accessToken, userDto, refreshToken);

            } else {
                throw new RuntimeException("Invalid Google ID token.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google token", e);
        }
    }



    public void forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with that email address."));


        passwordResetTokenRepository.deleteByEmail(email);


        String token = UUID.randomUUID().toString();


        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.save(resetToken);


        String resetLink = frontendUrl + "/reset-password?token=" + token;

        String subject = "Reset your StayLux password";
        String body = "<h3>Password Reset Request</h3>" +
                "<p>We received a request to reset your password. Click the link below to set a new one:</p>" +
                "<p><a href=\"" + resetLink + "\">Reset Password</a></p>" +
                "<p>If you did not request this, please ignore this email. This link will expire in 15 minutes.</p>";

        emailService.sendEmail(email, subject, body);
    }

    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or missing reset token."));


        if (resetToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("This reset link has expired. Please request a new one.");
        }


        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));


        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);


        passwordResetTokenRepository.delete(resetToken);
    }

}