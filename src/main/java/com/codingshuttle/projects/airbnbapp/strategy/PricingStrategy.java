package com.codingshuttle.projects.airbnbapp.strategy;

import com.codingshuttle.projects.airbnbapp.entity.Inventory;

import java.math.BigDecimal;

public interface PricingStrategy {

    BigDecimal calculatePrice(Inventory inventory);

}
