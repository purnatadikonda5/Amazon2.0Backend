import { useState, useEffect } from 'react';
import { Package, Tag, Clock, CheckCircle, AlertCircle, Wallet, ArrowDownToLine, ChevronRight } from 'lucide-react';
import { fetchUserOrders, fetchUserSellingListings, fetchBuyerOffers, buyerUpdateOffer, sellerUpdateOffer, fetchSellerOffers, fetchWalletBalance, withdrawWalletToBank } from '../services/api';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export default function Profile() {
  const [user] = useState(() => {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : { name: 'User', email: 'user@example.com' };
  });

  const [activeTab, setActiveTab] = useState('orders');
  const [managingItem, setManagingItem] = useState(null);

  // Data states
  const [orders, setOrders] = useState([]);
  const [activeBargains, setActiveBargains] = useState([]);
  const [sellingItems, setSellingItems] = useState([]);
  const [sellerOffers, setSellerOffers] = useState([]);
  const [walletBalance, setWalletBalance] = useState(0);

  // Pagination
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [initialLoad, setInitialLoad] = useState(true);
  const [error, setError] = useState('');

  // Load data based on active tab
  const loadData = async (pageNum, reset = false) => {
    if (loading) return;
    setLoading(true);
    setError('');
    try {
      if (activeTab === 'orders') {
        const res = await fetchUserOrders(pageNum, 10);
        if (res && res.content) {
          setOrders(prev => reset ? res.content : [...prev, ...res.content]);
          setHasMore(!res.last);
        }
      } else if (activeTab === 'bargains') {
        const res = await fetchBuyerOffers(pageNum, 10);
        if (res && res.offers) {
          setActiveBargains(prev => reset ? res.offers : [...prev, ...res.offers]);
          setHasMore(!res.last);
        }
      } else if (activeTab === 'selling') {
        const [resListings, resOffers] = await Promise.all([
          fetchUserSellingListings(pageNum, 10),
          fetchSellerOffers(0, 100)
        ]);
        if (resListings && resListings.content) {
          setSellingItems(prev => reset ? resListings.content : [...prev, ...resListings.content]);
          setHasMore(!resListings.last);
        }
        if (resOffers && resOffers.offers) {
          setSellerOffers(resOffers.offers);
        }
      }
      setPage(pageNum);
    } catch (e) {
      console.error("Dashboard error:", e);
      setError('Failed to load data. Please try again.');
    } finally {
      setLoading(false);
      setInitialLoad(false);
    }
  };

  // Track which tabs have been loaded
  const [loadedTabs, setLoadedTabs] = useState({});

  useEffect(() => {
    if (!loadedTabs[activeTab]) {
      setPage(0);
      setHasMore(true);
      setInitialLoad(true);
      loadData(0, true);
      setLoadedTabs(prev => ({ ...prev, [activeTab]: true }));
    }
  }, [activeTab]);

  // Load wallet balance on mount
  useEffect(() => {
    fetchWalletBalance()
      .then(data => setWalletBalance(data.balance || 0))
      .catch(() => {}); // Silently fail — wallet widget will show ₹0
  }, []);

  // Infinite scroll
  useEffect(() => {
    const handleScroll = () => {
      if (window.innerHeight + document.documentElement.scrollTop >= document.documentElement.offsetHeight - 80) {
        if (!loading && hasMore) {
          loadData(page + 1, false);
        }
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [page, loading, hasMore, activeTab]);

  // WebSocket for real-time offer updates
  useEffect(() => {
    if (!user?.id) return;
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        // Buyer stream — receive seller responses
        client.subscribe(`/topic/buyer/${user.id}`, (msg) => {
          const payload = JSON.parse(msg.body);
          setActiveBargains(prev => {
            const idx = prev.findIndex(o => o.id === payload.id);
            if (idx > -1) { const fresh = [...prev]; fresh[idx] = payload; return fresh; }
            return [payload, ...prev];
          });
        });
        // Seller stream — receive buyer offers
        client.subscribe(`/topic/seller/${user.id}`, (msg) => {
          const payload = JSON.parse(msg.body);
          setSellerOffers(prev => {
            const idx = prev.findIndex(o => o.id === payload.id);
            if (idx > -1) { const fresh = [...prev]; fresh[idx] = payload; return fresh; }
            return [payload, ...prev];
          });
        });
      }
    });
    client.activate();
    return () => client.deactivate();
  }, [user]);

  const handleSellerResponse = async (offerId, action, counterPrice = null) => {
    try {
      const res = await sellerUpdateOffer({ offerId, action, counterPrice });
      setSellerOffers(prev => {
        const idx = prev.findIndex(o => o.id === offerId);
        if (idx > -1) { const fresh = [...prev]; fresh[idx] = res; return fresh; }
        return prev;
      });
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to respond to offer');
    }
  };

  const handleBuyerResponse = async (offerId, action, counterPrice = null) => {
    try {
      const res = await buyerUpdateOffer({ offerId, action, counterPrice });
      setActiveBargains(prev => {
        const idx = prev.findIndex(o => o.id === offerId);
        if (idx > -1) { const fresh = [...prev]; fresh[idx] = res; return fresh; }
        return prev;
      });
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to respond');
    }
  };

  const handleWithdraw = async () => {
    try {
      const res = await withdrawWalletToBank();
      setError('');
      setWalletBalance(0);
      alert(res.message);
    } catch (err) {
      setError(err.response?.data?.message || 'Withdrawal failed');
    }
  };

  const getStatusBadge = (status) => {
    const s = (status || '').toLowerCase();
    if (s === 'completed' || s === 'accepted' || s === 'active') return 'badge-success';
    if (s === 'pending') return 'badge-warning';
    if (s === 'rejected' || s === 'cancelled') return 'badge-danger';
    if (s === 'countered') return 'badge-primary';
    return 'badge-neutral';
  };

  const tabs = [
    { key: 'orders', label: 'My Orders', icon: Package },
    { key: 'bargains', label: 'My Offers', icon: Clock },
    { key: 'selling', label: 'My Listings', icon: Tag },
  ];

  return (
    <div style={{ maxWidth: '960px', margin: '0 auto' }} className="animate-fadeIn">

      {/* Profile Header */}
      <div className="card" style={{ padding: '1.75rem 2rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1.5rem' }}>
          {/* User Info */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={{
              width: '56px', height: '56px', borderRadius: '50%',
              background: 'linear-gradient(135deg, var(--primary), var(--accent))',
              color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '1.4rem', fontWeight: '700', flexShrink: 0
            }}>
              {(user.name || user.email || 'U').charAt(0).toUpperCase()}
            </div>
            <div>
              <h2 style={{ fontSize: '1.3rem', fontWeight: '700', marginBottom: '0.15rem' }}>
                {user.name || 'Dashboard'}
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{user.email}</p>
            </div>
          </div>

          {/* Wallet */}
          <div style={{
            padding: '1rem 1.25rem', background: 'var(--bg-elevated)', borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border)', minWidth: '200px', textAlign: 'right'
          }}>
            <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '0.4rem', marginBottom: '0.3rem' }}>
              <Wallet className="icon-sm" style={{ color: 'var(--success)' }} />
              <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem', fontWeight: '500' }}>Wallet Balance</span>
            </div>
            <p style={{ fontSize: '1.6rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.6rem' }}>
              ₹{walletBalance.toFixed(2)}
            </p>
            <button
              onClick={handleWithdraw}
              disabled={walletBalance <= 0}
              className="btn btn-outline btn-sm"
              style={{ width: '100%', fontSize: '0.78rem' }}
            >
              <ArrowDownToLine className="icon-sm" /> Withdraw to Bank
            </button>
          </div>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="alert alert-error animate-fadeIn" style={{ marginBottom: '1rem' }}>
          <AlertCircle className="icon-sm" /> {error}
          <button onClick={() => setError('')} style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', color: 'inherit', fontSize: '1rem' }}>×</button>
        </div>
      )}

      {/* Tab Navigation */}
      <div style={{
        display: 'flex', gap: '0.35rem', marginBottom: '1.5rem',
        background: 'var(--bg-card)', padding: '0.35rem', borderRadius: 'var(--radius-lg)',
        border: '1px solid var(--border)'
      }}>
        {tabs.map(tab => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={isActive ? 'btn btn-sm' : 'btn btn-ghost btn-sm'}
              style={{
                flex: 1, borderRadius: 'var(--radius-md)',
                ...(isActive ? {} : { fontWeight: '500' })
              }}
            >
              <Icon className="icon-sm" /> {tab.label}
            </button>
          );
        })}
      </div>

      {/* Tab Content */}
      <div className="animate-fadeIn">
        {/* Loading State */}
        {initialLoad && loading && (
          <div className="loading-section">
            <div className="spinner"></div>
            <p>Loading your {activeTab}...</p>
          </div>
        )}

        {/* ===== ORDERS TAB ===== */}
        {activeTab === 'orders' && !initialLoad && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {orders.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">📦</div>
                <h3>No orders yet</h3>
                <p>Your purchase history will appear here after your first order.</p>
              </div>
            ) : orders.map(o => (
              <div key={o.id} className="card" style={{ padding: '1.25rem 1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
                <div>
                  <h3 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '0.3rem' }}>
                    Order #{o.id}
                  </h3>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>
                    Product #{o.productId} • {o.createdAt ? new Date(o.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : 'N/A'}
                  </p>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <p style={{ fontSize: '1.2rem', fontWeight: '700', marginBottom: '0.4rem', color: 'var(--primary)' }}>
                    ₹{o.purchasePrice?.toLocaleString('en-IN')}
                  </p>
                  <span className={`badge ${getStatusBadge(o.status)}`}>
                    <CheckCircle style={{ width: 12, height: 12 }} /> {o.status || 'completed'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* ===== BARGAINS TAB ===== */}
        {activeTab === 'bargains' && !initialLoad && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {activeBargains.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">🤝</div>
                <h3>No active offers</h3>
                <p>When you make an offer on a product, it will appear here.</p>
              </div>
            ) : activeBargains.map(b => (
              <div key={b.id} className="card" style={{ padding: '1.25rem 1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem', flexWrap: 'wrap' }}>
                  <div>
                    <h3 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '0.3rem' }}>
                      Offer #{b.id} — Product #{b.productId}
                    </h3>
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.82rem', marginBottom: '0.4rem' }}>
                      {b.createdAt ? new Date(b.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : ''}
                    </p>
                    <span className={`badge ${getStatusBadge(b.status)}`}>
                      {(b.status || 'pending').toUpperCase()}
                    </span>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <p style={{ fontSize: '1.15rem', fontWeight: '700', marginBottom: '0.3rem' }}>
                      Your Offer: <span style={{ color: 'var(--primary)' }}>₹{b.offerPrice?.toLocaleString('en-IN')}</span>
                    </p>
                    {b.counterPrice && (
                      <p style={{ fontSize: '0.95rem', fontWeight: '600', color: 'var(--warning)' }}>
                        Counter: ₹{b.counterPrice?.toLocaleString('en-IN')}
                      </p>
                    )}

                    {b.status === 'countered' && (
                      <div style={{ marginTop: '0.75rem', display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                        <button className="btn btn-success btn-sm" onClick={() => handleBuyerResponse(b.id, 'accept')}>
                          Accept
                        </button>
                        <button className="btn btn-outline btn-sm" onClick={() => {
                          const val = prompt('Enter your counter-offer price (₹):');
                          if (val) handleBuyerResponse(b.id, 'counter', parseFloat(val));
                        }}>
                          Counter
                        </button>
                        <button className="btn btn-danger btn-sm" onClick={() => handleBuyerResponse(b.id, 'reject')}>
                          Reject
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* ===== SELLING TAB ===== */}
        {activeTab === 'selling' && !initialLoad && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {sellingItems.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">🏷️</div>
                <h3>No listings yet</h3>
                <p>List your first item to start selling on BargainHub.</p>
              </div>
            ) : sellingItems.map(s => {
              const activeOffers = sellerOffers.filter(o => o.listingId === s.id && o.status !== 'rejected');
              return (
                <div key={s.id} className="card" style={{ padding: '1.25rem 1.5rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
                    <div>
                      <h3 style={{ fontSize: '1rem', fontWeight: '600', marginBottom: '0.3rem' }}>
                        {s.product?.title || 'Your Product'}
                      </h3>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <span className={`badge ${getStatusBadge(s.status)}`}>{s.status}</span>
                        <span style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>
                          {activeOffers.length} offer{activeOffers.length !== 1 ? 's' : ''} pending
                        </span>
                      </div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                      <p style={{ fontSize: '1.2rem', fontWeight: '700', color: 'var(--primary)' }}>
                        ₹{s.price?.toLocaleString('en-IN')}
                      </p>
                      {managingItem !== s.id && (
                        <button className="btn btn-outline btn-sm" onClick={() => setManagingItem(s.id)}>
                          Review <ChevronRight className="icon-sm" />
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Expanded Offers Section */}
                  {managingItem === s.id && (
                    <div style={{ marginTop: '1.25rem', borderTop: '1px solid var(--border)', paddingTop: '1.25rem' }}>
                      <h4 style={{ fontSize: '0.9rem', fontWeight: '600', marginBottom: '0.75rem', color: 'var(--text-secondary)' }}>
                        Incoming Offers
                      </h4>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                        {activeOffers.length === 0 ? (
                          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', padding: '1rem 0' }}>No offers yet for this listing.</p>
                        ) : activeOffers.map(offer => (
                          <div key={offer.id} style={{
                            display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between',
                            alignItems: 'center', background: 'var(--bg-elevated)', padding: '0.85rem 1rem',
                            borderRadius: 'var(--radius-md)', border: '1px solid var(--border)'
                          }}>
                            <div>
                              <p style={{ fontWeight: '600', fontSize: '0.9rem', marginBottom: '0.2rem' }}>
                                Buyer #{offer.buyerId} offered <span style={{ color: 'var(--primary)' }}>₹{offer.offerPrice?.toLocaleString('en-IN')}</span>
                              </p>
                              <span className={`badge ${getStatusBadge(offer.status)}`} style={{ fontSize: '0.7rem' }}>
                                {offer.status?.toUpperCase()}
                              </span>
                            </div>
                            <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.25rem' }}>
                              {offer.status === 'pending' && (
                                <>
                                  <button className="btn btn-success btn-sm" onClick={() => handleSellerResponse(offer.id, 'accept')}>Accept</button>
                                  <button className="btn btn-danger btn-sm" onClick={() => handleSellerResponse(offer.id, 'reject')}>Reject</button>
                                  <button className="btn btn-outline btn-sm" onClick={() => {
                                    const counter = prompt('Enter your counter-offer price (₹):');
                                    if (counter) handleSellerResponse(offer.id, 'counter', parseFloat(counter));
                                  }}>Counter</button>
                                </>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                      <button
                        className="btn btn-ghost btn-sm"
                        style={{ width: '100%', marginTop: '1rem' }}
                        onClick={() => setManagingItem(null)}
                      >
                        Close Offers
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Scroll spinner */}
        {loading && !initialLoad && (
          <div style={{ padding: '2rem', textAlign: 'center' }}>
            <div className="spinner"></div>
          </div>
        )}
      </div>
    </div>
  );
}
