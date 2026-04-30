package strategy;

import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.strategy.PricingStrategy;
import com.codingshuttle.projects.airBnbApp.strategy.UrgencyPricingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class UrgencyPricingStrategyTest {

    private final PricingStrategy baseStrategy =
            inventory -> BigDecimal.valueOf(1000);

    private final UrgencyPricingStrategy urgencyStrategy =
            new UrgencyPricingStrategy(baseStrategy);

    @Test
    void shouldApply15PercentMarkupWhenDateIsToday() {
        Inventory inventory = new Inventory();
        inventory.setDate(LocalDate.now());

        BigDecimal price = urgencyStrategy.calculatePrice(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1150));
    }

    @Test
    void shouldApply15PercentMarkupWhenDateIsWithIn7Days() {
        Inventory inventory = new Inventory();
        inventory.setDate(LocalDate.now().plusDays(6));

        BigDecimal price = urgencyStrategy.calculatePrice(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1150));
    }

    @Test
    void shouldNotApplyMarkupWhenDateIsExactly7DaysFromNow() {
        Inventory inventory = new Inventory();
        inventory.setDate(LocalDate.now().plusDays(7));

        BigDecimal price = urgencyStrategy.calculatePrice(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void shouldNotApplyMarkupWhenDateIsInThePast() {
        Inventory inventory = new Inventory();
        inventory.setDate(LocalDate.now().minusDays(1));

        BigDecimal price = urgencyStrategy.calculatePrice(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void shouldNotApplyMarkupWhenDateIsFarInFuture() {
        Inventory inventory = new Inventory();
        inventory.setDate(LocalDate.now().plusDays(30));

        BigDecimal price = urgencyStrategy.calculatePrice(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }
}
