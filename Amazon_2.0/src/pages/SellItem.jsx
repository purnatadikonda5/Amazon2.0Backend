import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, Image, DollarSign, Package, Calendar, Hash, CheckCircle, AlertCircle } from 'lucide-react';
import { uploadImage, createListing } from '../services/api';

export default function SellItem() {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [yearsOld, setYearsOld] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [status, setStatus] = useState('idle');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImageFile(file);
      const reader = new FileReader();
      reader.onload = (ev) => setImagePreview(ev.target.result);
      reader.readAsDataURL(file);
    }
  };

  const handleSellItem = async (e) => {
    e.preventDefault();
    setError('');
    setStatus('uploading');
    try {
      let cloudUrl = "";
      if (imageFile) {
        cloudUrl = await uploadImage(imageFile);
      }

      setStatus('submitting');
      await createListing({
        title: title.trim(),
        description: description.trim(),
        price: parseFloat(price) || 0,
        minAcceptablePrice: parseFloat(minPrice) || 0,
        yearsOld: parseInt(yearsOld) || 0,
        imageUrl: cloudUrl,
        customImageUrls: JSON.stringify([cloudUrl]),
        category: "General",
        quantity: parseInt(quantity) || 1,
        isAvailable: true
      });

      setStatus('success');
      setTimeout(() => navigate('/profile'), 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Error creating listing. Please try again.');
      setStatus('idle');
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '2rem auto' }} className="animate-slideUp">
      <div className="card" style={{ padding: '2.5rem 2rem' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            width: '52px', height: '52px', borderRadius: '50%',
            background: 'linear-gradient(135deg, var(--accent), var(--primary))',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 1rem'
          }}>
            <Package style={{ color: '#fff', width: 22, height: 22 }} />
          </div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '0.35rem' }}>List an Item</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Put your item up for sale or bargaining</p>
        </div>

        {status === 'success' ? (
          <div className="animate-scaleIn" style={{ textAlign: 'center', padding: '2rem' }}>
            <CheckCircle style={{ width: '64px', height: '64px', color: 'var(--success)', margin: '0 auto 1rem' }} />
            <h2 style={{ fontSize: '1.3rem', fontWeight: '700', marginBottom: '0.5rem' }}>Item Listed!</h2>
            <p style={{ color: 'var(--text-muted)' }}>Redirecting to your dashboard...</p>
          </div>
        ) : (
          <form onSubmit={handleSellItem}>
            {error && (
              <div className="alert alert-error animate-fadeIn">
                <AlertCircle className="icon-sm" /> {error}
              </div>
            )}

            <div className="form-group">
              <label>Product Title</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                placeholder="e.g. iPhone 14 Pro Max 256GB"
                id="sell-title"
              />
            </div>

            <div className="form-group">
              <label>Description</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
                placeholder="Describe condition, specs, and any defects..."
                rows="3"
                id="sell-description"
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label>Selling Price (₹)</label>
                <input type="number" value={price} onChange={(e) => setPrice(e.target.value)} required min="1" placeholder="0" id="sell-price" />
              </div>
              <div className="form-group">
                <label>Min. Acceptable (₹)</label>
                <input type="number" value={minPrice} onChange={(e) => setMinPrice(e.target.value)} required min="1" placeholder="0" id="sell-min-price" />
              </div>
              <div className="form-group">
                <label>Age (Years)</label>
                <input type="number" value={yearsOld} onChange={(e) => setYearsOld(e.target.value)} required min="0" placeholder="0" id="sell-years" />
              </div>
              <div className="form-group">
                <label>Quantity</label>
                <input type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} required min="1" placeholder="1" id="sell-quantity" />
              </div>
            </div>

            {/* Image Upload with Preview */}
            <div className="form-group">
              <label>Product Image</label>
              <div style={{
                border: '2px dashed var(--border)', borderRadius: 'var(--radius-lg)',
                padding: imagePreview ? '0' : '2rem', textAlign: 'center',
                cursor: 'pointer', overflow: 'hidden', position: 'relative',
                transition: 'border-color 0.2s'
              }}
                onClick={() => document.getElementById('file-upload').click()}
                onDragOver={(e) => e.preventDefault()}
              >
                {imagePreview ? (
                  <img src={imagePreview} alt="Preview" style={{ width: '100%', maxHeight: '240px', objectFit: 'cover', display: 'block' }} />
                ) : (
                  <div>
                    <Image style={{ width: 32, height: 32, color: 'var(--text-muted)', marginBottom: '0.5rem' }} />
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Click to upload (Max 5MB)</p>
                  </div>
                )}
                <input
                  id="file-upload"
                  type="file"
                  accept="image/*"
                  onChange={handleImageChange}
                  required
                  style={{ position: 'absolute', opacity: 0, width: 0, height: 0 }}
                />
              </div>
              {imageFile && (
                <small style={{ color: 'var(--text-muted)', marginTop: '0.35rem', display: 'block' }}>
                  {imageFile.name} ({(imageFile.size / 1024 / 1024).toFixed(2)} MB)
                </small>
              )}
            </div>

            <button
              type="submit"
              className="btn btn-accent btn-lg"
              style={{ width: '100%', marginTop: '0.5rem' }}
              disabled={status !== 'idle'}
              id="sell-submit"
            >
              {status === 'uploading' ? (
                <><div className="spinner spinner-sm" style={{ borderTopColor: '#fff', borderColor: 'rgba(255,255,255,0.3)' }}></div> Uploading Image...</>
              ) : status === 'submitting' ? (
                <><div className="spinner spinner-sm" style={{ borderTopColor: '#fff', borderColor: 'rgba(255,255,255,0.3)' }}></div> Creating Listing...</>
              ) : (
                <><Upload className="icon-sm" /> List Item for Sale</>
              )}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
