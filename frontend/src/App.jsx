import { useState } from 'react';
import { sendAiChat } from './services/api';
import Sidebar from './components/Sidebar';
import ChatPanel from './components/ChatPanel';
import ApprovalsPanel from './components/ApprovalsPanel';
import MemoryPanel from './components/MemoryPanel';

const TITLES = {
  chat: 'OmniServe Agent',
  approvals: 'Approval Queue',
  memories: 'MemoryCore',
};

function App() {
  const [activeTab, setActiveTab] = useState('chat');

  // Chat state lives here so messages survive switching tabs
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleSend = async (text) => {
    setMessages((prev) => [...prev, { role: 'user', text }]);
    setLoading(true);
    try {
      const response = await sendAiChat(text, 'session-1');
      const data = response.data;
      const reply = typeof data === 'string' ? data : (data.reply || JSON.stringify(data));
      setMessages((prev) => [...prev, { role: 'ai', text: reply }]);
    } catch (err) {
      console.error(err);
      setMessages((prev) => [...prev, { role: 'system', text: 'Error connecting to backend API.' }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex h-screen">
      <Sidebar activeTab={activeTab} onChange={setActiveTab} />

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
          <h1 className="text-base font-semibold">{TITLES[activeTab]}</h1>
          <span className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-500">
            Demo mode · login coming soon
          </span>
        </header>

        <main className="min-h-0 flex-1 overflow-hidden">
          {activeTab === 'chat' && (
            <ChatPanel messages={messages} loading={loading} onSend={handleSend} />
          )}
          {activeTab === 'approvals' && <ApprovalsPanel />}
          {activeTab === 'memories' && <MemoryPanel />}
        </main>
      </div>
    </div>
  );
}

export default App;
