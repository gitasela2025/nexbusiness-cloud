import { useEffect, useRef, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Send, Bot, User } from 'lucide-react';

const SUGGESTIONS = [
  'How many Paper Cups do we have in stock?',
  'What appointments do we have scheduled?',
  'Paper Cups stock is low, raise a purchase order for 40 units.',
];

function Message({ message }) {
  if (message.role === 'user') {
    return (
      <div className="flex justify-end gap-2">
        <div className="max-w-[80%] rounded-2xl rounded-br-sm bg-rose-600 px-4 py-2 text-sm text-white">
          {message.text}
        </div>
        <User size={20} className="mt-1 shrink-0 text-slate-400" />
      </div>
    );
  }
  if (message.role === 'system') {
    return (
      <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-2 text-sm text-rose-700">
        {message.text}
      </div>
    );
  }
  return (
    <div className="flex gap-2">
      <Bot size={20} className="mt-1 shrink-0 text-rose-600" />
      <div className="md max-w-[85%] overflow-x-auto rounded-2xl rounded-bl-sm border border-slate-200 bg-white px-4 py-2 text-sm">
        <ReactMarkdown remarkPlugins={[remarkGfm]}>{message.text}</ReactMarkdown>
      </div>
    </div>
  );
}

export default function ChatPanel({ messages, loading, onSend }) {
  const [input, setInput] = useState('');
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const submit = (e) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || loading) return;
    setInput('');
    onSend(text);
  };

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 overflow-y-auto px-4 py-6">
        <div className="mx-auto flex max-w-3xl flex-col gap-4">
          {messages.length === 0 && (
            <div className="mt-10 text-center">
              <Bot size={40} className="mx-auto text-rose-600" />
              <h2 className="mt-3 text-lg font-semibold">How can OmniServe help?</h2>
              <p className="mt-1 text-sm text-slate-500">
                Check stock, place orders, book appointments, or request approvals.
              </p>
              <div className="mt-5 flex flex-wrap justify-center gap-2">
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => onSend(s)}
                    className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-600 hover:border-rose-300 hover:text-rose-700"
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((m, i) => (
            <Message key={i} message={m} />
          ))}

          {loading && <div className="text-sm italic text-slate-400">Agent is working...</div>}
          <div ref={endRef} />
        </div>
      </div>

      <form onSubmit={submit} className="border-t border-slate-200 bg-white px-4 py-3">
        <div className="mx-auto flex max-w-3xl gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask about stock, orders, bookings..."
            className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100"
          />
          <button
            type="submit"
            disabled={loading || !input.trim()}
            className="flex items-center gap-1 rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <Send size={16} />
            <span className="hidden sm:inline">Send</span>
          </button>
        </div>
      </form>
    </div>
  );
}
