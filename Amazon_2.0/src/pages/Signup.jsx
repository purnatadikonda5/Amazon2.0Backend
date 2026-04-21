import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { UserPlus, Mail, Lock, User, AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import { signupUser, checkEmailExists } from '../services/api';

export default function Signup() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailStatus, setEmailStatus] = useState(''); // '', 'checking', 'available', 'taken'
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleEmailBlur = async () => {
    if (!email) return;
    setEmailStatus('checking');
    try {
      const taken = await checkEmailExists(email);
      setEmailStatus(taken ? 'taken' : 'available');
    } catch (e) {
      setEmailStatus('');
    }
  };

  const handleSignup = async (e) => {
    e.preventDefault();
    if (emailStatus === 'taken') return;
    setLoading(true);
    setError('');
    try {
      await signupUser({ name, email, password });
      navigate('/login');
    } catch (e) {
      setError(e.response?.data?.message || 'Error creating account. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '420px', margin: '3rem auto' }} className="animate-slideUp">
      <div className="card" style={{ padding: '2.5rem 2rem' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            width: '52px', height: '52px', borderRadius: '50%',
            background: 'linear-gradient(135deg, var(--accent), var(--primary))',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 1rem'
          }}>
            <UserPlus style={{ color: '#fff', width: 22, height: 22 }} />
          </div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '0.35rem' }}>Create Account</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Join BargainHub and start trading</p>
        </div>

        {/* Error */}
        {error && (
          <div className="alert alert-error animate-fadeIn">
            <AlertCircle className="icon-sm" /> {error}
          </div>
        )}

        <form onSubmit={handleSignup}>
          <div className="form-group">
            <label>Full Name</label>
            <div style={{ position: 'relative' }}>
              <User className="icon-sm" style={{ position: 'absolute', left: '0.85rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                placeholder="Your full name"
                style={{ paddingLeft: '2.5rem' }}
                id="signup-name"
              />
            </div>
          </div>

          <div className="form-group">
            <label>Email Address</label>
            <div style={{ position: 'relative' }}>
              <Mail className="icon-sm" style={{ position: 'absolute', left: '0.85rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="email"
                value={email}
                onChange={(e) => { setEmail(e.target.value); setEmailStatus(''); }}
                onBlur={handleEmailBlur}
                required
                placeholder="you@example.com"
                style={{
                  paddingLeft: '2.5rem', paddingRight: '2.5rem',
                  borderColor: emailStatus === 'taken' ? 'var(--danger)' : emailStatus === 'available' ? 'var(--success)' : undefined
                }}
                id="signup-email"
              />
              {emailStatus === 'available' && (
                <CheckCircle className="icon-sm" style={{ position: 'absolute', right: '0.85rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--success)' }} />
              )}
              {emailStatus === 'taken' && (
                <XCircle className="icon-sm" style={{ position: 'absolute', right: '0.85rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--danger)' }} />
              )}
            </div>
            {emailStatus === 'taken' && (
              <small style={{ color: 'var(--danger)', fontSize: '0.78rem', marginTop: '0.3rem', display: 'block' }}>
                This email is already registered.
              </small>
            )}
            {emailStatus === 'available' && (
              <small style={{ color: 'var(--success)', fontSize: '0.78rem', marginTop: '0.3rem', display: 'block' }}>
                Email is available!
              </small>
            )}
          </div>

          <div className="form-group">
            <label>Password</label>
            <div style={{ position: 'relative' }}>
              <Lock className="icon-sm" style={{ position: 'absolute', left: '0.85rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="Create a strong password"
                style={{ paddingLeft: '2.5rem' }}
                minLength={6}
                id="signup-password"
              />
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-accent btn-lg"
            style={{ width: '100%', marginTop: '0.5rem' }}
            disabled={loading || emailStatus === 'taken'}
            id="signup-submit"
          >
            {loading ? (
              <><div className="spinner spinner-sm" style={{ borderTopColor: '#fff', borderColor: 'rgba(255,255,255,0.3)' }}></div> Creating Account...</>
            ) : 'Create Account'}
          </button>
        </form>

        <p style={{ marginTop: '1.75rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: 'var(--primary)', fontWeight: '600', textDecoration: 'none' }}>Sign in</Link>
        </p>
      </div>
    </div>
  );
}
