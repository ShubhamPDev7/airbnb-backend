package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.entity.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Async
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            String body = """
                {
                    "sender": {"email": "shubhampawar5929@gmail.com", "name": "StayLux"},
                    "to": [{"email": "%s"}],
                    "subject": "%s",
                    "htmlContent": "%s"
                }
                """.formatted(to, subject, htmlContent.replace("\"", "\\\"").replace("\n", ""));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", apiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                log.info("Email sent successfully to: {}", to);
            } else {
                log.error("Failed to send email to: {}, status: {}, body: {}", to, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send email to: {}, reason: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendBookingConfirmationEmail(Booking booking) {
        sendEmail(
                booking.getUser().getEmail(),
                "Booking Confirmed - " + booking.getHotel().getName(),
                buildConfirmationEmailBody(booking)
        );
    }

    @Async
    public void sendBookingCancellationEmail(Booking booking) {
        sendEmail(
                booking.getUser().getEmail(),
                "Booking Cancelled - " + booking.getHotel().getName(),
                buildCancellationEmailBody(booking)
        );
    }

    private String buildConfirmationEmailBody(Booking booking) {
        return "<html><body style='font-family:Arial,sans-serif;padding:20px'>"
                + "<h2 style='color:#2e7d32'>Booking Confirmed!</h2>"
                + "<p>Dear <strong>" + booking.getUser().getName() + "</strong>,</p>"
                + "<p>Your booking has been confirmed.</p>"
                + "<p><strong>Hotel:</strong> " + booking.getHotel().getName() + "</p>"
                + "<p><strong>Check-in:</strong> " + booking.getCheckInDate() + "</p>"
                + "<p><strong>Check-out:</strong> " + booking.getCheckOutDate() + "</p>"
                + "<p><strong>Rooms:</strong> " + booking.getRoomsCount() + "</p>"
                + "<p><strong>Total:</strong> ₹" + booking.getAmount() + "</p>"
                + "<p>Thank you for choosing StayLux!</p>"
                + "</body></html>";
    }

    private String buildCancellationEmailBody(Booking booking) {
        return "<html><body style='font-family:Arial,sans-serif;padding:20px'>"
                + "<h2 style='color:#c62828'>Booking Cancelled</h2>"
                + "<p>Dear <strong>" + booking.getUser().getName() + "</strong>,</p>"
                + "<p>Your booking has been cancelled.</p>"
                + "<p><strong>Hotel:</strong> " + booking.getHotel().getName() + "</p>"
                + "<p><strong>Check-in:</strong> " + booking.getCheckInDate() + "</p>"
                + "<p><strong>Check-out:</strong> " + booking.getCheckOutDate() + "</p>"
                + "<p><strong>Amount Refunded:</strong> ₹" + booking.getAmount() + "</p>"
                + "<p>We hope to see you again soon!</p>"
                + "</body></html>";
    }
}