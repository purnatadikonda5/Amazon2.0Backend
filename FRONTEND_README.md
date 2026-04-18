# Amazon 2.0 Backend - Exact API Contract 

This document defines the strict, 1:1 mapping of all available Backend API routes, their exact JSON payload requirements, and structural behaviors to assist the React frontend team.

---

## 🛑 Global Requirements (Security)
Most endpoints in the platform are secured. For any endpoint marked with 🔒, you must include the JWT Access Token in the HTTP Headers:
```json
{
  "Authorization": "Bearer eyJhbGciOiY..."
}
```
*Note: Any requests missing a valid Bearer token for locked routes will instantly fail with a `403 Forbidden` or `401 Unauthorized`.*

---

## 1. 🔐 Authentication Subsystem

### A. Check if Email Exists (Fast Validation) 🌍
- **Endpoint**: `GET /api/auth/check-email`
- **Purpose**: Mimics Google's signup flow. Call this to check if an email is taken while the user types.
- **Query Params**: `?email=john@example.com`
- **Response Structure (Boolean)**:
```json
true 
// or
false
```

### B. User Signup 🌍
- **Endpoint**: `POST /api/auth/signup`
- **Request Body**:
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "Password123"
}
```
- **Response (`200 OK`)**: `"User registered successfully"`

### C. User Login 🌍
- **Endpoint**: `POST /api/auth/login`
- **Request Body**:
```json
{
  "email": "jane@example.com",
  "password": "Password123"
}
```
- **Response**:
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "78a23-44bd-uuid-..."
}
```

### D. User Logout 🔒
- **Endpoint**: `POST /api/auth/logout`
- **Purpose**: Blacklists the current token inside the system's global Redis cache preventing it from ever being used again.
- **Request Body**: None. (Just the strict `Authorization` Header).
- **Response**: `"Successfully eradicated Active Session from the central cluster."`

---

## 2. 🖼️ Cloudinary Image Uploads

### A. Upload File via Multipart 🔒
- **Endpoint**: `POST /api/upload/image`
- **Purpose**: Call this to convert a raw image file from the user's hard drive into a live public web URL BEFORE submitting a product.
- **Content-Type**: `multipart/form-data`
- **Form Data Field**: `file` (Must be an actual system image file: jpeg, png, webp. Max 5MB).
- **Response (200 OK)**:
```json
{
  "url": "https://res.cloudinary.com/dct3kibzv/image/upload/v12345/example.jpg"
}
```
*Errors*: 413 Payload Too Large if > 5MB. 400 Bad Request if it isn't an Image.

---

## 3. 🛍️ Products & Marketplace Listings

### A. Get All Available Market Listings 🌍
- **Endpoint**: `GET /api/products`
- **Query Params**: `?page=0&size=10`
- **Response**:
```json
{
  "content": [
    {
      "id": 1,
      "sellerId": 5,
      "price": 1000.0,
      "minAcceptablePrice": null, 
      "quantity": 1,
      "yearsOld": 2,
      "customImageUrls": "[\"https://...\", \"https://...\"]",
      "status": "active",
      "product": {
        "title": "MacBook Pro",
        "description": "Mint condition",
        "category": "Electronics",
        "imageUrl": "https://..."
      }
    }
  ],
  "pageable": { ... },
  "totalElements": 1,
  "totalPages": 1
}
```
*Note*: The frontend shouldn't see `minAcceptablePrice`. The backend deliberately purges this to `null` to protect sellers.

### B. Create a Listing 🔒
- **Endpoint**: `POST /api/products`
- **Request Body**:
```json
{
  "title": "MacBook Pro",
  "description": "Mint condition",
  "price": 1000.00,
  "imageUrl": "https://cloudinary...", 
  "category": "Electronics",
  "quantity": 1,
  "minAcceptablePrice": 850.00,
  "isAvailable": true,
  "yearsOld": 2,
  "customImageUrls": "[\"https://...\"]"
}
```
- **Response**: Same as the `ListingResponseDTO` above.

---

## 4. 📈 User Dashboard Statistics 

### A. Get Customer's Purchase History 🔒
- **Endpoint**: `GET /api/user/orders`
- **Query Params**: `?page=0&size=10`
- **Response**: A Pageable JSON object containing `OrderResponseDTO` objects.

### B. Get Seller's Active Store Items 🔒
- **Endpoint**: `GET /api/user/selling`
- **Query Params**: `?page=0&size=10`
- **Response**: A Pageable JSON object containing `ListingResponseDTO` objects (these WILL expose `minAcceptablePrice` because the caller is the seller themselves).

---

## 5. 🤝 Bargaining / Negotiation Workflows 

### A. Submit an Offer (Buyer) 🔒
- **Endpoint**: `POST /api/bargain/submit`
- **Request Body**:
```json
{
  "listingId": 1,
  "offeredPrice": 850.00,
  "quantity": 1
}
```
- **Response**:
```json
{
  "message": "Offer submitted successfully",
  "data": {
    "id": 25,
    "listingId": 1,
    "buyerId": 5,
    "sellerId": 2,
    "offerPrice": 850.00,
    "counterPrice": null,
    "quantity": 1,
    "status": "pending",
    "createdOrderId": null
  }
}
```

### B. View Received Offers (Seller) 🔒
- **Endpoint**: `GET /api/bargain/seller`
- **Query Params**: `?page=0&size=10`

### C. View My Active Offers (Buyer) 🔒
- **Endpoint**: `GET /api/bargain/buyer`
- **Query Params**: `?page=0&size=10`

### D. Respond to Offer (Seller) 🔒
- **Endpoint**: `POST /api/bargain/update`
- **Request Body**:
```json
{
  "offerId": 25,
  "action": "accept", // OR: reject, counter
  "counterPrice": 900.00 // Required ONLY if action is "counter"
}
```
- **Response (If action is `accept`)**: Watch the `createdOrderId` element perfectly!
```json
{
  "message": "Offer updated by seller",
  "data": {
    "status": "accepted",
    "createdOrderId": 102 
  }
}
```

### E. Respond to Counter Offer (Buyer) 🔒
- **Endpoint**: `POST /api/bargain/buyer/update`
- **Request Body**:
```json
{
  "offerId": 25,
  "action": "accept" // OR: reject, cancel
}
```

---

## 6. 💳 Razorpay Webhook Payments 

### A. Create an Unpaid Secure Check-out Session 🔒
- **Endpoint**: `POST /api/payment/create`
- **Purpose**: Creates an impending Order inside Razorpay while freezing race-conditions via idempotency.
- **Request Body**:
```json
{
  "listingId": 1,
  "buyerId": 5,
  "amount": 850.00,
  "idempotencyKey": "pay_xyz123abc_999",
  "quantity": 1
}
```
- **Response**:
```json
{
  "razorpayOrderId": "order_Fk...9Xp",
  "status": "CREATED",
  "currency": "INR",
  "amount": 850.00
}
```
*Frontend Action:* Pass `razorpayOrderId` into the Razorpay SDK checkout pop-up on the client side!

### B. Verify Successful Purchase Webhook from Gateway 🔒
- **Endpoint**: `POST /api/payment/verify`
- **Purpose**: Hits the server to cryptographically unlock and resolve the transaction post-checkout!
- **Request Body**:
```json
{
  "razorpayOrderId": "order_Fk...9Xp",
  "razorpayPaymentId": "pay_Fk...2Lp",
  "razorpaySignature": "3xY4h8...8XmB"
}
```
- **Response (200 OK)**:
```json
{
  "razorpayOrderId": "order_Fk...9Xp",
  "status": "SUCCESS"
}
```
