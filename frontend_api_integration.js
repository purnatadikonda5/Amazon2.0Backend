/**
 * API Integration Service for Amazon 2.0 Frontend
 * 
 * Instructions:
 * 1. Install Axios: `npm install axios`
 * 2. Save this file as `src/services/api.js` in your React project.
 * 3. Use these pre-configured functions inside your React components!
 */

import axios from 'axios';

// Create a globally configured Axios instance
const API = axios.create({
    baseURL: 'http://localhost:8080/api', // Adjust base URL if deploying to Render/Heroku
});

// Interceptor: Automatically attach the JWT Bearer Token to ALL secure requests!
API.interceptors.request.use((req) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        req.headers.Authorization = `Bearer ${token}`;
    }
    return req;
});

// ==========================================
// 1. AUTHENTICATION & USERS
// ==========================================

export const checkEmailExists = async (email) => {
    // Fast O(1) Redis execution
    const response = await API.get(`/auth/check-email?email=${email}`);
    return response.data; // true or false
};

export const signup = async (userData) => {
    // userData = { name, email, password }
    const response = await API.post('/auth/signup', userData);
    return response.data;
};

export const login = async (credentials) => {
    // credentials = { email, password }
    const response = await API.post('/auth/login', credentials);
    if (response.data.accessToken) {
        localStorage.setItem('accessToken', response.data.accessToken);
        localStorage.setItem('refreshToken', response.data.refreshToken);
    }
    return response.data;
};

export const logout = async () => {
    const response = await API.post('/auth/logout');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    return response.data;
};

// ==========================================
// 2. CLOUDINARY IMAGE UPLOAD (The "Multer" Alternative)
// ==========================================

export const uploadImage = async (imageFile) => {
    const formData = new FormData();
    formData.append('file', imageFile);

    const response = await API.post('/upload/image', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data.url; // Returns the secure Cloudinary string URL!
};

// ==========================================
// 3. PRODUCTS & LISTINGS
// ==========================================

export const fetchAvailableListings = async (page = 0, size = 10) => {
    // Unauthenticated global store fetch
    const response = await API.get(`/products?page=${page}&size=${size}`);
    return response.data; 
};

export const createListing = async (listingData) => {
    // Call uploadImage() first, get the URL, and attach to this payload!
    // listingData = { title, description, price, imageUrl, category, quantity, minAcceptablePrice, yearsOld, customImageUrls }
    const response = await API.post('/products', listingData);
    return response.data;
};

// ==========================================
// 4. BARGAINING / NEGOTIATION SUBSYSTEM
// ==========================================

export const submitBuyerOffer = async (offerData) => {
    // offerData = { listingId, offeredPrice, quantity }
    const response = await API.post('/bargain/submit', offerData);
    return response.data; 
};

// Fetch notifications/offers directed at the logged in Seller
export const fetchSellerOffers = async (page = 0, size = 10) => {
    const response = await API.get(`/bargain/seller?page=${page}&size=${size}`);
    return response.data; 
};

// Fetch active offers the logged-in Buyer has submitted
export const fetchBuyerOffers = async (page = 0, size = 10) => {
    const response = await API.get(`/bargain/buyer?page=${page}&size=${size}`);
    return response.data; 
};

// Respond to an offer natively
export const sellerUpdateOffer = async (updateData) => {
    // updateData = { offerId, action: "accept"|"reject"|"counter", counterPrice }
    const response = await API.post('/bargain/update', updateData);
    return response.data;
};

// ==========================================
// 5. RAZORPAY PAYMENT GATEWAY 
// ==========================================

export const createPaymentSession = async (paymentData) => {
    // paymentData = { listingId, buyerId, amount, idempotencyKey, quantity }
    const response = await API.post('/payment/create', paymentData);
    return response.data; // You will receive { razorpayOrderId: "..." }
};

export const verifyPaymentWebhook = async (verificationData) => {
    // Call this inside the Razorpay UI handler() success callback!
    // verificationData = { razorpayOrderId, razorpayPaymentId, razorpaySignature }
    const response = await API.post('/payment/verify', verificationData);
    return response.data; // Should return { status: "SUCCESS" }
};
