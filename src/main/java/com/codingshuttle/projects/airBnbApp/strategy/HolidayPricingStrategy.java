package com.codingshuttle.projects.airBnbApp.strategy;

import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@RequiredArgsConstructor
public class HolidayPricingStrategy implements PricingStrategy {

    private final PricingStrategy wrapped;

    // Indian public holidays (month-day format, year-independent)
    private static final Set<String> PUBLIC_HOLIDAYS = Set.of(
            "01-26", // Republic Day
            "08-15", // Independence Day
            "10-02", // Gandhi Jayanti
            "12-25", // Christmas
            "11-01", // Diwali approx (you can adjust)
            "03-25", // Holi approx
            "10-24", // Dussehra approx
            "01-14"  // Makar Sankranti
    );

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);

        if (isHoliday(inventory.getDate())) {
            price = price.multiply(BigDecimal.valueOf(1.25));
        }
        return price;
    }

    private boolean isHoliday(LocalDate date) {
        String monthDay = String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
        return PUBLIC_HOLIDAYS.contains(monthDay);
    }
}
