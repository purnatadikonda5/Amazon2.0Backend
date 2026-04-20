import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import ProductDetails from './pages/ProductDetails';
import Profile from './pages/Profile';
import SellItem from './pages/SellItem';
import Payment from './pages/Payment';
import PaymentStatus from './pages/PaymentStatus';

function App() {
  return (
    <BrowserRouter>
      <div className="app-container">
        <Navbar />
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/product/:id" element={<ProductDetails />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/sell" element={<SellItem />} />
            <Route path="/payment" element={<Payment />} />
            <Route path="/payment/status" element={<PaymentStatus />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
