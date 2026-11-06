import { useState } from 'react';
import { Bot, ShieldCheck, Brain, FileText, Mail, Lock, User, Eye, EyeOff } from 'lucide-react';
import { login, signup, storeSession } from '../services/api';

const FEATURES = [
  { icon: Bot, title: 'AI action agent', text: 'Checks stock, books appointments and raises orders using real tools.' },
  { icon: ShieldCheck, title: 'Human approval', text: 'Refunds and purchase orders wait for an admin to approve.' },
  { icon: FileText, title: 'Secure documents', text: 'Ask your PDFs questions. Restricted files stay hidden by role.' },
  { icon: Brain, title: 'Per-user memory', text: 'The agent remembers your preferences, and you control them.' },
];

function Field({ icon: Icon, right, ...props }) {
  return (
    <div className="relative">
      <Icon size={18} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
      <input
        {...props}
        className="w-full rounded-xl border border-slate-300 bg-white py-3 pl-11 pr-11 text-base outline-none transition focus:border-rose-500 focus:ring-4 focus:ring-rose-100"
      />
      {right}
    </div>
  );
}

export default function LoginPage({ onAuthed }) {
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [show, setShow] = useState(false);
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

  const isLogin = mode === 'login';

  return (
    <div className="flex min-h-screen">
      {/* Brand panel */}
      <div className="sidebar-bg relative hidden w-1/2 flex-col justify-between overflow-hidden p-12 text-white lg:flex">
        <div className="pointer-events-none absolute -right-24 -top-24 h-96 w-96 rounded-full bg-amber-400/20 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-32 -left-20 h-96 w-96 rounded-full bg-rose-400/20 blur-3xl" />

        <div className="relative flex items-center gap-3">
          <div className="brand-badge flex h-12 w-12 items-center justify-center rounded-2xl">
            <Bot size={26} className="text-white" />
          </div>
          <div>
            <div className="text-xl font-semibold leading-tight">NexBusiness</div>
            <div className="text-[11px] font-medium uppercase tracking-widest text-amber-300">AI Agent Suite</div>
          </div>
        </div>

        <div className="relative">
          <h1 className="text-4xl font-bold leading-tight">
            An AI assistant that acts,<br />
            <span className="text-amber-300">with a human in control.</span>
          </h1>
          <p className="mt-4 max-w-md text-white/70">
            Run stock, bookings and document questions from one place, with role-based access built in.
          </p>

          <div className="mt-10 grid max-w-xl grid-cols-1 gap-5 sm:grid-cols-2">
            {FEATURES.map(({ icon: Icon, title, text }) => (
              <div key={title} className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur-sm">
                <Icon size={22} className="mb-2 text-amber-300" />
                <div className="text-sm font-semibold">{title}</div>
                <div className="mt-1 text-xs leading-relaxed text-white/65">{text}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="relative text-xs text-white/50">Spring Boot · React · MySQL · Groq</div>
      </div>

      {/* Form panel */}
      <div className="flex w-full items-center justify-center bg-[#faf7f2] px-6 py-10 lg:w-1/2">
        <form onSubmit={submit} className="w-full max-w-md">
          {/* mobile brand */}
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <div className="brand-badge flex h-11 w-11 items-center justify-center rounded-xl">
              <Bot size={24} className="text-white" />
            </div>
            <div className="text-lg font-semibold">NexBusiness</div>
          </div>

          <h2 className="text-3xl font-bold text-slate-900">
            {isLogin ? 'Welcome back' : 'Create your account'}
          </h2>
          <p className="mt-2 text-slate-500">
            {isLogin ? 'Sign in to continue to your dashboard.' : 'Sign up to start using the agent.'}
          </p>

          <div className="mt-8 space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-lg shadow-slate-200/60">
            {!isLogin && (
              <Field icon={User} type="text" placeholder="Full name" value={name}
                onChange={(e) => setName(e.target.value)} required />
            )}
            <Field icon={Mail} type="email" placeholder="Email address" value={email}
              onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
            <Field
              icon={Lock}
              type={show ? 'text' : 'password'}
              placeholder={isLogin ? 'Password' : 'Password (8+ characters)'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={isLogin ? undefined : 8}
              autoComplete={isLogin ? 'current-password' : 'new-password'}
              right={
                <button type="button" onClick={() => setShow(!show)} tabIndex={-1}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                  title={show ? 'Hide password' : 'Show password'}>
                  {show ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              }
            />

            {error && (
              <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {error}
              </div>
            )}

            <button type="submit" disabled={busy}
              className="w-full rounded-xl bg-gradient-to-r from-rose-700 to-rose-600 px-4 py-3 text-base font-semibold text-white shadow-md shadow-rose-300/50 transition hover:from-rose-800 hover:to-rose-700 disabled:opacity-60">
              {busy ? 'Please wait...' : isLogin ? 'Sign in' : 'Create account'}
            </button>
          </div>

          <div className="mt-6 text-center text-sm text-slate-600">
            {isLogin ? "Don't have an account? " : 'Already have an account? '}
            <button type="button"
              onClick={() => { setMode(isLogin ? 'signup' : 'login'); setError(''); }}
              className="font-semibold text-rose-700 hover:underline">
              {isLogin ? 'Sign up' : 'Sign in'}
            </button>
          </div>

          {!isLogin && (
            <p className="mt-3 text-center text-xs text-slate-400">
              New accounts are created as regular users. Admin access is assigned by the system owner.
            </p>
          )}
        </form>
      </div>
    </div>
  );
}
