import { MessageSquare, ShieldCheck, Brain, FileText, Bot } from 'lucide-react';

const NAV = [
  { id: 'chat', label: 'Chat', icon: MessageSquare },
  { id: 'approvals', label: 'Approvals', icon: ShieldCheck },
  { id: 'memories', label: 'Memory', icon: Brain },
  { id: 'documents', label: 'Documents', icon: FileText, soon: true },
];

export default function Sidebar({ activeTab, onChange }) {
  return (
    <aside className="flex w-16 shrink-0 flex-col border-r border-rose-200 sidebar-bg md:w-60">
      <div className="flex items-center gap-2 border-b border-slate-200 px-4 py-4">
        <Bot className="shrink-0 text-emerald-600" size={26} />
        <div className="hidden md:block">
          <div className="text-sm font-semibold leading-tight">NexBusiness</div>
          <div className="text-xs text-slate-500">AI Agent Suite</div>
        </div>
      </div>

      <nav className="flex flex-1 flex-col gap-1 p-2">
        {NAV.map(({ id, label, icon: Icon, soon }) => {
          const active = activeTab === id;
          return (
            <button
              key={id}
              type="button"
              disabled={soon}
              onClick={() => onChange(id)}
              className={
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition ' +
                (active
                  ? 'bg-white/80 font-medium text-emerald-800'
                  : soon
                    ? 'cursor-not-allowed text-slate-400'
                    : 'text-slate-600 hover:bg-white/60')
              }
            >
              <Icon size={18} className="shrink-0" />
              <span className="hidden md:inline">{label}</span>
              {soon && (
                <span className="ml-auto hidden rounded bg-slate-100 px-1.5 py-0.5 text-[10px] text-slate-400 md:inline">
                  soon
                </span>
              )}
            </button>
          );
        })}
      </nav>

      <div className="hidden border-t border-slate-200 p-4 text-xs text-slate-500 md:block">
        Spring Boot · React · Groq
      </div>
    </aside>
  );
}
