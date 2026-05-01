package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.entity.Booking;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendBookingConfirmationEmail(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Booking Confirmed - " + booking.getHotel().getName());
            helper.setText(buildConfirmationEmailBody(booking), true); // true = HTML

            mailSender.send(message);
            log.info("Confirmation email sent for booking ID: {}", booking.getId());

        } catch (Exception e) {
            log.error("Failed to send confirmation email for booking ID: {}, reason: {}",
                    booking.getId(), e.getMessage());
        }
    }

    @Async
    public void sendBookingCancellationEmail(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Booking Cancelled - " + booking.getHotel().getName());
            helper.setText(buildCancellationEmailBody(booking), true);

            mailSender.send(message);
            log.info("Cancellation email sent for booking ID: {}", booking.getId());

        } catch (Exception e) {
            log.error("Failed to send cancellation email for booking ID: {}, reason: {}",
                    booking.getId(), e.getMessage());
        }
    }

    private String buildConfirmationEmailBody(Booking booking) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #2e7d32;">Booking Confirmed!</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your booking has been confirmed. Here are your details:</p>
                    <table style="border-collapse: collapse; width: 100%%;">
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Hotel</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Check-in</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Check-out</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Rooms</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%d</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Total Amount</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">₹%s</td>
                        </tr>
                    </table>
                    <p style="margin-top: 20px;">Thank you for choosing us. Enjoy your stay!</p>
                </body>
                </html>
                """.formatted(
                booking.getUser().getName(),
                booking.getHotel().getName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount(),
                booking.getAmount()
        );
    }

    private String buildCancellationEmailBody(Booking booking) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #c62828;">Booking Cancelled</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your booking has been cancelled. Here are the details:</p>
                    <table style="border-collapse: collapse; width: 100%%;">
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Hotel</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Check-in</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Check-out</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Amount Refunded</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">₹%s</td>
                        </tr>
                    </table>
                    <p style="margin-top: 20px;">We hope to see you again soon!</p>
                </body>
                </html>
                """.formatted(
                booking.getUser().getName(),
                booking.getHotel().getName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getAmount()
        );
    }
}