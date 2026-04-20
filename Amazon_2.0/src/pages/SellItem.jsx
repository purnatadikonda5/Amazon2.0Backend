import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { uploadImage, createListing } from '../services/api';

export default function SellItem() {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [yearsOld, setYearsOld] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [imageFile, setImageFile] = useState(null);
  const [status, setStatus] = useState('idle'); // idle, uploading, submitting, success
  const navigate = useNavigate();

  const handleSellItem = async (e) => {
    e.preventDefault();
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
        setTimeout(() => navigate('/profile'), 1500);
    } catch(err) {
        console.error(err);
        alert(err.response?.data?.message || 'Error creating listing');
        setStatus('idle');
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <div className="card" style={{ padding: '2rem' }}>
        <h1 style={{ marginBottom: '1.5rem', textAlign: 'center' }}>Sell an Item</h1>
        
        {status === 'success' ? (
          <div style={{ textAlign: 'center', padding: '2rem' }}>
            <div style={{ fontSize: '3rem', color: 'var(--success-color)', marginBottom: '1rem' }}>✓</div>
            <h2>Item Listed Successfully!</h2>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>Redirecting to your profile...</p>
          </div>
        ) : (
          <form onSubmit={handleSellItem}>
            <div className="form-group">
              <label>Product Title</label>
              <input 
                type="text" 
                value={title} 
                onChange={(e) => setTitle(e.target.value)} 
                required 
                placeholder="What are you selling?" 
              />
            </div>
            
            <div className="form-group">
              <label>Description</label>
              <textarea 
                value={description} 
                onChange={(e) => setDescription(e.target.value)} 
                required 
                placeholder="Describe your item in detail..." 
                rows="4"
              />
            </div>
            
            <div className="form-group" style={{ display: 'flex', gap: '1rem' }}>
              <div style={{ flex: 1 }}>
                <label>Selling Price ($)</label>
                <input type="number" value={price} onChange={(e) => setPrice(e.target.value)} required min="1" placeholder="0.00" />
              </div>
              <div style={{ flex: 1 }}>
                <label>Minimum Acceptable Price ($)</label>
                <input type="number" value={minPrice} onChange={(e) => setMinPrice(e.target.value)} required min="1" placeholder="0.00" />
              </div>
              <div style={{ flex: 1 }}>
                <label>Years Old</label>
                <input type="number" value={yearsOld} onChange={(e) => setYearsOld(e.target.value)} required min="0" placeholder="e.g. 2" />
              </div>
              <div style={{ flex: 1 }}>
                <label>Quantity</label>
                <input type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} required min="1" placeholder="1" />
              </div>
            </div>
            
            <div className="form-group">
              <label>Upload Image File (Max 5MB)</label>
              <input 
                type="file" 
                accept="image/*"
                onChange={(e) => setImageFile(e.target.files[0])} 
                required 
              />
            </div>
            
            <button type="submit" className="btn" style={{ width: '100%', marginTop: '1rem', fontSize: '1.1rem' }} disabled={status !== 'idle'}>
              {status === 'uploading' ? 'Uploading Image to Cloud...' : status === 'submitting' ? 'Finalizing Database...' : 'List Item for Sale'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
