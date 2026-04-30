package strategy;

import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.strategy.HolidayPricingStrategy;
import com.codingshuttle.projects.airBnbApp.strategy.PricingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HolidayPricingStrategyTest {


    private final PricingStrategy baseStrategy = inventory -> BigDecimal.valueOf(1000);

    private final HolidayPricingStrategy holidayStrategy =
            new HolidayPricingStrategy(baseStrategy);

    @Test
    void shouldApply25PercentMarkupOnHoliday() {
        Inventory inventory = new Inventory();
        inventory.setDate(LocalDate.of(2025, 8, 15)); // Independence Day

        BigDecimal price = holidayStrategy.calculatePrice(inventory);

        // 1000 * 1.25 = 1250
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1250));
    }

    @Test
    void shouldNotApplyMarkupOnNormalDay() {
        Inventory inventory = new Inventory();
        inventory.setDate(LocalDate.of(2025, 7, 10)); // random normal day

        BigDecimal price = holidayStrategy.calculatePrice(inventory);

        // price should stay 1000, no markup
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }
}