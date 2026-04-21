import axios from 'axios';

// ==========================================
// Axios Instance — Centralized API Client
// ==========================================
const API = axios.create({
    baseURL: '/api',
});

// Request Interceptor: Attach JWT token to every request
API.interceptors.request.use((req) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        req.headers.Authorization = `Bearer ${token}`;
    }
    return req;
});

// Response Interceptor: Auto-logout on 401/403
API.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            console.warn("Session expired. Logging out...");
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            window.dispatchEvent(new Event('auth-change'));
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// ==========================================
// 1. AUTHENTICATION
// ==========================================

export const checkEmailExists = async (email) => {
    const response = await API.get(`/auth/check-email?email=${email}`);
    return response.data;
};

export const signupUser = async (userData) => {
    const response = await API.post('/auth/signup', userData);
    return response.data;
};

export const loginUser = async (credentials) => {
    const response = await API.post('/auth/login', credentials);
    if (response.data.accessToken) {
        localStorage.setItem('accessToken', response.data.accessToken);
        localStorage.setItem('refreshToken', response.data.refreshToken);
    }
    return response.data;
};

export const logoutUser = async () => {
    const response = await API.post('/auth/logout');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    return response.data;
};

// ==========================================
// 2. IMAGE UPLOAD (Cloudinary)
// ==========================================

export const uploadImage = async (imageFile) => {
    const formData = new FormData();
    formData.append('file', imageFile);
    const response = await API.post('/upload/image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data.url;
};

// ==========================================
// 3. PRODUCTS & LISTINGS
// ==========================================

export const fetchAvailableListings = async (page = 0, size = 10) => {
    const response = await API.get(`/products?page=${page}&size=${size}`);
    return response.data;
};

export const fetchListingById = async (id) => {
    const response = await API.get(`/products/${id}`);
    return response.data;
};

export const createListing = async (listingData) => {
    const response = await API.post('/products', listingData);
    return response.data;
};

/** Fuzzy search — uses Jaro-Winkler distance on the backend */
export const searchProducts = async (query, page = 0, size = 10) => {
    const response = await API.get(`/products/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`);
    return response.data;
};

// ==========================================
// 4. USER DATA (Orders, Selling, Wallet)
// ==========================================

export const fetchUserOrders = async (page = 0, size = 10) => {
    const response = await API.get(`/user/orders?page=${page}&size=${size}`);
    return response.data;
};

export const fetchUserSellingListings = async (page = 0, size = 10) => {
    const response = await API.get(`/user/selling?page=${page}&size=${size}`);
    return response.data;
};

export const fetchWalletBalance = async () => {
    const response = await API.get('/user/wallet');
    return response.data;
};

export const withdrawWalletToBank = async () => {
    const response = await API.post('/user/wallet/withdraw');
    return response.data;
};

// ==========================================
// 5. BARGAINING / OFFERS
// ==========================================

export const submitBuyerOffer = async (offerData) => {
    const response = await API.post('/bargain/submit', offerData);
    return response.data;
};

export const fetchSellerOffers = async (page = 0, size = 10) => {
    const response = await API.get(`/bargain/seller?page=${page}&size=${size}`);
    return response.data;
};

export const fetchBuyerOffers = async (page = 0, size = 10) => {
    const response = await API.get(`/bargain/buyer?page=${page}&size=${size}`);
    return response.data;
};

export const sellerUpdateOffer = async (updateData) => {
    const response = await API.post('/bargain/update', updateData);
    return response.data;
};

export const buyerUpdateOffer = async (updateData) => {
    const response = await API.post('/bargain/buyer/update', updateData);
    return response.data;
};

// ==========================================
// 6. RAZORPAY PAYMENTS
// ==========================================

export const createPaymentSession = async (paymentData) => {
    const response = await API.post('/payment/create', paymentData);
    return response.data;
};

export const verifyPaymentWebhook = async (verificationData) => {
    const response = await API.post('/payment/verify', verificationData);
    return response.data;
};
