# Airbnb Clone — Backend

A full-featured hotel booking backend built with Spring Boot, inspired by Airbnb. Supports hotel management, room inventory, dynamic pricing, Stripe payments, and guest management.

---

## Tech Stack

- **Java 21** + **Spring Boot 4**
- **Spring Security** — JWT-based stateless authentication
- **Spring Data JPA** — PostgreSQL with pessimistic locking
- **Stripe** — Checkout sessions, webhooks, refunds
- **ModelMapper** — DTO mapping
- **Lombok** — Boilerplate reduction
- **Springdoc OpenAPI** — Swagger UI (`/swagger-ui.html`)

---

## Features

### Auth
- Signup / Login with JWT
- Refresh token via HTTP-only cookie
- Role-based access: `GUEST`, `HOTEL_MANAGER`

### Hotel Management (Admin)
- Create, update, delete, activate hotels
- Add and manage rooms per hotel
- View all bookings for a hotel
- Generate revenue reports with date range filtering

### Inventory Management (Admin)
- Auto-initialize 1 year of inventory per room on hotel activation
- Update surge factor and open/close rooms by date range
- Pessimistic locking to prevent race conditions

### Booking Flow (User)
1. **Init Booking** — reserves inventory with pessimistic lock, calculates dynamic price
2. **Add Guests** — attach guest info to booking
3. **Initiate Payment** — creates Stripe checkout session
4. **Webhook** — Stripe confirms payment, booking marked `CONFIRMED`
5. **Cancel** — auto-refund via Stripe, inventory released

### Dynamic Pricing (Decorator Pattern)
Pricing strategies applied in chain:
- **Base** — room base price × surge factor
- **Surge** — custom multiplier per inventory record
- **Occupancy** — +20% if occupancy > 80%
- **Urgency** — higher price as check-in date approaches
- **Holiday** — premium on public holidays

### Guest Management (User)
- Add, update, delete personal guest profiles
- Reuse guests across bookings

### User Profile
- View and update profile (name, gender, date of birth)
- View all personal bookings

---

## API Endpoints

### Auth — `/api/v1/auth`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/signup` | Register new user |
| POST | `/login` | Login, returns JWT |
| POST | `/refresh` | Refresh access token |

### Admin Hotels — `/api/v1/admin/hotels`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create hotel |
| GET | `/` | Get all hotels |
| GET | `/{hotelId}` | Get hotel by ID |
| PUT | `/{hotelId}` | Update hotel |
| DELETE | `/{hotelId}` | Delete hotel |
| PATCH | `/{hotelId}/activate` | Activate hotel |
| GET | `/{hotelId}/bookings` | Get all bookings |
| GET | `/{hotelId}/reports` | Revenue report |

### Admin Rooms — `/api/v1/admin/hotels/{hotelId}/rooms`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create room |
| GET | `/` | Get all rooms |
| GET | `/{roomId}` | Get room by ID |
| PUT | `/{roomId}` | Update room |
| DELETE | `/{roomId}` | Delete room |

### Admin Inventory — `/api/v1/admin/inventory`
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/rooms/{roomId}` | Get inventory by room |
| PATCH | `/rooms/{roomId}` | Update surge/closed status |

### Browse (Public) — `/api/v1/hotels`
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/search` | Search hotels by city & dates |
| GET | `/{hotelId}` | Get hotel info |
| GET | `/{hotelId}/rooms/{roomId}` | Get room info |

### Bookings — `/api/v1/bookings`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/init` | Initialize booking |
| POST | `/{bookingId}/addGuests` | Add guests |
| POST | `/{bookingId}/payments` | Initiate payment |
| POST | `/{bookingId}/cancel` | Cancel & refund |
| GET | `/{bookingId}/status` | Get booking status |

### Users — `/api/v1/users`
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/profile` | Get my profile |
| PATCH | `/profile` | Update profile |
| GET | `/myBookings` | Get my bookings |
| GET | `/guests` | Get my guests |
| POST | `/guests` | Add guest |
| PUT | `/guests/{guestId}` | Update guest |
| DELETE | `/guests/{guestId}` | Delete guest |

---

## Getting Started

### Prerequisites
- Java 21+
- PostgreSQL
- Maven
- Stripe account (for payments)

### Setup

1. **Clone the repo**
```bash
git clone https://github.com/ShubhamPDev7/airbnb-backend.git
cd airbnb-backend
```

2. **Configure properties**

Copy `application-example.properties` to `application.properties` and fill in your values:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/your_db
spring.datasource.username=your_username
spring.datasource.password=your_password

jwt.secretKey=your_jwt_secret_key

stripe.secretKey=your_stripe_secret_key
stripe.webhookSecret=your_stripe_webhook_secret

frontend.url=http://localhost:3000
```

3. **Run the app**
```bash
./mvnw spring-boot:run
```

4. **Access Swagger UI**

Navigate to `http://localhost:8080/api/v1/swagger-ui.html`

---

## Project Structure

```
src/main/java/com/codingshuttle/projects/airBnbApp/
├── advice/          # Global exception handler & response wrapper
├── config/          # ModelMapper, Stripe config
├── controller/      # REST controllers
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities
├── exception/       # Custom exceptions
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter, auth service, security config
├── service/         # Business logic
├── strategy/        # Dynamic pricing strategies
└── util/            # AppUtils (getCurrentUser)
```

---

## Security

- JWT access token in `Authorization: Bearer <token>` header
- Refresh token stored in HTTP-only cookie
- Passwords hashed with BCrypt
- Role-based endpoint protection (`HOTEL_MANAGER` for admin routes)
- Ownership checks on all hotel, booking and guest operations

---

## License

This project is for educational purposes, built while following a Spring Boot course.