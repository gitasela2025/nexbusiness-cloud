import React, { useState, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { sendAiChat, getPendingApprovals, updateApprovalStatus, getUserMemories, saveUserMemory } from './services/api';

function App() {
  const [activeTab, setActiveTab] = useState('chat');
  
  // Chat State
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);

  // Approvals State
  const [approvals, setApprovals] = useState([]);

  // Memories State
  const [memories, setMemories] = useState([]);
  const [newMemory, setNewMemory] = useState('');
  const userId = 1; // Default test user

  useEffect(() => {
    if (activeTab === 'approvals') {
      fetchApprovals();
    } else if (activeTab === 'memories') {
      fetchMemories();
    }
  }, [activeTab]);

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!inputMessage.trim()) return;

    const userMsg = inputMessage;
    setMessages(prev => [...prev, { sender: 'You', text: userMsg }]);
    setInputMessage('');
    setLoading(true);

    try {
      const response = await sendAiChat(userMsg, 'session-1');
      setMessages(prev => [...prev, { sender: 'OmniServe AI', text: typeof response.data === 'string' ? response.data : (response.data.reply || JSON.stringify(response.data)) }]);
    } catch (err) {
      setMessages(prev => [...prev, { sender: 'System', text: 'Error connecting to backend API.' }]);
    } finally {
      setLoading(false);
    }
  };

  const fetchApprovals = async () => {
    try {
      const res = await getPendingApprovals();
      setApprovals(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleApprovalAction = async (id, status) => {
    try {
      await updateApprovalStatus(id, status);
      fetchApprovals();
    } catch (err) {
      console.error(err);
    }
  };

  const fetchMemories = async () => {
    try {
      const res = await getUserMemories(userId);
      setMemories(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleSaveMemory = async (e) => {
    e.preventDefault();
    if (!newMemory.trim()) return;
    try {
      await saveUserMemory(userId, newMemory);
      setNewMemory('');
      fetchMemories();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div style={{ fontFamily: 'Arial, sans-serif', maxWidth: '900px', margin: '20px auto', padding: '20px', background: '#f8f9fa', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}>
      <header style={{ textAlign: 'center', marginBottom: '20px' }}>
        <h1 style={{ color: '#2c3e50' }}>🏢 NexBusiness Cloud - Enterprise AI Suite</h1>
        <p style={{ color: '#7f8c8d' }}>AI-Powered Automation, Approvals & Long-Term Memory</p>
      </header>

      <nav style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginBottom: '20px' }}>
        <button onClick={() => setActiveTab('chat')} style={{ padding: '10px 20px', background: activeTab === 'chat' ? '#3498db' : '#bdc3c7', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>OmniServe AI Chat</button>
        <button onClick={() => setActiveTab('approvals')} style={{ padding: '10px 20px', background: activeTab === 'approvals' ? '#3498db' : '#bdc3c7', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Approval Queue</button>
        <button onClick={() => setActiveTab('memories')} style={{ padding: '10px 20px', background: activeTab === 'memories' ? '#3498db' : '#bdc3c7', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>MemoryCore</button>
      </nav>

      {activeTab === 'chat' && (
        <div style={{ background: 'white', padding: '20px', borderRadius: '6px' }}>
          <h3>💬 OmniServe Agent Chat</h3>
          <div style={{ height: '300px', border: '1px solid #ddd', borderRadius: '4px', padding: '10px', overflowY: 'scroll', marginBottom: '10px', background: '#fff' }}>
            {messages.map((m, idx) => (
              <div key={idx} style={{ margin: '8px 0', textAlign: m.sender === 'You' ? 'right' : 'left' }}>
                <span style={{ background: m.sender === 'You' ? '#3498db' : '#e2e8f0', color: m.sender === 'You' ? 'white' : '#333', padding: '8px 12px', borderRadius: '6px', display: 'inline-block' }}>
                  <strong>{m.sender}:</strong>{m.sender === 'You' ? ' ' + m.text : <ReactMarkdown remarkPlugins={[remarkGfm]}>{m.text}</ReactMarkdown>}
                </span>
              </div>
            ))}
            {loading && <p style={{ color: '#888', fontStyle: 'italic' }}>AI is thinking...</p>}
          </div>
          <form onSubmit={handleSendMessage} style={{ display: 'flex', gap: '10px' }}>
            <input type="text" value={inputMessage} onChange={(e) => setInputMessage(e.target.value)} placeholder="Ask about stock, bookings, or business insights..." style={{ flex: 1, padding: '10px', border: '1px solid #ccc', borderRadius: '4px' }} />
            <button type="submit" style={{ padding: '10px 20px', background: '#2ecc71', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Send</button>
          </form>
        </div>
      )}

      {activeTab === 'approvals' && (
        <div style={{ background: 'white', padding: '20px', borderRadius: '6px' }}>
          <h3>🛡️ Human-in-the-Loop Approval Queue</h3>
          {approvals.length === 0 ? <p>No pending approval requests.</p> : (
            <ul style={{ listStyle: 'none', padding: 0 }}>
              {approvals.map((item) => (
                <li key={item.id} style={{ border: '1px solid #ddd', padding: '15px', marginBottom: '10px', borderRadius: '4px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <p><strong>Action:</strong> {item.actionType}</p>
                    <p><strong>Payload:</strong> {item.payloadJson}</p>
                    <p><strong>Status:</strong> <span style={{ color: item.status === 'PENDING' ? '#e67e22' : '#27ae60' }}>{item.status}</span></p>
                  </div>
                  {item.status === 'PENDING' && (
                    <div style={{ display: 'flex', gap: '10px' }}>
                      <button onClick={() => handleApprovalAction(item.id, 'APPROVED')} style={{ background: '#2ecc71', color: 'white', border: 'none', padding: '8px 12px', borderRadius: '4px', cursor: 'pointer' }}>Approve</button>
                      <button onClick={() => handleApprovalAction(item.id, 'REJECTED')} style={{ background: '#e74c3c', color: 'white', border: 'none', padding: '8px 12px', borderRadius: '4px', cursor: 'pointer' }}>Reject</button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {activeTab === 'memories' && (
        <div style={{ background: 'white', padding: '20px', borderRadius: '6px' }}>
          <h3>🧠 MemoryCore (Long-Term Fact Memory)</h3>
          <form onSubmit={handleSaveMemory} style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
            <input type="text" value={newMemory} onChange={(e) => setNewMemory(e.target.value)} placeholder="Store a long-term fact (e.g., Preferred currency is USD)..." style={{ flex: 1, padding: '10px', border: '1px solid #ccc', borderRadius: '4px' }} />
            <button type="submit" style={{ padding: '10px 20px', background: '#9b59b6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Save Fact</button>
          </form>
          <ul style={{ paddingLeft: '20px' }}>
            {memories.map((mem) => (
              <li key={mem.id} style={{ marginBottom: '8px', color: '#34495e' }}>{mem.memoryFact}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

export default App;
