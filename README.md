<div align="center">

# 🏡 Airbnb Clone — Backend

**A full-featured hotel booking backend inspired by Airbnb — built with Spring Boot, Stripe payments, dynamic pricing, and async email notifications.**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-JPA-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Stripe](https://img.shields.io/badge/Stripe-Payments-635BFF?style=for-the-badge&logo=stripe&logoColor=white)](https://stripe.com/)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://swagger.io/)

*Built to explore real-world backend patterns — pessimistic locking, decorator-based pricing, webhook flows, and rate limiting.*

</div>

---

## 📐 Architecture Overview

```
  Client
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│              Spring Boot Application  :8080              │
│                                                          │
│  ┌──────────────┐   ┌──────────────┐  ┌──────────────┐  │
│  │    Auth      │   │  Controllers │  │  Scheduling  │  │
│  │  JWT Filter  │──►│  REST APIs   │  │  Expiry Job  │  │
│  └──────────────┘   └──────┬───────┘  └──────────────┘  │
│                            │                             │
│          ┌─────────────────┼─────────────────┐          │
│          ▼                 ▼                 ▼          │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│   │   Service   │  │  Pricing    │  │    Email     │    │
│   │   Layer     │  │  Strategies │  │   (Async)    │    │
│   └──────┬──────┘  └─────────────┘  └─────────────┘    │
│          │                                              │
│   ┌──────▼──────┐   ┌─────────────┐  ┌─────────────┐   │
│   │    JPA      │   │   Stripe    │  │  Bucket4j   │   │
│   │ Repositories│   │  Webhooks   │  │ Rate Limit  │   │
│   └──────┬──────┘   └─────────────┘  └─────────────┘   │
└──────────┼──────────────────────────────────────────────┘
           │
      ┌────▼────┐
      │  PG DB  │
      └─────────┘
```

---

## ✨ Features

### 🔐 Authentication
- Signup / Login with **JWT** access tokens
- Refresh token via **HTTP-only cookie**
- Role-based access control: `GUEST` and `HOTEL_MANAGER`

### 🏨 Hotel Management *(Hotel Manager)*
- Full CRUD for hotels and rooms
- Activate hotels to auto-initialize **1 year of inventory** per room
- View all bookings and generate **revenue reports** with date range filtering

### 📦 Inventory Management *(Hotel Manager)*
- Update surge factor and open/close rooms by date range
- **Pessimistic locking** prevents race conditions during concurrent bookings

### 🛒 Booking Flow *(Guest)*

```
1. Init Booking      ──► reserves inventory (pessimistic lock) + calculates price
2. Add Guests        ──► attach guest profiles to the booking
3. Initiate Payment  ──► creates Stripe Checkout session
4. Stripe Webhook    ──► confirms payment → booking marked CONFIRMED
5. Cancel (optional) ──► auto-refund via Stripe + inventory released
```

### ⏱️ Booking Expiry
- Scheduled job runs **every 10 minutes**
- Automatically expires `RESERVED` bookings older than 10 minutes
- Releases locked inventory back to availability

### 💰 Dynamic Pricing *(Decorator Pattern)*

Pricing strategies applied in chain:

| Strategy | Rule |
|---|---|
| **Base** | Room base price × surge factor |
| **Surge** | Custom multiplier per inventory record |
| **Occupancy** | +20% if occupancy > 80% |
| **Urgency** | Higher price as check-in date approaches |
| **Holiday** | Premium on public holidays |

### 📧 Email Notifications *(Async)*
- Booking **confirmation** email — hotel name, dates, rooms, amount
- Booking **cancellation** email — refund details
- HTML formatted; sent asynchronously so booking flow is never blocked

### 🚦 Rate Limiting
- Per-IP limiting on the public hotel **search** endpoint
- **20 requests/minute** via Bucket4j token bucket algorithm
- Returns `429 Too Many Requests` when exceeded

### 👥 Guest & Profile Management
- Add, update, delete reusable guest profiles
- View and update personal profile (name, gender, date of birth)
- View all personal bookings

---

## 📡 API Reference

> Full interactive docs at **`http://localhost:8080/api/v1/swagger-ui.html`**

### 🔐 Auth — `/api/v1/auth`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/signup` | Register a new user |
| `POST` | `/login` | Login and receive JWT |
| `POST` | `/refresh` | Refresh access token via cookie |

### 🏨 Admin — Hotels `/api/v1/admin/hotels`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/` | Create a hotel |
| `GET` | `/` | List all hotels |
| `GET` | `/{hotelId}` | Get hotel by ID |
| `PUT` | `/{hotelId}` | Update hotel |
| `DELETE` | `/{hotelId}` | Delete hotel |
| `PATCH` | `/{hotelId}/activate` | Activate hotel + init inventory |
| `GET` | `/{hotelId}/bookings` | View all bookings |
| `GET` | `/{hotelId}/reports` | Revenue report (date range) |

### 🛏️ Admin — Rooms `/api/v1/admin/hotels/{hotelId}/rooms`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/` | Add a room |
| `GET` | `/` | List all rooms |
| `GET` | `/{roomId}` | Get room by ID |
| `PUT` | `/{roomId}` | Update room |
| `DELETE` | `/{roomId}` | Delete room |

### 📦 Admin — Inventory `/api/v1/admin/inventory`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/rooms/{roomId}` | Get inventory by room |
| `PATCH` | `/rooms/{roomId}` | Update surge factor / open-close |

### 🔍 Browse *(Public)* — `/api/v1/hotels`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/search` | Search hotels by city & dates *(rate limited)* |
| `GET` | `/{hotelId}/info` | Get hotel details |

### 🛒 Bookings — `/api/v1/bookings`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/init` | Initialize a booking |
| `POST` | `/{bookingId}/addGuests` | Attach guests to booking |
| `POST` | `/{bookingId}/payments` | Create Stripe checkout session |
| `POST` | `/{bookingId}/cancel` | Cancel booking + auto-refund |
| `GET` | `/{bookingId}/status` | Get booking status |

### 👤 Users — `/api/v1/users`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/profile` | Get my profile |
| `PATCH` | `/profile` | Update profile |
| `GET` | `/myBookings` | View all my bookings |
| `GET` | `/guests` | List my guest profiles |
| `POST` | `/guests` | Add a guest |
| `PUT` | `/guests/{guestId}` | Update a guest |
| `DELETE` | `/guests/{guestId}` | Delete a guest |

---

## 💻 Tech Stack

| Concern | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 |
| Security | Spring Security — JWT + BCrypt + HTTP-only cookies |
| Database | PostgreSQL — Spring Data JPA / Hibernate |
| Payments | Stripe — Checkout sessions, webhooks, refunds |
| Rate Limiting | Bucket4j — token bucket algorithm |
| Email | Spring Mail — async HTML notifications |
| API Docs | Springdoc OpenAPI — Swagger UI |
| Object Mapping | ModelMapper 3.2.0 |
| Build | Apache Maven |
| Utilities | Lombok |

---

## 📁 Project Structure

```
src/main/java/.../airBnbApp/
├── advice/       ← Global exception handler & response wrapper
├── config/       ← ModelMapper, Stripe, rate limiting config
├── controller/   ← REST controllers
├── dto/          ← Request / Response DTOs
├── entity/       ← JPA entities
├── exception/    ← Custom exception classes
├── repository/   ← Spring Data JPA repositories
├── security/     ← JWT filter, auth service, security config
├── service/      ← Business logic + email + booking expiry job
├── strategy/     ← Dynamic pricing decorator chain
└── util/         ← AppUtils (getCurrentUser, etc.)
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- PostgreSQL
- Maven
- Stripe account
- Gmail account (with App Password)

### Setup

**1. Clone the repo**
```bash
git clone https://github.com/ShubhamPDev7/airbnb-backend.git
cd airbnb-backend
```

**2. Configure properties**

Copy the example file and fill in your values:
```bash
cp src/main/resources/application-example.properties \
   src/main/resources/application.properties
```

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/your_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT
jwt.secretKey=your_jwt_secret_key

# Stripe
stripe.secretKey=your_stripe_secret_key
stripe.webhookSecret=your_stripe_webhook_secret

# Frontend (for Stripe redirect URLs)
frontend.url=http://localhost:3000

# Gmail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```

> **Gmail tip:** Use an App Password, not your regular password. Enable 2FA on your Google account, then generate one at [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).

**3. Run the app**
```bash
./mvnw spring-boot:run
```

**4. Open Swagger UI**
```
http://localhost:8080/api/v1/swagger-ui.html
```

---

## 🔒 Security Model

| Mechanism | Detail |
|---|---|
| Access Token | JWT in `Authorization: Bearer <token>` header |
| Refresh Token | Stored in HTTP-only cookie |
| Passwords | Hashed with BCrypt |
| Role Protection | `HOTEL_MANAGER` required for all `/admin/**` routes |
| Ownership Checks | Enforced on hotel, booking, and guest operations |
| Rate Limiting | 20 req/min per IP on public search endpoint |

---

<div align="center">

Built by [ShubhamPDev7](https://github.com/ShubhamPDev7) for educational purposes, as part of a Spring Boot backend engineering deep-dive.

</div>