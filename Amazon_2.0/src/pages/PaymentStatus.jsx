import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle } from 'lucide-react';

export default function PaymentStatus() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const isSuccess = searchParams.get('success') === 'true';

  return (
    <div style={{ maxWidth: '500px', margin: '4rem auto', textAlign: 'center' }}>
      <div className="card" style={{ padding: '3rem 2rem' }}>
        {isSuccess ? (
          <>
            <CheckCircle style={{ width: '80px', height: '80px', color: 'var(--success-color)', margin: '0 auto 1.5rem' }} />
            <h1 style={{ marginBottom: '1rem', color: 'var(--success-color)' }}>Payment Successful!</h1>
            <p style={{ color: 'var(--text-muted)', marginBottom: '2rem' }}>Your order has been confirmed and is being processed by the seller.</p>
          </>
        ) : (
          <>
            <XCircle style={{ width: '80px', height: '80px', color: 'var(--danger-color)', margin: '0 auto 1.5rem' }} />
            <h1 style={{ marginBottom: '1rem', color: 'var(--danger-color)' }}>Payment Failed</h1>
            <p style={{ color: 'var(--text-muted)', marginBottom: '2rem' }}>There was an issue processing your payment. Please try again or use a different payment method.</p>
          </>
        )}
        
        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
          <button className="btn btn-primary" onClick={() => navigate('/profile')}>
            View My Orders
          </button>
          <button className="btn btn-outline" onClick={() => navigate('/')}>
            Continue Shopping
          </button>
        </div>
      </div>
    </div>
  );
}
