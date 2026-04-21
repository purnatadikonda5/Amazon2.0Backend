import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, X } from 'lucide-react';
import { fetchAvailableListings, searchProducts } from '../services/api';
import './Home.css';

export default function Home() {
  const [products, setProducts] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const navigate = useNavigate();

  // Fetch listings (normal or search)
  const getListings = useCallback(async (pageNum, query = '') => {
    if (loading) return;
    setLoading(true);
    try {
      const data = query.trim()
        ? await searchProducts(query, pageNum, 8)
        : await fetchAvailableListings(pageNum, 8);

      if (data && data.content) {
        setProducts(prev => pageNum === 0 ? data.content : [...prev, ...data.content]);
        setHasMore(!data.last);
        setPage(pageNum + 1);
      }
    } catch (e) {
      console.error("Error fetching listings", e);
    } finally {
      setLoading(false);
    }
  }, [loading]);

  // Initial load
  useEffect(() => {
    getListings(0);
  }, []);

  // Debounced search
  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchQuery.trim()) {
        setIsSearching(true);
        setPage(0);
        setHasMore(true);
        setProducts([]);
        getListings(0, searchQuery);
      } else if (isSearching) {
        setIsSearching(false);
        setPage(0);
        setHasMore(true);
        setProducts([]);
        getListings(0);
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Infinite scroll
  useEffect(() => {
    const handleScroll = () => {
      if (window.innerHeight + document.documentElement.scrollTop >= document.documentElement.offsetHeight - 150) {
        if (!loading && hasMore) {
          getListings(page, searchQuery);
        }
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [page, loading, hasMore, searchQuery]);

  const clearSearch = () => {
    setSearchQuery('');
  };

  const getConditionLabel = (yearsOld) => {
    if (!yearsOld || yearsOld === 0) return 'Brand New';
    if (yearsOld <= 1) return '< 1 Year Old';
    return `${yearsOld} Years Old`;
  };

  const getConditionClass = (yearsOld) => {
    if (!yearsOld || yearsOld === 0) return 'badge-success';
    if (yearsOld <= 2) return 'badge-primary';
    return 'badge-warning';
  };

  return (
    <div className="home-page">
      {/* Hero */}
      <div className="home-hero animate-fadeIn">
        <h1>Discover & Bargain</h1>
        <p className="subtitle">
          Buy at listed prices or negotiate directly with sellers for the best deal
        </p>

        {/* Search Bar */}
        <div className="search-container">
          <Search className="search-icon" />
          <input
            type="text"
            placeholder='Try "iPhone", "MacBook", "Headphones"...'
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            id="search-input"
          />
          {searchQuery && (
            <button className="search-clear" onClick={clearSearch} aria-label="Clear search">
              <X style={{ width: 14, height: 14 }} />
            </button>
          )}
          {isSearching && (
            <span className="search-tag">
              Showing results for <span className="highlight">"{searchQuery}"</span>
            </span>
          )}
        </div>
      </div>

      {/* Product Grid */}
      <div className="products-grid">
        {products.map(product => (
          <div key={product.id} className="card product-card animate-fadeIn">
            <div className="product-image" onClick={() => navigate(`/product/${product.id}`)}>
              <img
                src={(product.product?.imageUrl || 'https://placehold.co/500x400/1a2035/6366f1?text=No+Image').replace('/upload/', '/upload/q_auto,f_auto,w_500/')}
                alt={product.product?.title || 'Product'}
                loading="lazy"
              />
              <div className="product-condition">
                <span className={`badge ${getConditionClass(product.yearsOld)}`}>
                  {getConditionLabel(product.yearsOld)}
                </span>
              </div>
            </div>
            <div className="product-content">
              <h3>{product.product?.title || 'Untitled Product'}</h3>
              <p className="product-seller">
                {product.product?.category || 'General'} • Verified Seller
              </p>
              <p className="product-price">₹{product.price?.toLocaleString('en-IN')}</p>
              <div className="product-actions">
                <button
                  className="btn btn-success"
                  onClick={() => navigate('/payment', { state: { product } })}
                >
                  Buy Now
                </button>
                <button
                  className="btn btn-outline"
                  onClick={() => navigate(`/product/${product.id}`)}
                >
                  Bargain
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Loading */}
      {loading && (
        <div className="loading-more">
          <div className="spinner"></div>
        </div>
      )}

      {/* End of list */}
      {!hasMore && products.length > 0 && (
        <p className="end-of-list">You've seen all available listings 🎉</p>
      )}

      {/* No results */}
      {!loading && products.length === 0 && (
        <div className="empty-state animate-fadeIn">
          <div className="empty-icon">🔍</div>
          <h3>{isSearching ? 'No products match your search' : 'No products available yet'}</h3>
          <p>{isSearching ? 'Try a different keyword — our search handles typos!' : 'Be the first to list an item for sale.'}</p>
        </div>
      )}
    </div>
  );
}
