import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { CreditCard, ShieldCheck, User, AlertCircle } from 'lucide-react';
import { createPaymentSession, verifyPaymentWebhook } from '../services/api';

export default function Payment() {
  const [status, setStatus] = useState('idle');
  const [errorMessage, setErrorMessage] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  const product = location.state?.product;
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    document.body.appendChild(script);
  }, []);

  const handlePayment = async () => {
    setStatus('processing');
    setErrorMessage('');

    try {
      const orderData = await createPaymentSession({
        listingId: product.id,
        buyerId: user.id || 0,
        amount: parseFloat(product.price),
        idempotencyKey: "pay_rzp_" + Date.now(),
        quantity: 1
      });

      if (!orderData.razorpayOrderId) throw new Error("Payment gateway failed to create order.");

      const options = {
        key: "rzp_test_SevHWfdpmcrBfL",
        amount: orderData.amount * 100,
        currency: "INR",
        name: "BargainHub",
        description: product.product?.title || "Checkout",
        order_id: orderData.razorpayOrderId,
        handler: async function (response) {
          try {
            await verifyPaymentWebhook({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature
            });
            navigate('/payment/status?success=true');
          } catch (error) {
            setErrorMessage("Payment verification failed. Contact support.");
            setStatus('idle');
          }
        },
        prefill: {
          name: user.name || "Buyer",
          email: user.email || "buyer@example.com",
        },
        theme: { color: "#6366f1" }
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', function (response) {
        setErrorMessage("Payment failed: " + response.error.description);
        setStatus('idle');
      });
      rzp.open();
    } catch (error) {
      setErrorMessage(error.response?.data?.message || error.message || "Payment gateway unavailable.");
      setStatus('idle');
    }
  };

  return (
    <div style={{ maxWidth: '520px', margin: '3rem auto' }} className="animate-slideUp">
      <div className="card" style={{ padding: '2rem' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '1.75rem' }}>
          <div style={{
            width: '48px', height: '48px', borderRadius: '50%',
            background: 'linear-gradient(135deg, var(--primary), var(--success))',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 0.85rem'
          }}>
            <CreditCard style={{ color: '#fff', width: 20, height: 20 }} />
          </div>
          <h1 style={{ fontSize: '1.4rem', fontWeight: '700' }}>Checkout</h1>
        </div>

        {/* Error */}
        {errorMessage && (
          <div className="alert alert-error animate-fadeIn">
            <AlertCircle className="icon-sm" /> {errorMessage}
          </div>
        )}

        {!product ? (
          <div className="empty-state">
            <div className="empty-icon">🛒</div>
            <h3>No product selected</h3>
            <p>Go back and select an item to purchase.</p>
          </div>
        ) : (
          <>
            {/* Order Summary */}
            <div style={{
              background: 'var(--bg-elevated)', padding: '1.25rem', borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--border)', marginBottom: '1rem'
            }}>
              <h3 style={{ fontSize: '0.85rem', fontWeight: '600', color: 'var(--text-muted)', marginBottom: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Order Summary
              </h3>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{product.product?.title || 'Product'}</span>
                <span style={{ fontWeight: '600' }}>₹{product.price?.toLocaleString('en-IN')}</span>
              </div>
              <div style={{ borderTop: '1px solid var(--border)', paddingTop: '0.6rem', marginTop: '0.5rem', display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ fontWeight: '700' }}>Total</span>
                <span style={{ fontWeight: '700', color: 'var(--primary)', fontSize: '1.15rem' }}>₹{product.price?.toLocaleString('en-IN')}</span>
              </div>
            </div>

            {/* User Info */}
            <div style={{
              background: 'var(--bg-elevated)', padding: '1rem 1.25rem', borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--border)', marginBottom: '1.75rem', display: 'flex', alignItems: 'center', gap: '0.75rem'
            }}>
              <User className="icon" style={{ color: 'var(--text-muted)' }} />
              <div>
                <p style={{ fontWeight: '600', fontSize: '0.9rem' }}>{user.name || 'N/A'}</p>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>{user.email || 'N/A'}</p>
              </div>
            </div>

            <button
              className="btn btn-success btn-lg"
              style={{ width: '100%', fontSize: '1rem' }}
              onClick={handlePayment}
              disabled={status === 'processing'}
              id="pay-btn"
            >
              {status === 'processing' ? (
                <><div className="spinner spinner-sm" style={{ borderTopColor: '#fff', borderColor: 'rgba(255,255,255,0.3)' }}></div> Processing...</>
              ) : (
                <><ShieldCheck className="icon" /> Pay ₹{product.price?.toLocaleString('en-IN')}</>
              )}
            </button>

            <p style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.75rem', marginTop: '0.85rem' }}>
              🔒 Secured by Razorpay. Your payment info is encrypted.
            </p>
          </>
        )}
      </div>
    </div>
  );
}
