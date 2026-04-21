import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ShoppingCart, Send, ArrowLeft, Tag, AlertCircle, CheckCircle } from 'lucide-react';
import { fetchListingById, submitBuyerOffer } from '../services/api';

export default function ProductDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [offerPrice, setOfferPrice] = useState('');
  const [bargainStatus, setBargainStatus] = useState(null); // null, 'submitting', 'success', 'error'
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    const loadItem = async () => {
      try {
        const data = await fetchListingById(id);
        setProduct(data);
        setOfferPrice(Math.floor(data.price * 0.9).toString());
      } catch (err) {
        console.error("Failed fetching listing.", err);
      }
    };
    loadItem();
  }, [id]);

  const handleBargain = async (e) => {
    e.preventDefault();
    setBargainStatus('submitting');
    setErrorMsg('');
    try {
      await submitBuyerOffer({
        listingId: parseInt(id),
        quantity: 1,
        offeredPrice: parseFloat(offerPrice)
      });
      setBargainStatus('success');
    } catch (err) {
      setErrorMsg(err.response?.data?.message || err.message || 'Failed to submit offer');
      setBargainStatus('error');
    }
  };

  if (!product) {
    return (
      <div className="loading-section" style={{ minHeight: '400px' }}>
        <div className="spinner"></div>
        <p>Loading product details...</p>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }} className="animate-fadeIn">
      {/* Back button */}
      <button
        className="btn btn-ghost btn-sm"
        onClick={() => navigate(-1)}
        style={{ marginBottom: '1rem' }}
      >
        <ArrowLeft className="icon-sm" /> Back
      </button>

      <div className="card" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 0, overflow: 'hidden' }}>
        {/* Image */}
        <div style={{ minHeight: '400px', background: 'var(--bg-elevated)', position: 'relative' }}>
          <img
            src={(product.product?.imageUrl || 'https://placehold.co/800x600/1a2035/6366f1?text=No+Image').replace('/upload/', '/upload/q_auto,f_auto,w_800/')}
            alt={product.product?.title || 'Product'}
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
          {/* Condition badge */}
          <div style={{ position: 'absolute', top: '1rem', left: '1rem' }}>
            <span className="badge badge-primary">
              <Tag className="icon-sm" />
              {product.yearsOld ? `${product.yearsOld}yr old` : 'Brand New'}
            </span>
          </div>
        </div>

        {/* Details */}
        <div style={{ padding: '2rem', display: 'flex', flexDirection: 'column' }}>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', fontWeight: '500', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            {product.product?.category || 'General'} • Verified Seller
          </p>
          <h1 style={{ fontSize: '1.6rem', fontWeight: '700', marginBottom: '0.75rem', lineHeight: 1.3 }}>
            {product.product?.title || 'Product'}
          </h1>

          <div style={{ margin: '1rem 0 1.5rem' }}>
            <span style={{ fontSize: '2.2rem', fontWeight: '800', color: 'var(--primary)' }}>
              ₹{product.price?.toLocaleString('en-IN')}
            </span>
          </div>

          <p style={{ lineHeight: 1.7, marginBottom: '1.5rem', flex: 1, color: 'var(--text-secondary)', fontSize: '0.92rem' }}>
            {product.product?.description || 'No description available.'}
          </p>

          {/* Buy Now */}
          <button
            className="btn btn-success btn-lg"
            style={{ width: '100%', marginBottom: '1.25rem', fontSize: '1rem' }}
            onClick={() => navigate('/payment', { state: { product } })}
            id="buy-now-btn"
          >
            <ShoppingCart className="icon" /> Buy Now — ₹{product.price?.toLocaleString('en-IN')}
          </button>

          {/* Bargain Section */}
          <div style={{
            background: 'var(--bg-elevated)', padding: '1.25rem', borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border)'
          }}>
            <h3 style={{ fontSize: '0.95rem', fontWeight: '600', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <Send className="icon-sm" style={{ color: 'var(--primary)' }} /> Make an Offer
            </h3>

            {bargainStatus === 'success' ? (
              <div className="alert alert-success animate-scaleIn" style={{ margin: 0 }}>
                <CheckCircle className="icon-sm" /> Your offer of ₹{offerPrice} has been sent!
              </div>
            ) : (
              <>
                {errorMsg && (
                  <div className="alert alert-error" style={{ marginBottom: '0.75rem' }}>
                    <AlertCircle className="icon-sm" /> {errorMsg}
                  </div>
                )}
                <form onSubmit={handleBargain} style={{ display: 'flex', gap: '0.75rem' }}>
                  <div style={{ position: 'relative', flex: 1 }}>
                    <span style={{ position: 'absolute', left: '0.85rem', top: '50%', transform: 'translateY(-50%)', fontWeight: '600', color: 'var(--text-muted)' }}>₹</span>
                    <input
                      type="number"
                      value={offerPrice}
                      onChange={(e) => setOfferPrice(e.target.value)}
                      style={{ paddingLeft: '2rem' }}
                      required
                      min="1"
                      id="offer-price-input"
                    />
                  </div>
                  <button type="submit" className="btn" disabled={bargainStatus === 'submitting'} id="submit-offer-btn">
                    {bargainStatus === 'submitting' ? 'Sending...' : 'Submit Offer'}
                  </button>
                </form>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
