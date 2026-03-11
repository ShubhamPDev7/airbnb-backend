package com.codingshuttle.projects.airbnbapp.repository;

import com.codingshuttle.projects.airbnbapp.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory,Long> {
}
