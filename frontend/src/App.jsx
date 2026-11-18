import { useEffect, useState } from 'react';
import { Search, Bell, LogOut } from 'lucide-react';
import { sendAiChat, getStoredUser, clearSession, getPendingApprovals } from './services/api';
import LoginPage from './components/LoginPage';
import Sidebar from './components/Sidebar';
import Dashboard from './components/Dashboard';
import ChatPanel from './components/ChatPanel';
import ApprovalsPanel from './components/ApprovalsPanel';
import MemoryPanel from './components/MemoryPanel';
import DocumentsPanel from './components/DocumentsPanel';

const TITLES = {
  dashboard: 'Dashboard',
  chat: 'AI Assistant',
  approvals: 'Approval Queue',
  memories: 'MemoryCore',
  documents: 'SecureDoc RAG',
};

// URL hash ("#/approvals") නිසා browser Back / Forward බොත්තම් වැඩ කරයි
function tabFromHash() {
  const name = window.location.hash.replace('#/', '').replace('#', '');
  return TITLES[name] ? name : 'dashboard';
}

function initials(name) {
  const parts = String(name || '?').trim().split(/\s+/);
  return ((parts[0] || '?')[0] + (parts[1] ? parts[1][0] : '')).toUpperCase();
}

function App() {
  const [user, setUser] = useState(getStoredUser);
  const [activeTab, setActiveTab] = useState(tabFromHash);
  const [search, setSearch] = useState('');
  const [approvals, setApprovals] = useState([]);

  // Chat state lives here so messages survive switching tabs
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);

  const isAdmin = !!user && user.role === 'ADMIN';

  useEffect(() => {
    const onLogout = () => { setUser(null); setMessages([]); };
    window.addEventListener('nex-logout', onLogout);
    return () => window.removeEventListener('nex-logout', onLogout);
  }, []);

  useEffect(() => {
    const onHashChange = () => setActiveTab(tabFromHash());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  // Pending-approval count for the sidebar badge, bell and dashboard (admins only)
  useEffect(() => {
    if (!isAdmin) return undefined;
    let alive = true;
    getPendingApprovals()
      .then((res) => { if (alive) setApprovals(Array.isArray(res.data) ? res.data : []); })
      .catch(() => {});
    return () => { alive = false; };
  }, [isAdmin, activeTab]);

  const logout = () => {
    clearSession();
    setUser(null);
    setMessages([]);
    setApprovals([]);
    window.location.hash = '#/dashboard';
  };

  const goTo = (tab) => {
    window.location.hash = '#/' + tab;
  };

  const handleSend = async (text) => {
    setMessages((prev) => [...prev, { role: 'user', text }]);
    setLoading(true);
    try {
      const response = await sendAiChat(text, 'session-1');
      const data = response.data;
      const reply = typeof data === 'string' ? data : (data.reply ?? JSON.stringify(data));
      const steps = data && Array.isArray(data.steps) ? data.steps : [];
      setMessages((prev) => [...prev, { role: 'ai', text: reply, steps }]);
    } catch (err) {
      console.error(err);
      setMessages((prev) => [...prev, { role: 'system', text: 'Error connecting to backend API.' }]);
    } finally {
      setLoading(false);
    }
  };

  // Send a question to the agent and jump to the chat view
  const ask = (text) => {
    goTo('chat');
    handleSend(text);
  };

  const onSearch = (e) => {
    e.preventDefault();
    const text = search.trim();
    if (!text || loading) return;
    setSearch('');
    ask(text);
  };

  if (!user) return <LoginPage onAuthed={setUser} />;

  const pendingCount = approvals.filter((a) => a.status === 'PENDING').length;

  return (
    <div className="flex h-screen">
      <Sidebar
        activeTab={activeTab}
        onChange={goTo}
        isAdmin={isAdmin}
        pendingCount={pendingCount}
        user={user}
        onLogout={logout}
      />

      <div className="main-bg flex min-w-0 flex-1 flex-col">
        <header className="flex items-center gap-4 px-6 py-4">
          {activeTab !== 'dashboard' && (
            <h1 className="hidden shrink-0 text-lg font-semibold text-white lg:block">{TITLES[activeTab]}</h1>
          )}

          <form
            onSubmit={onSearch}
            className="flex w-full max-w-xl items-center gap-3 rounded-full bg-white/10 px-4 py-2.5 ring-1 ring-white/15 backdrop-blur"
          >
            <Search size={16} className="shrink-0 text-white/70" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder='Ask the agent... (e.g. "Check stock")'
              className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-white/55"
            />
          </form>

          <div className="ml-auto flex items-center gap-3">
            <button
              type="button"
              onClick={() => isAdmin && goTo('approvals')}
              title={isAdmin ? 'Pending approvals' : 'Notifications'}
              className="relative flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white ring-1 ring-white/15 hover:bg-white/20"
            >
              <Bell size={18} />
              {pendingCount > 0 && (
                <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-rose-500 px-1 text-[11px] font-semibold text-white">
                  {pendingCount}
                </span>
              )}
            </button>
            <div
              title={user.name + ' · ' + user.role}
              className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-violet-600 text-sm font-semibold text-white ring-2 ring-white/30"
            >
              {initials(user.name)}
            </div>
            <button
              type="button"
              onClick={logout}
              title="Logout"
              className="flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white ring-1 ring-white/15 hover:bg-white/20 md:hidden"
            >
              <LogOut size={17} />
            </button>
          </div>
        </header>

        <main className="min-h-0 flex-1 overflow-hidden px-6 pb-6">
          {activeTab === 'dashboard' && (
            <Dashboard
              user={user}
              isAdmin={isAdmin}
              approvals={approvals}
              messages={messages}
              onAsk={ask}
              goTo={goTo}
            />
          )}

          {activeTab !== 'dashboard' && (
            <div className="h-full overflow-hidden rounded-3xl bg-[#f5f6fb] text-slate-900 shadow-2xl ring-1 ring-white/20">
              {activeTab === 'chat' && (
                <ChatPanel messages={messages} loading={loading} onSend={handleSend} />
              )}
              {activeTab === 'approvals' && isAdmin && <ApprovalsPanel />}
              {activeTab === 'memories' && <MemoryPanel />}
              {activeTab === 'documents' && <DocumentsPanel isAdmin={isAdmin} />}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

export default App;
