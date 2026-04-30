package strategy;

import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.strategy.OccupancyPricingStrategy;
import com.codingshuttle.projects.airBnbApp.strategy.PricingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class OccupancyPricingStrategyTest {

    private final PricingStrategy baseStrategy = inventory -> BigDecimal.valueOf(1000);

    private final OccupancyPricingStrategy occupancyStrategy =
            new OccupancyPricingStrategy(baseStrategy);

    @Test
    void shouldApply20PercentMarkupWhenOccupancyAbove80Percent() {
        Inventory inventory = new Inventory();
        inventory.setBookedCount(9);
        inventory.setTotalCount(10);

        BigDecimal price = occupancyStrategy.calculatePrice(inventory);

//        1000 * 1.2 = 1200
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1200));
    }

    @Test
    void shouldNotApplyMarkupWhenOccupancyIsExactly80Percent() {
        Inventory inventory = new Inventory();
        inventory.setBookedCount(8);
        inventory.setTotalCount(10);

        BigDecimal price = occupancyStrategy.calculatePrice(inventory);

//        condition is strictly > 0.8, so no markup
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void shouldNotApplyMarkupWhenOccupancyBelow80Percent() {
        Inventory inventory = new Inventory();
        inventory.setBookedCount(7);
        inventory.setTotalCount(10);

        BigDecimal price = occupancyStrategy.calculatePrice(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }
}
