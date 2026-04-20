import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { signupUser, checkEmailExists } from '../services/api';

export default function Signup() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailStatus, setEmailStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleEmailBlur = async () => {
    if(!email) return;
    try {
        const taken = await checkEmailExists(email);
        if(taken) setEmailStatus('Email is already taken!');
        else setEmailStatus('Email is available!');
    } catch(e) { console.error(e) }
  };

  const handleSignup = async (e) => {
    e.preventDefault();
    if(emailStatus === 'Email is already taken!') return;
    setLoading(true);
    try {
        await signupUser({ name, email, password });
        navigate('/login');
    } catch(e) {
        alert(e.response?.data?.message || 'Error creating account');
    } finally {
        setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: '400px', margin: '2rem auto', padding: '2rem' }}>
      <h1 style={{ marginBottom: '1.5rem', textAlign: 'center' }}>Create an Account</h1>
      <form onSubmit={handleSignup}>
        <div className="form-group">
          <label>Full Name</label>
          <input 
            type="text" 
            value={name} 
            onChange={(e) => setName(e.target.value)} 
            required 
            placeholder="Enter your name" 
          />
        </div>
        <div className="form-group">
          <label>Email Address</label>
          <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
            <input 
              type="email" 
              value={email} 
              onChange={(e) => { setEmail(e.target.value); setEmailStatus(''); }} 
              onBlur={handleEmailBlur}
              required 
              style={{ paddingRight: '30px', width: '100%' }}
              placeholder="Enter your email" 
            />
            {emailStatus && (
              <span style={{ 
                position: 'absolute', right: '10px', fontWeight: 'bold', fontSize: '1.2rem',
                color: emailStatus.includes('taken') ? 'red' : 'green' 
              }}>
                {emailStatus.includes('taken') ? '❌' : '✅'}
              </span>
            )}
          </div>
          {emailStatus && <small style={{ color: emailStatus.includes('taken') ? 'red' : 'green', marginTop: '4px', display: 'block' }}>{emailStatus}</small>}
        </div>
        <div className="form-group">
          <label>Password</label>
          <input 
            type="password" 
            value={password} 
            onChange={(e) => setPassword(e.target.value)} 
            required 
            placeholder="Create a password" 
          />
        </div>
        <button type="submit" className="btn" style={{ width: '100%', marginTop: '1rem' }} disabled={loading}>
          {loading ? 'Processing...' : 'Sign Up'}
        </button>
      </form>
      <p style={{ marginTop: '1.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        Already have an account? <Link to="/login" style={{ color: 'var(--primary-color)' }}>Login</Link>
      </p>
    </div>
  );
}
