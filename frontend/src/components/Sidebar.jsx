import { MessageSquare, ShieldCheck, Brain, FileText, Bot } from 'lucide-react';

const NAV = [
  { id: 'chat', label: 'Chat', icon: MessageSquare },
  { id: 'approvals', label: 'Approvals', icon: ShieldCheck },
  { id: 'memories', label: 'Memory', icon: Brain },
  { id: 'documents', label: 'Documents', icon: FileText, soon: true },
];

export default function Sidebar({ activeTab, onChange }) {
  return (
    <aside className="sidebar-bg flex w-16 shrink-0 flex-col md:w-64">
      <button type="button" onClick={() => onChange('chat')} title="Home" className="flex w-full items-center gap-3 px-4 py-5 text-left">
        <div className="brand-badge flex h-10 w-10 shrink-0 items-center justify-center rounded-xl">
          <Bot size={22} className="text-white" />
        </div>
        <div className="hidden md:block">
          <div className="text-sm font-semibold leading-tight text-white">NexBusiness</div>
          <div className="text-[10px] font-medium uppercase tracking-wider text-amber-300">
            AI Agent Suite
          </div>
        </div>
      </button>

      <div className="hidden px-6 pb-2 pt-3 text-[10px] font-semibold uppercase tracking-widest text-amber-300 md:block">
        Main menu
      </div>

      <nav className="flex flex-1 flex-col gap-1 px-2 md:px-3">
        {NAV.map(({ id, label, icon: Icon, soon }) => {
          const active = activeTab === id;
          return (
            <button
              key={id}
              type="button"
              disabled={soon}
              onClick={() => onChange(id)}
              className={
                'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition ' +
                (active
                  ? 'nav-active font-medium text-white'
                  : soon
                    ? 'cursor-not-allowed text-white/30'
                    : 'text-white/80 hover:bg-white/10')
              }
            >
              <Icon size={18} className="shrink-0" />
              <span className="hidden md:inline">{label}</span>
              {soon && (
                <span className="ml-auto hidden rounded bg-white/10 px-1.5 py-0.5 text-[10px] text-white/50 md:inline">
                  soon
                </span>
              )}
            </button>
          );
        })}
      </nav>

      <div className="hidden border-t border-white/10 p-4 text-xs text-white/50 md:block">
        Spring Boot · React · Groq
      </div>
    </aside>
  );
}
