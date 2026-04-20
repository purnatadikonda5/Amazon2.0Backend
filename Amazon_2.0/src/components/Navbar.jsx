import { Link } from 'react-router-dom';
import { ShoppingBag, User, LogIn } from 'lucide-react';
import './Navbar.css';

export default function Navbar() {
  return (
    <nav className="navbar">
      <div className="nav-brand">
        <Link to="/">
          <ShoppingBag className="icon" />
          <span>BargainHub</span>
        </Link>
      </div>
      <div className="nav-links">
        <Link to="/">Home</Link>
        <Link to="/sell" className="nav-btn-highlight">Sell Item</Link>
        <Link to="/profile" className="nav-btn-soft"><User className="icon-sm" /> Profile</Link>
        <Link to="/login" className="nav-btn"><LogIn className="icon-sm" /> Login</Link>
      </div>
    </nav>
  );
}
