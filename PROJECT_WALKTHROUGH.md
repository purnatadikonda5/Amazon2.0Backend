# BargainHub — Project Walkthrough

> A full-stack e-commerce platform with **price bargaining** built using React + Spring Boot.

---

## 📋 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Feature Breakdown](#feature-breakdown)
4. [API Reference](#api-reference)
5. [Data Flow Diagrams](#data-flow-diagrams)
6. [Folder Structure](#folder-structure)
7. [How to Run](#how-to-run)
8. [Interview Explanation Guide](#interview-explanation-guide)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    FRONTEND (React/Vite)                 │
│                    Port: 5173                            │
│                                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │   Home   │ │ Profile  │ │  Sell    │ │ Payment  │   │
│  │ (Search) │ │(Orders/  │ │ Item    │ │(Razorpay)│   │
│  │          │ │ Offers/  │ │         │ │          │   │
│  │          │ │ Selling) │ │         │ │          │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│         │           │             │           │         │
│         └───────────┴─────────────┴───────────┘         │
│                         │                               │
│              ┌──────────┴──────────┐                    │
│              │   api.js (Axios)    │                    │
│              │  JWT auto-attached  │                    │
│              └──────────┬──────────┘                    │
│                         │ HTTP + WebSocket              │
└─────────────────────────┼───────────────────────────────┘
                          │
            Vite Proxy (/api → :8080)
                          │
┌─────────────────────────┼───────────────────────────────┐
│                    BACKEND (Spring Boot)                 │
│                    Port: 8080                            │
│                                                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │          Security Layer (JWT + Rate Limit)       │    │
│  │  JwtAuthFilter → RateLimitingFilter → Controller │    │
│  └─────────────────────────────────┬───────────────┘    │
│                                    │                     │
│  ┌──────────┐ ┌──────────┐ ┌──────┴────┐ ┌──────────┐  │
│  │  Auth    │ │ Product  │ │  Offer    │ │ Payment  │  │
│  │Controller│ │Controller│ │Controller │ │Controller│  │
│  └────┬─────┘ └────┬─────┘ └────┬──────┘ └────┬─────┘  │
│       │             │            │              │        │
│  ┌────┴─────────────┴────────────┴──────────────┴───┐   │
│  │              Service Layer                        │   │
│  │  AuthServices, ProductServices, OfferServices,    │   │
│  │  PaymentService, OrderServices, UserServices      │   │
│  └───────────────────────┬───────────────────────────┘   │
│                          │                               │
│  ┌───────────────────────┴───────────────────────────┐   │
│  │              Repository Layer (JPA)                │   │
│  │  UserRepo, ProductRepo, ListingRepo, OfferRepo,   │   │
│  │  OrderRepo, PaymentTransactionRepo                 │   │
│  └───────────────────────┬───────────────────────────┘   │
│                          │                               │
│  ┌──────────┐  ┌─────────┴──┐  ┌──────────────┐         │
│  │  MySQL   │  │   Redis    │  │  Cloudinary  │         │
│  │  (Data)  │  │  (Cache/   │  │  (Images)    │         │
│  │          │  │  Sessions) │  │              │         │
│  └──────────┘  └────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Why This Choice |
|-------|-----------|----------------|
| **Frontend** | React 19 + Vite | Fast HMR, modern JSX, lightweight bundler |
| **Styling** | Vanilla CSS + CSS Variables | Full control, dark mode via `prefers-color-scheme` |
| **HTTP Client** | Axios | Interceptors for JWT auto-attach & auto-logout |
| **Real-time** | STOMP.js + SockJS | WebSocket for live bargaining updates |
| **Icons** | Lucide React | Tree-shakeable, consistent icon library |
| **Backend** | Spring Boot 3 | Enterprise-grade Java framework |
| **Security** | Spring Security + JWT | Stateless authentication, no server sessions |
| **Database** | MySQL + Spring Data JPA | Relational data, automatic query generation |
| **Cache** | Redis + Spring Cache | Sub-millisecond reads for frequent data |
| **Rate Limit** | Bucket4j | Prevents brute-force on auth endpoints |
| **Payments** | Razorpay | Indian payment gateway, UPI/cards/netbanking |
| **Image CDN** | Cloudinary | Auto-optimization (WebP, resizing, compression) |
| **WebSocket** | Spring WebSocket (STOMP) | Real-time offer notifications |
| **Search** | Jaro-Winkler (Apache Commons) | Fuzzy search tolerating typos |

---

## Feature Breakdown

### 1. Authentication (JWT)
- **Signup**: Creates user → hashes password (BCrypt) → stores in MySQL.
- **Login**: Validates credentials → generates JWT access token + refresh token.
- **Auto-logout**: Axios interceptor catches 401/403 → clears tokens → redirects to `/login`.
- **Email check**: Redis-backed O(1) check if email is already taken during signup.
- **Token refresh**: `/api/auth/refresh` endpoint for extending sessions.

### 2. Product Marketplace
- **Listing model**: Products have a `Listing` wrapper that adds seller-specific info (price, quantity, min acceptable price, age).
- **Pagination**: Backend returns Spring `Page<T>` objects. Frontend uses infinite scroll.
- **Fuzzy search**: Uses Jaro-Winkler string distance algorithm. "iPhon 14" matches "iPhone 14" at ~95% similarity.
- **Cloudinary upload**: Frontend sends `FormData` → backend validates MIME type + size → uploads to Cloudinary → returns URL.

### 3. Bargaining System (Core Feature)
This is what makes the project unique:

```
Buyer sees product → Makes an offer → Seller receives it
    ↓ Seller can: Accept / Reject / Counter-offer
        ↓ If countered → Buyer can: Accept / Reject
            ↓ If accepted → Buyer proceeds to payment
```

- **Security**: Offers use server-side user ID extraction from JWT (prevents BOLA attacks).
- **Real-time**: WebSocket push notifications so buyers/sellers see updates instantly without refreshing.
- **WebSocket topics**: `/topic/buyer/{userId}` and `/topic/seller/{userId}`.

### 4. Payments (Razorpay)
- **Flow**: Create session → Razorpay popup → User pays → Verify signature → Create order.
- **Idempotency**: Each payment has a unique `idempotencyKey` preventing duplicate charges.
- **Verification**: Backend verifies Razorpay signature cryptographically before creating the order.
- **Post-payment**: Order created + listing inventory decremented + seller wallet credited.

### 5. Profile Dashboard
Three tabs with real data:
- **My Orders**: Shows completed purchases with order ID, product, price, date, status.
- **My Offers**: Shows offers you've made as a buyer (pending/countered/accepted/rejected).
- **My Listings**: Shows items you're selling + incoming offers from buyers with accept/reject/counter controls.
- **Wallet**: Shows escrow balance from sales with withdrawal capability.

### 6. Protected Routes
- Frontend checks `localStorage.accessToken` before allowing access to `/profile`, `/sell`, `/payment`.
- Redirects to `/login` if not authenticated.

---

## API Reference

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/signup` | Register new user |
| `POST` | `/api/auth/login` | Login, returns JWT tokens |
| `POST` | `/api/auth/logout` | Invalidate session |
| `POST` | `/api/auth/refresh` | Refresh access token |
| `GET` | `/api/auth/check-email?email=x` | Check if email is taken |

### Products
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/products?page=0&size=10` | Paginated listings |
| `GET` | `/api/products/{id}` | Single listing details |
| `GET` | `/api/products/search?q=keyword` | Fuzzy search |
| `POST` | `/api/products` | Create new listing (auth required) |

### Bargaining
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/bargain/submit` | Submit buyer offer |
| `GET` | `/api/bargain/buyer` | Get buyer's offers |
| `GET` | `/api/bargain/seller` | Get seller's offers |
| `POST` | `/api/bargain/update` | Seller responds to offer |
| `POST` | `/api/bargain/buyer/update` | Buyer responds to counter |

### Payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/payment/create` | Create Razorpay session |
| `POST` | `/api/payment/verify` | Verify payment signature |
| `GET` | `/api/payment/status?order_id=x` | Check payment status |

### User
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/user/orders` | User's purchase history |
| `GET` | `/api/user/selling` | User's active listings |
| `GET` | `/api/user/wallet` | Wallet balance |
| `POST` | `/api/user/wallet/withdraw` | Withdraw to bank |

### Upload
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/upload/image` | Upload image to Cloudinary |

---

## Data Flow Diagrams

### User Registration & Login
```
User fills form → POST /api/auth/signup → BCrypt hash → Save to MySQL
                                                        ↓
User logs in → POST /api/auth/login → Verify password → Generate JWT
                                                        ↓
                                        Store accessToken + refreshToken
                                        in localStorage
                                                        ↓
                                        Navbar detects auth-change event
                                        → Shows user avatar + logout
```

### Buying Flow
```
Home page → Browse/Search products → Click "Buy Now"
    ↓
Payment page → POST /api/payment/create → Razorpay order created
    ↓
Razorpay popup opens → User completes payment
    ↓
POST /api/payment/verify → Signature verified → Order created
    ↓
Seller wallet credited → Listing quantity decremented
    ↓
Redirect to /payment/status?success=true
```

### Bargaining Flow
```
Buyer clicks "Bargain" → Product detail page → Enter offer price
    ↓
POST /api/bargain/submit → Offer saved → WebSocket push to seller
    ↓
Seller sees offer in "My Listings" tab → Accept / Reject / Counter
    ↓
POST /api/bargain/update → Updated offer → WebSocket push to buyer
    ↓
Buyer sees counter in "My Offers" tab → Accept / Reject
    ↓
If accepted → Buyer proceeds to payment with agreed price
```

---

## Folder Structure

### Frontend (`Amazon_2.0/src/`)
```
src/
├── main.jsx              # Entry point, renders App
├── index.css             # Global design system (CSS variables, components)
├── App.jsx               # Routes + ProtectedRoute wrapper
├── components/
│   ├── Navbar.jsx        # Auth-aware navigation bar
│   └── Navbar.css        # Glassmorphism navbar styles
├── pages/
│   ├── Home.jsx          # Product listing + fuzzy search
│   ├── Home.css          # Hero, search bar, product grid styles
│   ├── Login.jsx         # JWT login with inline errors
│   ├── Signup.jsx        # Registration with email availability check
│   ├── ProductDetails.jsx # Product view + bargain form
│   ├── Profile.jsx       # Dashboard (orders/offers/selling/wallet)
│   ├── SellItem.jsx      # Create listing with image upload
│   ├── Payment.jsx       # Razorpay checkout
│   └── PaymentStatus.jsx # Success/failure result page
└── services/
    └── api.js            # Axios instance, all API functions
```

### Backend (`src/main/java/com/purna/`)
```
com.purna/
├── Amazon2dotOBackendApplication.java   # Main Spring Boot entry
├── config/
│   ├── WebSecurityConfig.java           # JWT filter chain, CORS
│   ├── WebSocketConfig.java             # STOMP broker setup
│   └── SwaggerConfig.java              # API documentation
├── controller/
│   ├── AuthController.java             # Login, signup, refresh, logout
│   ├── ProductController.java          # CRUD + search
│   ├── OfferController.java            # Bargaining endpoints
│   ├── PaymentController.java          # Razorpay create/verify
│   ├── OrderController.java            # Order lookup
│   ├── UserController.java             # User orders/selling/wallet
│   ├── ImageUploadController.java      # Cloudinary upload
│   └── ChatController.java            # WebSocket messaging
├── dto/                                # Request/Response objects
├── model/                              # JPA entities
├── repository/                         # Spring Data JPA interfaces
├── service/                            # Business logic layer
├── security/filter/                    # JWT + Rate limiting filters
├── exception/                          # Global error handling
├── event/                              # Payment event listeners
└── utils/                              # Auth utilities
```

---

## How to Run

### Prerequisites
- Java 17+
- MySQL running on port 3306
- Redis running on port 6379
- Node.js 18+

### Backend
```bash
# From project root
./mvnw spring-boot:run
# Runs on http://localhost:8080
```

### Frontend
```bash
cd Amazon_2.0
npm install
npm run dev
# Runs on http://localhost:5173
# Vite automatically proxies /api requests to :8080
```

---

## Interview Explanation Guide

### 30-Second Elevator Pitch
> "BargainHub is a full-stack e-commerce platform where buyers can negotiate prices with sellers in real-time. Built with React and Spring Boot, it features JWT authentication, Razorpay payment integration, WebSocket-based live bargaining, fuzzy search with Jaro-Winkler algorithm, Cloudinary image uploads, and Redis caching — mimicking a production-grade marketplace."

### Key Technical Talking Points

1. **"Why bargaining?"** — Most e-commerce platforms have fixed prices. This adds a unique negotiation layer that demonstrates complex state management (offer → counter → accept/reject lifecycle).

2. **"How does authentication work?"** — Stateless JWT with access + refresh tokens. The frontend Axios interceptor auto-attaches tokens and auto-logs out on 401. Rate limiting prevents brute-force.

3. **"How do you prevent security issues?"** — Server-side user ID extraction from JWT (prevents BOLA attacks). Razorpay signature verification prevents payment tampering. MIME type validation on uploads. Input validation with `@Valid`.

4. **"Why WebSockets?"** — Traditional polling would hit the DB every second. WebSocket STOMP protocol gives instant push updates when offers change — zero-latency UI updates.

5. **"What's fuzzy search?"** — Jaro-Winkler distance algorithm calculates string similarity. "iPhon" matches "iPhone" at 95%. This tolerates user typos unlike SQL `LIKE`.

6. **"How does caching work?"** — `@Cacheable` stores frequently-read data (product listings) in memory/Redis. First user gets a 100ms DB hit; next 50,000 users get a 1ms cache hit.

7. **"How does payment work?"** — Frontend creates a Razorpay order → user pays in popup → frontend sends signature to backend → backend cryptographically verifies → creates order + credits seller wallet atomically.

### Questions You Might Get Asked
- **Q: What happens if payment succeeds but verification fails?** A: The Razorpay order ID is tracked. The payment can be manually reconciled using the `/api/payment/status` endpoint.
- **Q: Can a buyer bargain on their own product?** A: The backend checks `buyerId != sellerId` and throws `UnauthorizedBargainException`.
- **Q: How do you handle concurrent offers?** A: Database-level transactions with `@Transactional` ensure atomic state changes.
