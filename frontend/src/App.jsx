import { useEffect, useState } from 'react';
import { ArrowLeft, LogOut } from 'lucide-react';
import { sendAiChat, getStoredUser, clearSession } from './services/api';
import LoginPage from './components/LoginPage';
import Sidebar from './components/Sidebar';
import ChatPanel from './components/ChatPanel';
import ApprovalsPanel from './components/ApprovalsPanel';
import MemoryPanel from './components/MemoryPanel';
import DocumentsPanel from './components/DocumentsPanel';

const TITLES = {
  chat: 'OmniServe Agent',
  approvals: 'Approval Queue',
  memories: 'MemoryCore',
  documents: 'SecureDoc RAG',
};

// URL hash ("#/approvals") නිසා browser Back / Forward බොත්තම් වැඩ කරයි
function tabFromHash() {
  const name = window.location.hash.replace('#/', '').replace('#', '');
  return TITLES[name] ? name : 'chat';
}

function App() {
  const [user, setUser] = useState(getStoredUser);
  const [activeTab, setActiveTab] = useState(tabFromHash);

  // Chat state lives here so messages survive switching tabs
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);

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

  const logout = () => {
    clearSession();
    setUser(null);
    setMessages([]);
    window.location.hash = '#/chat';
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

  if (!user) return <LoginPage onAuthed={setUser} />;

  const isAdmin = user.role === 'ADMIN';

  return (
    <div className="flex h-screen">
      <Sidebar activeTab={activeTab} onChange={goTo} isAdmin={isAdmin} />

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
          <div className="flex items-center gap-3">
            {activeTab !== 'chat' && (
              <button
                type="button"
                onClick={() => goTo('chat')}
                className="flex items-center gap-1.5 rounded-lg bg-rose-700 px-3 py-1.5 text-xs font-semibold text-white shadow hover:bg-rose-800"
              >
                <ArrowLeft size={14} /> Back to Home
              </button>
            )}
            <h1 className="text-base font-semibold">{TITLES[activeTab]}</h1>
          </div>
          <div className="flex items-center gap-3">
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
              {user.name} · {user.role}
            </span>
            <button type="button" onClick={logout} className="flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-1 text-xs font-semibold text-slate-600 hover:bg-slate-50">
              <LogOut size={13} /> Logout
            </button>
          </div>
        </header>

        <main className="min-h-0 flex-1 overflow-hidden">
          {activeTab === 'chat' && (
            <ChatPanel messages={messages} loading={loading} onSend={handleSend} />
          )}
          {activeTab === 'approvals' && isAdmin && <ApprovalsPanel />}
          {activeTab === 'memories' && <MemoryPanel />}
          {activeTab === 'documents' && <DocumentsPanel isAdmin={isAdmin} />}
        </main>
      </div>
    </div>
  );
}

export default App;
