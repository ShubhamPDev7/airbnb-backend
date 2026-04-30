package strategy;

import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.strategy.PricingStrategy;
import com.codingshuttle.projects.airBnbApp.strategy.SurgePricingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class SurgePricingStrategyTest {
    private final PricingStrategy baseStrategy = inventory -> BigDecimal.valueOf(1000);

    private final SurgePricingStrategy surgeStrategy =
            new SurgePricingStrategy(baseStrategy);

    @Test
    void shouldNotChangePriceWhenSurgeFactorIsOne() {
        Inventory inventory = new Inventory();
        inventory.setSurgeFactor(BigDecimal.valueOf(1.0));

        BigDecimal price = surgeStrategy.calculatePrice(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void shouldIncreasePriceWhenSurgeFactorIsGreaterThanOne() {
        Inventory inventory = new Inventory();
        inventory.setSurgeFactor(BigDecimal.valueOf(1.5));

        BigDecimal price = surgeStrategy.calculatePrice(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }

    @Test
    void shouldDecreasePriceWhenSurgeFactorIsLessThanOne() {
        Inventory inventory = new Inventory();
        inventory.setSurgeFactor(BigDecimal.valueOf(0.8));

        BigDecimal price = surgeStrategy.calculatePrice(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(800));
    }
}
