package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.dto.*;
import com.codingshuttle.projects.airBnbApp.entity.Hotel;
import com.codingshuttle.projects.airBnbApp.entity.HotelMinPrice;
import com.codingshuttle.projects.airBnbApp.entity.Inventory;
import com.codingshuttle.projects.airBnbApp.entity.Room;
import com.codingshuttle.projects.airBnbApp.entity.User;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.repository.HotelMinPriceRepository;
import com.codingshuttle.projects.airBnbApp.repository.InventoryRepository;
import com.codingshuttle.projects.airBnbApp.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.codingshuttle.projects.airBnbApp.util.AppUtils.getCurrentUser;
import static com.codingshuttle.projects.airBnbApp.util.AppUtils.toTitleCase;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService{
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;

    private final InventoryRepository inventoryRepository;
    private final HotelMinPriceRepository hotelMinPriceRepository;

    @Override
    public void initializeRoomForAYear(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        List<Inventory> newInventories = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(endDate); date = date.plusDays(1)) {
            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .reservedCount(0)
                    .city(room.getHotel().getCity())
                    .date(date)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            newInventories.add(inventory);
        }
        inventoryRepository.saveAll(newInventories);

        updateHotelMinPriceForHotel(room.getHotel(), newInventories, false);
    }

    @Override
    public void updateHotelMinPriceForHotel(Hotel hotel, List<Inventory> inventoryList) {
        updateHotelMinPriceForHotel(hotel, inventoryList, true);
    }

    private void updateHotelMinPriceForHotel(Hotel hotel, List<Inventory> inventoryList, boolean overwrite) {
        Map<LocalDate, Optional<BigDecimal>> dailyMinPrices = inventoryList.stream()
                .collect(Collectors.groupingBy(
                        Inventory::getDate,
                        Collectors.mapping(Inventory::getPrice, Collectors.minBy(Comparator.naturalOrder()))
                ));

        List<HotelMinPrice> hotelMinPrices = new ArrayList<>();
        dailyMinPrices.forEach((date, priceOpt) -> {
            BigDecimal newPrice = priceOpt.orElse(BigDecimal.ZERO);
            HotelMinPrice hotelMinPrice = hotelMinPriceRepository
                    .findByHotelAndDate(hotel, date)
                    .orElse(new HotelMinPrice(hotel, date));

            if (overwrite || hotelMinPrice.getPrice() == null || newPrice.compareTo(hotelMinPrice.getPrice()) < 0) {
                hotelMinPrice.setPrice(newPrice);
            }
            hotelMinPrices.add(hotelMinPrice);
        });

        hotelMinPriceRepository.saveAll(hotelMinPrices);
    }

    @Override
    public void deleteAllInventories(Room room) {
        log.info("Deleting the inventories of room with id: {}", room.getId());
        inventoryRepository.deleteByRoom(room);
    }


    @Override
    public Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest) {
        if (hotelSearchRequest.getCity() != null && !hotelSearchRequest.getCity().isEmpty()) {
            hotelSearchRequest.setCity(toTitleCase(hotelSearchRequest.getCity()));
        }

        log.info("Searching hotels for {} city, category {}, from {} to {}",
                hotelSearchRequest.getCity(), hotelSearchRequest.getCategory(), hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate());

        Pageable pageable = PageRequest.of(hotelSearchRequest.getPage(), hotelSearchRequest.getSize());
        long dateCount =
                ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate()) + 1;


        Integer roomsCount = hotelSearchRequest.getRoomsCount() == null || hotelSearchRequest.getRoomsCount() < 1
                ? 1
                : hotelSearchRequest.getRoomsCount();

        String categoryKeyword = hotelSearchRequest.getCategory();
        if (categoryKeyword != null) {
            categoryKeyword = categoryKeyword.trim().toLowerCase();
            if (categoryKeyword.length() > 1 && categoryKeyword.endsWith("s")) {
                categoryKeyword = categoryKeyword.substring(0, categoryKeyword.length() - 1);
            }
        }


        Page<HotelPriceDto> hotelPage =
                hotelMinPriceRepository.findHotelsWithAvailableInventory(
                        hotelSearchRequest.getCity(),
                        hotelSearchRequest.getStartDate(),
                        hotelSearchRequest.getEndDate(),
                        roomsCount,
                        dateCount,
                        hotelSearchRequest.getCategory(),
                        categoryKeyword,
                        pageable
                );

        return hotelPage;
    }

    @Override
    public List<InventoryDto> getAllInventoryByRoom(Long roomId) {
        log.info("Getting All inventory by room for room with id: {}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        User user = getCurrentUser();
        if(!user.equals(room.getHotel().getOwner())) throw new AccessDeniedException("You are not the owner of the room with id: " + roomId);

        return inventoryRepository.findByRoomOrderByDate(room)
                .stream()
                .map((element) -> modelMapper.map(element, InventoryDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto) {

        if (!updateInventoryRequestDto.getEndDate().isAfter(updateInventoryRequestDto.getStartDate())) {
            throw new IllegalStateException("End date must be after start date");
        }

        log.info("Updating All inventory by room for room with id: {} between date range: {} - {}", roomId,
                updateInventoryRequestDto.getStartDate(), updateInventoryRequestDto.getEndDate());

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        User user = getCurrentUser();
        if(!user.equals(room.getHotel().getOwner())) throw new AccessDeniedException("You are not the owner of the room with id: " + roomId);

        inventoryRepository.getInventoryAndLockBeforeUpdate(roomId, updateInventoryRequestDto.getStartDate(), updateInventoryRequestDto.getEndDate());

        inventoryRepository.updateInventory(roomId, updateInventoryRequestDto.getStartDate(),
                updateInventoryRequestDto.getEndDate(), updateInventoryRequestDto.getClosed(), updateInventoryRequestDto.getSurgeFactor());
    }
}
