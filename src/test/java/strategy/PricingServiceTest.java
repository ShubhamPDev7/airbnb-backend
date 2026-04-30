package strategy;

import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import com.codingshuttle.projects.airBnbApp.strategy.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class PricingServiceTest {

    private PricingService pricingService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();

        Room room = new Room();
        room.setBasePrice(BigDecimal.valueOf(1000));

        inventory = new Inventory();
        inventory.setRoom(room);

        inventory.setSurgeFactor(BigDecimal.valueOf(1.0));

        inventory.setBookedCount(5);
        inventory.setTotalCount(10);

        inventory.setDate(LocalDate.now().plusDays(30));
    }

    @Test
    void shouldReturnBasePriceWhenNoStrategyAppliesMarkup() {

        BigDecimal price = pricingService.calculateDynamicPricing(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void shouldApplyOccupancyMarkupWhenOccupancyAbove80Percent() {
        inventory.setBookedCount(9);
        inventory.setTotalCount(10);

        BigDecimal price = pricingService.calculateDynamicPricing(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1200));
    }

    @Test
    void shouldApplySurgeMarkupWhenSurgeFactorIsAboveOne() {
        inventory.setSurgeFactor(BigDecimal.valueOf(1.5));

        BigDecimal price = pricingService.calculateDynamicPricing(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }

    @Test
    void shouldCombineSurgeAndOccupancyMarkupWhenBothApply() {

        inventory.setSurgeFactor(BigDecimal.valueOf(1.5));
        inventory.setBookedCount(9);
        inventory.setTotalCount(10);

        BigDecimal price = pricingService.calculateDynamicPricing(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1800));
    }

    @Test
    void shouldApplyUrgencyMarkupWhenDateIsWithIn7Days() {
        inventory.setDate(LocalDate.now().plusDays(3));

        BigDecimal price = pricingService.calculateDynamicPricing(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1150));
    }

    @Test
    void shouldApplyHolidayMarkupOnPublicHoliday() {
        inventory.setDate(LocalDate.of(2030, 8, 15));

        BigDecimal price = pricingService.calculateDynamicPricing(inventory);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1250));
    }
}
