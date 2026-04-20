import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchAvailableListings } from '../services/api';
import './Home.css';

export default function Home() {
  const [products, setProducts] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const getListings = async (pageNum) => {
    if (loading || !hasMore) return;
    setLoading(true);
    try {
        const data = await fetchAvailableListings(pageNum, 6);
        if(data && data.content) {
            setProducts(prev => pageNum === 0 ? data.content : [...prev, ...data.content]);
            setHasMore(!data.last); // Backend pageable usually returns `last: boolean`
            setPage(pageNum + 1);
        }
    } catch(e) {
        console.error("Error fetching listings", e);
    } finally {
        setLoading(false);
    }
  };

  useEffect(() => {
    getListings(0);
  }, []);

  useEffect(() => {
    const handleScroll = () => {
      // Trigger if user is within 100 pixels of the bottom
      if (window.innerHeight + document.documentElement.scrollTop >= document.documentElement.offsetHeight - 100) {
        if (!loading && hasMore) {
          getListings(page);
        }
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [page, loading, hasMore]);

  return (
    <div className="home-page">
      <div className="home-header">
        <h1>Discover Great Deals</h1>
        <p className="subtitle">Buy directly or bargain for the best price</p>
      </div>
      
      <div className="products-grid">
        {products.map(product => (
          <div key={product.id} className="card product-card">
            <div className="product-image">
              {/* Product response has 'product' nested structure. Cloudinary on-the-fly optimization included! */}
              <img 
                src={(product.product?.imageUrl || 'https://via.placeholder.com/150').replace('/upload/', '/upload/q_auto,f_auto,w_500/')} 
                alt={product.product?.title || 'Item'} 
                loading="lazy"
              />
            </div>
            <div className="product-content">
              <h3>{product.product?.title || 'Unknown Title'}</h3>
              <p className="seller">Condition: {product.yearsOld ? `${product.yearsOld} years old` : 'New'}</p>
              <p className="price">${product.price}</p>
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
      
      {loading && (
        <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
          <h3>Loading more deals...</h3>
        </div>
      )}
      
      {!hasMore && products.length > 0 && (
        <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
          <p>You've reached the end of the market!</p>
        </div>
      )}
    </div>
  );
}
