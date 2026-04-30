package service;

import com.codingshuttle.projects.airBnbApp.dto.BookingDto;
import com.codingshuttle.projects.airBnbApp.dto.BookingRequest;
import com.codingshuttle.projects.airBnbApp.dto.GuestDto;
import com.codingshuttle.projects.airBnbApp.entity.*;
import com.codingshuttle.projects.airBnbApp.entity.enums.BookingStatus;
import com.codingshuttle.projects.airBnbApp.exception.ResourceNotFoundException;
import com.codingshuttle.projects.airBnbApp.exception.UnAuthorisedException;
import com.codingshuttle.projects.airBnbApp.repository.*;
import com.codingshuttle.projects.airBnbApp.service.BookingServiceImpl;
import com.codingshuttle.projects.airBnbApp.service.CheckOutService;
import com.codingshuttle.projects.airBnbApp.strategy.PricingService;
import com.codingshuttle.projects.airBnbApp.util.AppUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock private HotelRepository hotelRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private PricingService pricingService;
    @Mock private CheckOutService checkOutService;
    @Mock private ModelMapper modelMapper;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private BookingRequest bookingRequest;
    private Hotel hotel;
    private Room room;
    private User currentUser;
    private User differentUser;
    private Booking freshReservedBooking;

    @BeforeEach
    void setUp() {
        // inject @Value field since we're not loading Spring context
        ReflectionTestUtils.setField(bookingService, "frontendUrl", "http://localhost:3000");

        bookingRequest = new BookingRequest();
        bookingRequest.setHotelId(1L);
        bookingRequest.setRoomId(2L);
        bookingRequest.setCheckInDate(LocalDate.now().plusDays(1));
        bookingRequest.setCheckOutDate(LocalDate.now().plusDays(4));
        bookingRequest.setRoomsCount(1);

        hotel = new Hotel();
        hotel.setId(1L);

        room = new Room();
        room.setId(2L);

        currentUser = new User();
        currentUser.setId(10L);

        differentUser = new User();
        differentUser.setId(99L);

        // a fresh RESERVED booking that belongs to currentUser
        // used across many tests
        freshReservedBooking = Booking.builder()
                .id(1L)
                .user(currentUser)
                .hotel(hotel)
                .room(room)
                .bookingStatus(BookingStatus.RESERVED)
                .createdAt(LocalDateTime.now())
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(4))
                .roomsCount(1)
                .amount(BigDecimal.valueOf(3000))
                .guests(new HashSet<>())
                .build();
    }

    // ---- initialiseBooking tests ----

    @Test
    void initialiseBooking_shouldThrowWhenCheckOutIsNotAfterCheckIn() {
        bookingRequest.setCheckOutDate(bookingRequest.getCheckInDate());

        assertThatThrownBy(() -> bookingService.initialiseBooking(bookingRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Check-out date must be after check-in date");
    }

    @Test
    void initialiseBooking_shouldThrowWhenHotelNotFound() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.initialiseBooking(bookingRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Hotel not found with id: 1");
    }

    @Test
    void initialiseBooking_shouldThrowWhenRoomNotFound() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(roomRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.initialiseBooking(bookingRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room not found with id: 2");
    }

    @Test
    void initialiseBooking_shouldThrowWhenRoomIsNotAvailable() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(room));

        // only 2 inventory records returned but we need 3 days (days 1,2,3)
        when(inventoryRepository.findAndLockAvailableInventory(
                any(), any(), any(), any()))
                .thenReturn(List.of(new Inventory(), new Inventory()));

        assertThatThrownBy(() -> bookingService.initialiseBooking(bookingRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Room is not available anymore");
    }

    @Test
    void initialiseBooking_shouldInitialiseBookingSuccessfully() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(room));

        // 3 inventory records = 3 days (matches plusDays(1) to plusDays(4))
        List<Inventory> inventoryList = List.of(
                new Inventory(), new Inventory(), new Inventory());
        when(inventoryRepository.findAndLockAvailableInventory(
                any(Long.class), any(LocalDate.class), any(LocalDate.class), anyInt()))
                .thenReturn(inventoryList);

        when(pricingService.calculateTotalPrice(inventoryList))
                .thenReturn(BigDecimal.valueOf(3000));

        Booking savedBooking = Booking.builder()
                .id(100L)
                .hotel(hotel)
                .room(room)
                .bookingStatus(BookingStatus.RESERVED)
                .amount(BigDecimal.valueOf(3000))
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingDto expectedDto = new BookingDto();
        expectedDto.setId(100L);
        when(modelMapper.map(savedBooking, BookingDto.class)).thenReturn(expectedDto);

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            BookingDto result = bookingService.initialiseBooking(bookingRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            verify(bookingRepository).save(any(Booking.class));
            verify(inventoryRepository).initBooking(
                    any(Long.class), any(LocalDate.class), any(LocalDate.class), anyInt());
        }
    }

    // ---- addGuests tests ----

    @Test
    void addGuests_shouldThrowWhenBookingNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.addGuests(99L, List.of()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found with id: 99");
    }

    @Test
    void addGuests_shouldThrowWhenBookingBelongsToDifferentUser() {
        Booking bookingOwnedByOther = Booking.builder()
                .id(1L)
                .user(differentUser)
                .bookingStatus(BookingStatus.RESERVED)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(bookingOwnedByOther));

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            assertThatThrownBy(() -> bookingService.addGuests(1L, List.of()))
                    .isInstanceOf(UnAuthorisedException.class)
                    .hasMessageContaining("Booking does not belong to this user");
        }
    }

    @Test
    void addGuests_shouldThrowWhenBookingHasExpired() {
        Booking expiredBooking = Booking.builder()
                .id(1L)
                .user(currentUser)
                .bookingStatus(BookingStatus.RESERVED)
                .createdAt(LocalDateTime.now().minusMinutes(20)) // expired
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(expiredBooking));

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            assertThatThrownBy(() -> bookingService.addGuests(1L, List.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Booking has already expired");
        }
    }

    @Test
    void addGuests_shouldThrowWhenBookingStatusIsNotReserved() {
        Booking confirmedBooking = Booking.builder()
                .id(1L)
                .user(currentUser)
                .bookingStatus(BookingStatus.CONFIRMED) // wrong status
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(confirmedBooking));

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            assertThatThrownBy(() -> bookingService.addGuests(1L, List.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Booking is not under reserved state");
        }
    }

    @Test
    void addGuests_shouldAddGuestsSuccessfully() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(freshReservedBooking));

        GuestDto guestDto = new GuestDto();
        Guest guest = new Guest();
        when(modelMapper.map(guestDto, Guest.class)).thenReturn(guest);
        when(guestRepository.save(any(Guest.class))).thenReturn(guest);

        Booking savedBooking = Booking.builder()
                .id(1L)
                .bookingStatus(BookingStatus.GUESTS_ADDED)
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingDto expectedDto = new BookingDto();
        expectedDto.setId(1L);
        when(modelMapper.map(savedBooking, BookingDto.class)).thenReturn(expectedDto);

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            BookingDto result = bookingService.addGuests(1L, List.of(guestDto));

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(guestRepository).save(any(Guest.class));
            verify(bookingRepository).save(any(Booking.class));
        }
    }

    // ---- initiatePayments tests ----

    @Test
    void initiatePayments_shouldThrowWhenBookingNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.initiatePayments(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found with id: 99");
    }

    @Test
    void initiatePayments_shouldThrowWhenBookingBelongsToDifferentUser() {
        Booking bookingOwnedByOther = Booking.builder()
                .id(1L)
                .user(differentUser)
                .bookingStatus(BookingStatus.GUESTS_ADDED)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(bookingOwnedByOther));

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            assertThatThrownBy(() -> bookingService.initiatePayments(1L))
                    .isInstanceOf(UnAuthorisedException.class)
                    .hasMessageContaining("Booking does not belong to this user");
        }
    }

    @Test
    void initiatePayments_shouldThrowWhenBookingHasExpired() {
        Booking expiredBooking = Booking.builder()
                .id(1L)
                .user(currentUser)
                .bookingStatus(BookingStatus.GUESTS_ADDED)
                .createdAt(LocalDateTime.now().minusMinutes(20)) // expired
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(expiredBooking));

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            assertThatThrownBy(() -> bookingService.initiatePayments(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Booking has already expired");
        }
    }

    @Test
    void initiatePayments_shouldReturnSessionUrlSuccessfully() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(freshReservedBooking));
        when(checkOutService.getCheckoutSession(any(), any(), any()))
                .thenReturn("https://stripe.com/pay/test_session");
        when(bookingRepository.save(any(Booking.class))).thenReturn(freshReservedBooking);

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            String sessionUrl = bookingService.initiatePayments(1L);

            assertThat(sessionUrl).isEqualTo("https://stripe.com/pay/test_session");
            verify(bookingRepository).save(any(Booking.class));
        }
    }

    // ---- cancelBooking tests ----

    @Test
    void cancelBooking_shouldThrowWhenBookingNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found with id: 99");
    }

    @Test
    void cancelBooking_shouldThrowWhenBookingBelongsToDifferentUser() {
        Booking bookingOwnedByOther = Booking.builder()
                .id(1L)
                .user(differentUser)
                .bookingStatus(BookingStatus.RESERVED)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(bookingOwnedByOther));

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                    .isInstanceOf(UnAuthorisedException.class)
                    .hasMessageContaining("Booking does not belong to this user");
        }
    }

    @Test
    void cancelBooking_shouldCancelAndReleaseInventoryWhenStatusIsReserved() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(freshReservedBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(freshReservedBooking);

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            bookingService.cancelBooking(1L);

            // verify inventory was released
            verify(inventoryRepository).releaseReservedInventory(
                    any(Long.class), any(LocalDate.class), any(LocalDate.class), anyInt());

            // verify booking was saved with CANCELLED status
            verify(bookingRepository).save(argThat(b ->
                    b.getBookingStatus() == BookingStatus.CANCELLED));
        }
    }

    @Test
    void cancelBooking_shouldCancelAndReleaseInventoryWhenStatusIsGuestsAdded() {
        Booking guestsAddedBooking = Booking.builder()
                .id(1L)
                .user(currentUser)
                .hotel(hotel)
                .room(room)
                .bookingStatus(BookingStatus.GUESTS_ADDED)
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(4))
                .roomsCount(1)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(guestsAddedBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(guestsAddedBooking);

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            bookingService.cancelBooking(1L);

            verify(inventoryRepository).releaseReservedInventory(
                    any(Long.class), any(LocalDate.class), any(LocalDate.class), anyInt());
            verify(bookingRepository).save(argThat(b ->
                    b.getBookingStatus() == BookingStatus.CANCELLED));
        }
    }

    @Test
    void cancelBooking_shouldThrowWhenStatusIsAlreadyCancelled() {
        Booking cancelledBooking = Booking.builder()
                .id(1L)
                .user(currentUser)
                .bookingStatus(BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(cancelledBooking));

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot be cancelled");
        }
    }

    // ---- getBookingStatus tests ----

    @Test
    void getBookingStatus_shouldThrowWhenBookingNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingStatus(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found with id: 99");
    }

    @Test
    void getBookingStatus_shouldThrowWhenBookingBelongsToDifferentUser() {
        Booking bookingOwnedByOther = Booking.builder()
                .id(1L)
                .user(differentUser)
                .bookingStatus(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(bookingOwnedByOther));

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            assertThatThrownBy(() -> bookingService.getBookingStatus(1L))
                    .isInstanceOf(UnAuthorisedException.class)
                    .hasMessageContaining("Booking does not belong to this user");
        }
    }

    @Test
    void getBookingStatus_shouldReturnStatusSuccessfully() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(freshReservedBooking));

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            String status = bookingService.getBookingStatus(1L);

            assertThat(status).isEqualTo("RESERVED");
        }
    }

    // ---- getMyBookings tests ----

    @Test
    void getMyBookings_shouldReturnEmptyListWhenNoBookings() {
        when(bookingRepository.findByUser(currentUser)).thenReturn(List.of());

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            List<BookingDto> result = bookingService.getMyBookings();

            assertThat(result).isEmpty();
        }
    }

    @Test
    void getMyBookings_shouldReturnBookingsSuccessfully() {
        when(bookingRepository.findByUser(currentUser))
                .thenReturn(List.of(freshReservedBooking));

        BookingDto bookingDto = new BookingDto();
        bookingDto.setId(1L);
        when(modelMapper.map(freshReservedBooking, BookingDto.class)).thenReturn(bookingDto);

        try (MockedStatic<AppUtils> mockedStatic = mockStatic(AppUtils.class)) {
            mockedStatic.when(AppUtils::getCurrentUser).thenReturn(currentUser);

            List<BookingDto> result = bookingService.getMyBookings();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }
    }
}