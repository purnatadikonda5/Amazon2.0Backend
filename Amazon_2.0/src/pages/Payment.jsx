import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { createPaymentSession, verifyPaymentWebhook } from '../services/api';

export default function Payment() {
  const [status, setStatus] = useState('idle');
  const [errorMessage, setErrorMessage] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  
  const product = location.state?.product;
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  useEffect(() => {
    // Load Razorpay script dynamically
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
          buyerId: user.id || 0, // Fallback if user object parse issue
          amount: parseFloat(product.price),
          idempotencyKey: "pay_rzp_" + Date.now(),
          quantity: 1
        });

        if (!orderData.razorpayOrderId) throw new Error("Gateway failed to return an order definition.");

        const options = {
            key: "rzp_test_SevHWfdpmcrBfL", // Real Test Key
            amount: orderData.amount * 100, // paise
            currency: "INR",
            name: "BargainHub Marketplace",
            description: "Checkout",
            order_id: orderData.razorpayOrderId,
            handler: async function (response) {
                try {
                    await verifyPaymentWebhook({
                        razorpayOrderId: response.razorpay_order_id,
                        razorpayPaymentId: response.razorpay_payment_id,
                        razorpaySignature: response.razorpay_signature
                    });
                    navigate('/payment/status?success=true');
                } catch(error) {
                    setErrorMessage("Payment verification failed! " + error.message);
                }
            },
            prefill: {
                name: user.name || "Known Buyer",
                email: user.email || "buyer@example.com",
            },
            theme: { color: "#3b82f6" } // matches primary blue color
        };

        const rzp = new window.Razorpay(options);
        rzp.on('payment.failed', function (response){
             setErrorMessage("Payment Failed: " + response.error.description);
             setStatus('idle');
        });
        rzp.open();

    } catch (error) {
      console.error(error);
      setErrorMessage("Payment Gateway Unavailable. " + (error.response?.data?.message || error.message));
      setStatus('idle');
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '2rem auto' }}>
      <div className="card" style={{ padding: '2rem' }}>
        <h1 style={{ marginBottom: '1.5rem', textAlign: 'center' }}>Checkout</h1>
        
        {errorMessage && (
          <div style={{ padding: '1rem', backgroundColor: '#fee2e2', color: '#b91c1c', borderRadius: '0.5rem', marginBottom: '1rem' }}>
            {errorMessage}
          </div>
        )}

        {!product ? (
          <div style={{ padding: '1rem', textAlign: 'center', color: '#6b7280' }}>
            No product selected. Please go back and select an item to buy.
          </div>
        ) : (
          <>
            <div style={{ backgroundColor: 'var(--bg-color)', padding: '1.5rem', borderRadius: '0.5rem', marginBottom: '1.5rem' }}>
              <h3 style={{ marginBottom: '1rem' }}>Order Summary</h3>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span>{product.product?.title || 'Unknown Item'}</span>
                <span>${product.price}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid var(--border-color)', paddingTop: '0.5rem', marginTop: '0.5rem', fontWeight: 'bold' }}>
                <span>Total</span>
                <span style={{ color: 'var(--primary-color)', fontSize: '1.2rem' }}>${product.price}</span>
              </div>
            </div>

            <div style={{ backgroundColor: 'var(--bg-color)', padding: '1.5rem', borderRadius: '0.5rem', marginBottom: '2rem' }}>
              <h3 style={{ marginBottom: '1rem' }}>User Details</h3>
              <p style={{ marginBottom: '0.5rem' }}><strong>Name:</strong> {user.name || 'N/A'}</p>
              <p><strong>Email:</strong> {user.email || 'N/A'}</p>
            </div>

            <button 
              className="btn btn-success" 
              style={{ width: '100%', fontSize: '1.2rem', padding: '1rem' }}
              onClick={handlePayment}
              disabled={status === 'processing'}
            >
              {status === 'processing' ? 'Processing...' : 'Proceed to Payment (Razorpay)'}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
