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

    private void generateAndSendOtp(String email) {
        // 1. Remove any old OTPs for this email to prevent clutter
        otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .ifPresent(otpRepository::delete);

        // 2. Generate a random 6-digit code
        String code = String.format("%06d", new java.util.Random().nextInt(999999));

        // 3. Save it to the database
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));
        otpRepository.save(otp);

        // 4. Send the email!
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

        // Code is valid! Verify the user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsVerified(true);
        userRepository.save(user);

        // Clean up the used OTP
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

        // 1. Explicitly set the user as unverified
        newUser.setIsVerified(false);
        log.info("Saving new user: {}", newUser.getEmail());
        newUser = userRepository.save(newUser);
        log.info("User saved. Now calling generateAndSendOtp...");

        // 2. Automatically generate and email the 6-digit OTP
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

        return new LoginResponseDto(accessToken, userDto);
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

                return new LoginResponseDto(accessToken, userDto);

            } else {
                throw new RuntimeException("Invalid Google ID token.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google token", e);
        }
    }



    public void forgotPassword(String email) {
        // 1. Verify the user actually exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with that email address."));

        // 2. Clean up any old tokens for this email
        passwordResetTokenRepository.deleteByEmail(email);

        // 3. Generate a secure, random UUID token
        String token = UUID.randomUUID().toString();

        // 4. Save it to the database (valid for 15 minutes)
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.save(resetToken);

        // 5. Send the Magic Link email
        // Note: We use localhost:5173 which is your React frontend port!
        String resetLink = "http://localhost:5173/reset-password?token=" + token;

        String subject = "Reset your StayLux password";
        String body = "<h3>Password Reset Request</h3>" +
                "<p>We received a request to reset your password. Click the link below to set a new one:</p>" +
                "<p><a href=\"" + resetLink + "\">Reset Password</a></p>" +
                "<p>If you did not request this, please ignore this email. This link will expire in 15 minutes.</p>";

        emailService.sendEmail(email, subject, body);
    }

    public void resetPassword(String token, String newPassword) {
        // 1. Find the token in the database
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or missing reset token."));

        // 2. Check if it has expired
        if (resetToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("This reset link has expired. Please request a new one.");
        }

        // 3. Find the user associated with this token's email
        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // 4. Update their password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 5. Delete the token so it cannot be used again
        passwordResetTokenRepository.delete(resetToken);
    }

}