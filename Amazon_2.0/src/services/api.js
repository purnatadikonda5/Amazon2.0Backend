import axios from 'axios';

// Create a globally configured Axios instance
const API = axios.create({
    baseURL: '/api', 
});

API.interceptors.request.use((req) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        req.headers.Authorization = `Bearer ${token}`;
    }
    return req;
});

// Interceptor: Automatically catch Expired or Orphaned Users (401/403) and forcefully logout
API.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            console.warn("Security Event: Invalid/Expired Session. Auto-Logging out...");
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// ==========================================
// 1. AUTHENTICATION & USERS
// ==========================================

export const checkEmailExists = async (email) => {
    // Fast O(1) Redis execution
    const response = await API.get(`/auth/check-email?email=${email}`);
    return response.data; // true or false
};

export const signupUser = async (userData) => {
    // userData = { name, email, password }
    const response = await API.post('/auth/signup', userData);
    return response.data;
};

export const loginUser = async (credentials) => {
    // credentials = { email, password }
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

export const fetchUserOrders = async (page = 0, size = 10) => {
    const response = await API.get(`/user/orders?page=${page}&size=${size}`);
    return response.data;
};

export const fetchUserSellingListings = async (page = 0, size = 10) => {
    const response = await API.get(`/user/selling?page=${page}&size=${size}`);
    return response.data;
};

// Wallet Operations
export const fetchWalletBalance = async () => {
    const response = await API.get('/user/wallet');
    return response.data;
};

export const withdrawWalletToBank = async () => {
    const response = await API.post('/user/wallet/withdraw');
    return response.data;
};

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

// ==========================================
// 4. BARGAINING / NEGOTIATION SUBSYSTEM
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
// 5. RAZORPAY PAYMENT GATEWAY 
// ==========================================

export const createPaymentSession = async (paymentData) => {
    const response = await API.post('/payment/create', paymentData);
    return response.data; 
};

export const verifyPaymentWebhook = async (verificationData) => {
    const response = await API.post('/payment/verify', verificationData);
    return response.data;
};
