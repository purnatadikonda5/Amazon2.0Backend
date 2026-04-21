import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, ArrowRight } from 'lucide-react';

export default function PaymentStatus() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const isSuccess = searchParams.get('success') === 'true';

  return (
    <div style={{ maxWidth: '480px', margin: '5rem auto' }} className="animate-scaleIn">
      <div className="card" style={{ padding: '3rem 2rem', textAlign: 'center' }}>
        {isSuccess ? (
          <>
            <div style={{
              width: '80px', height: '80px', borderRadius: '50%',
              background: 'var(--success-light)', display: 'flex', alignItems: 'center',
              justifyContent: 'center', margin: '0 auto 1.5rem'
            }}>
              <CheckCircle style={{ width: '44px', height: '44px', color: 'var(--success)' }} />
            </div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--success)', marginBottom: '0.5rem' }}>
              Payment Successful!
            </h1>
            <p style={{ color: 'var(--text-muted)', marginBottom: '2rem', fontSize: '0.9rem' }}>
              Your order has been confirmed and the seller has been notified.
            </p>
          </>
        ) : (
          <>
            <div style={{
              width: '80px', height: '80px', borderRadius: '50%',
              background: 'var(--danger-light)', display: 'flex', alignItems: 'center',
              justifyContent: 'center', margin: '0 auto 1.5rem'
            }}>
              <XCircle style={{ width: '44px', height: '44px', color: 'var(--danger)' }} />
            </div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--danger)', marginBottom: '0.5rem' }}>
              Payment Failed
            </h1>
            <p style={{ color: 'var(--text-muted)', marginBottom: '2rem', fontSize: '0.9rem' }}>
              Something went wrong. Please try again or use a different payment method.
            </p>
          </>
        )}

        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
          <button className="btn" onClick={() => navigate('/profile')} id="view-orders-btn">
            View My Orders <ArrowRight className="icon-sm" />
          </button>
          <button className="btn btn-outline" onClick={() => navigate('/')}>
            Continue Shopping
          </button>
        </div>
      </div>
    </div>
  );
}
