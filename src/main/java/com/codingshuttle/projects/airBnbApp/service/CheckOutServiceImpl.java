package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.entity.Booking;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.repository.BookingRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
@Service
@Slf4j
public class CheckOutServiceImpl implements CheckOutService {

    private final BookingRepository bookingRepository;


    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.12");

    @Override
    public String getCheckoutSession(Booking booking, String successUrl, String failureUrl) {
        log.info("Creating session for booking with ID: {}", booking.getId());
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            BigDecimal baseAmount   = booking.getAmount();
            BigDecimal serviceFee   = baseAmount.multiply(SERVICE_FEE_RATE)
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal totalAmount  = baseAmount.add(serviceFee);

            long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());

            String productDescription = String.format(
                    "Check-in: %s  |  Check-out: %s  |  %d night%s  |  %d room%s  |  Booking ID: %d",
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    nights, nights == 1 ? "" : "s",
                    booking.getRoomsCount(), booking.getRoomsCount() == 1 ? "" : "s",
                    booking.getId()
            );

            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setName(user.getName())
                    .setEmail(user.getEmail())
                    .build();

            Customer customer = Customer.create(customerParams);


            SessionCreateParams.LineItem roomLineItem = SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("inr")
                                    .setUnitAmount(baseAmount.multiply(BigDecimal.valueOf(100)).longValue())
                                    .setProductData(
                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                    .setName(booking.getHotel().getName() + " : " + booking.getRoom().getType())
                                                    .setDescription(productDescription)
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();


            SessionCreateParams.LineItem serviceFeeLineItem = SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("inr")
                                    .setUnitAmount(serviceFee.multiply(BigDecimal.valueOf(100)).longValue())
                                    .setProductData(
                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                    .setName("Service fee")
                                                    .setDescription("Platform service charge (12%)")
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();


            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                    .setCustomer(customer.getId())
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(failureUrl)
                    .addLineItem(roomLineItem)
                    .addLineItem(serviceFeeLineItem)
                    .build();

            Session session = Session.create(sessionParams);


            booking.setPaymentSessionId(session.getId());
            booking.setAmount(totalAmount);
            bookingRepository.save(booking);

            log.info("Session created for booking ID: {} | base: {} | fee: {} | total: {}",
                    booking.getId(), baseAmount, serviceFee, totalAmount);

            return session.getUrl();

        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }
}