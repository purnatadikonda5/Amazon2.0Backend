import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchListingById, submitBuyerOffer } from '../services/api';

export default function ProductDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [offerPrice, setOfferPrice] = useState('');
  const [bargainStatus, setBargainStatus] = useState(null);

  useEffect(() => {
    const loadItem = async () => {
      try {
        const data = await fetchListingById(id);
        setProduct(data);
        setOfferPrice(Math.floor(data.price * 0.9).toString()); // auto-suggest 10% lower
      } catch (err) {
        console.error("Failed fetching listing.", err);
      }
    };
    loadItem();
  }, [id]);

  const handleBargain = async (e) => {
    e.preventDefault();
    setBargainStatus('submitting');
    try {
        await submitBuyerOffer({
            listingId: parseInt(id),
            quantity: 1,
            offeredPrice: parseFloat(offerPrice)
        });
        setBargainStatus('success');
    } catch(err) {
        alert(err.response?.data?.message || err.message);
        setBargainStatus('idle');
    }
  };

  if (!product) return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading...</div>;

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      <div className="card" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '0', overflow: 'hidden' }}>
        <div style={{ height: '100%', minHeight: '400px' }}>
          <img 
            src={(product.product?.imageUrl || 'https://via.placeholder.com/500').replace('/upload/', '/upload/q_auto,f_auto,w_800/')} 
            alt={product.product?.title || 'Item'} 
            style={{ width: '100%', height: '100%', objectFit: 'cover' }} 
          />
        </div>
        <div style={{ padding: '2.5rem', display: 'flex', flexDirection: 'column' }}>
          <h1 style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>{product.product?.title || 'Unknown Title'}</h1>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1rem' }}>BargainHub Authenticated Seller</p>
          
          <div style={{ margin: '1.5rem 0' }}>
            <span style={{ fontSize: '2.5rem', fontWeight: 'bold', color: 'var(--primary-color)' }}>
              ${product.price}
            </span>
          </div>

          <p style={{ lineHeight: '1.6', marginBottom: '2rem', flex: 1, color: 'var(--text-color)' }}>{product.product?.description}</p>
          
          <button 
            className="btn btn-success" 
            style={{ width: '100%', marginBottom: '2rem', fontSize: '1.1rem', padding: '1rem' }}
            onClick={() => navigate('/payment', { state: { product } })}
          >
            Buy Now
          </button>

          <div style={{ backgroundColor: 'var(--bg-color)', padding: '1.5rem', borderRadius: '0.5rem', border: '1px solid var(--border-color)' }}>
            <h3 style={{ marginBottom: '1rem', fontSize: '1.2rem' }}>Make an Offer</h3>
            {bargainStatus === 'success' ? (
              <div style={{ color: 'var(--success-color)', fontWeight: '500', padding: '1rem', backgroundColor: '#ecfdf5', borderRadius: '0.5rem', textAlign: 'center' }}>
                Your offer of ${offerPrice} has been sent to the seller!
              </div>
            ) : (
              <form onSubmit={handleBargain} style={{ display: 'flex', gap: '1rem' }}>
                <div style={{ position: 'relative', flex: 1 }}>
                  <span style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', fontWeight: 'bold' }}>$</span>
                  <input 
                    type="number" 
                    value={offerPrice} 
                    onChange={(e) => setOfferPrice(e.target.value)} 
                    style={{ paddingLeft: '2rem' }}
                    required 
                    min="1"
                  />
                </div>
                <button type="submit" className="btn" disabled={bargainStatus === 'submitting'}>
                  {bargainStatus === 'submitting' ? 'Sending...' : 'Submit Offer'}
                </button>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
