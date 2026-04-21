import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShoppingBag, User, LogIn, LogOut, Plus, Menu, X } from 'lucide-react';
import { logoutUser } from '../services/api';
import './Navbar.css';

export default function Navbar() {
  const [user, setUser] = useState(null);
  const [menuOpen, setMenuOpen] = useState(false);

  // Check auth state on mount and listen for storage changes
  useEffect(() => {
    const checkAuth = () => {
      const stored = localStorage.getItem('user');
      setUser(stored ? JSON.parse(stored) : null);
    };
    checkAuth();
    window.addEventListener('storage', checkAuth);
    // Custom event for same-tab auth updates
    window.addEventListener('auth-change', checkAuth);
    return () => {
      window.removeEventListener('storage', checkAuth);
      window.removeEventListener('auth-change', checkAuth);
    };
  }, []);

  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await logoutUser();
    } catch (e) {
      // Even if server call fails, clear local state
      console.warn('Logout API failed, clearing local session', e);
    }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
    window.dispatchEvent(new Event('auth-change'));
    setMenuOpen(false);
    navigate('/login');
  };

  const closeMenu = () => setMenuOpen(false);

  return (
    <nav className="navbar">
      <div className="nav-brand">
        <Link to="/" onClick={closeMenu}>
          <ShoppingBag className="icon" />
          <span>BargainHub</span>
        </Link>
      </div>

      <button
        className="nav-mobile-toggle"
        onClick={() => setMenuOpen(!menuOpen)}
        aria-label="Toggle menu"
      >
        {menuOpen ? <X className="icon" /> : <Menu className="icon" />}
      </button>

      <div className={`nav-links ${menuOpen ? 'open' : ''}`}>
        <Link to="/" onClick={closeMenu}>Home</Link>

        {user ? (
          <>
            <Link to="/sell" className="nav-sell-btn" onClick={closeMenu}>
              <Plus className="icon-sm" /> Sell Item
            </Link>
            <Link to="/profile" className="nav-profile-btn" onClick={closeMenu}>
              <User className="icon-sm" /> Dashboard
            </Link>
            <div className="nav-user-chip">
              <div className="nav-user-avatar">
                {(user.name || user.email || 'U').charAt(0).toUpperCase()}
              </div>
              <span className="nav-user-name">{user.name || 'User'}</span>
            </div>
            <button className="nav-logout-btn" onClick={handleLogout} title="Logout">
              <LogOut className="icon-sm" />
            </button>
          </>
        ) : (
          <>
            <Link to="/sell" className="nav-sell-btn" onClick={closeMenu}>
              <Plus className="icon-sm" /> Sell Item
            </Link>
            <Link to="/login" className="nav-auth-btn" onClick={closeMenu}>
              <LogIn className="icon-sm" /> Login
            </Link>
          </>
        )}
      </div>
    </nav>
  );
}
