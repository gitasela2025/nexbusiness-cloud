import { useState } from 'react';
import { Bot } from 'lucide-react';
import { login, signup, storeSession } from '../services/api';

export default function LoginPage({ onAuthed }) {
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const res = mode === 'login'
        ? await login(email, password)
        : await signup(name, email, password);
      storeSession(res.data.token, res.data.user);
      onAuthed(res.data.user);
    } catch (err) {
      const msg = err.response && err.response.data && err.response.data.error;
      setError(msg || 'Could not reach the backend.');
    } finally {
      setBusy(false);
    }
  };

  const input = 'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100';

  return (
    <div className="flex h-screen items-center justify-center bg-slate-50 p-4">
      <form onSubmit={submit} className="w-full max-w-sm space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="brand-badge flex h-10 w-10 items-center justify-center rounded-xl">
            <Bot size={22} className="text-white" />
          </div>
          <div>
            <div className="text-sm font-semibold">NexBusiness</div>
            <div className="text-xs text-slate-500">{mode === 'login' ? 'Sign in to continue' : 'Create your account'}</div>
          </div>
        </div>

        {mode === 'signup' && (
          <input className={input} placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} required />
        )}
        <input className={input} type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <input className={input} type="password" placeholder="Password (8+ characters)" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={mode === 'signup' ? 8 : undefined} />

        {error && <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-700">{error}</div>}

        <button type="submit" disabled={busy} className="w-full rounded-lg bg-rose-700 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-800 disabled:opacity-50">
          {busy ? 'Please wait...' : mode === 'login' ? 'Sign in' : 'Sign up'}
        </button>

        <button type="button" onClick={() => { setMode(mode === 'login' ? 'signup' : 'login'); setError(''); }} className="w-full text-center text-xs text-rose-700 hover:underline">
          {mode === 'login' ? "No account? Sign up" : 'Have an account? Sign in'}
        </button>
      </form>
    </div>
  );
}
