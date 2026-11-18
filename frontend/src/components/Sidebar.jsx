import { LayoutDashboard, Bot, FileText, ShieldCheck, Brain, Rocket, LogOut, Cloud } from 'lucide-react';

const NAV = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { id: 'chat', label: 'AI Assistant', icon: Bot },
  { id: 'documents', label: 'Documents', icon: FileText },
  { id: 'approvals', label: 'Approvals', icon: ShieldCheck, adminOnly: true },
  { id: 'memories', label: 'Memory', icon: Brain },
];

function initials(name) {
  const parts = String(name || '?').trim().split(/\s+/);
  return ((parts[0] || '?')[0] + (parts[1] ? parts[1][0] : '')).toUpperCase();
}

export default function Sidebar({ activeTab, onChange, isAdmin, pendingCount, user, onLogout }) {
  const items = NAV.filter((n) => !n.adminOnly || isAdmin);

  return (
    <aside className="sidebar-bg flex w-16 shrink-0 flex-col md:w-64">
      <button
        type="button"
        onClick={() => onChange('dashboard')}
        title="Dashboard"
        className="flex w-full items-center gap-3 px-4 py-6 text-left"
      >
        <div className="brand-badge flex h-10 w-10 shrink-0 items-center justify-center rounded-xl">
          <Cloud size={22} className="text-white" />
        </div>
        <div className="hidden md:block">
          <div className="text-base font-semibold leading-tight text-white">NexBusiness</div>
          <div className="text-xs font-medium text-sky-300">Cloud</div>
        </div>
      </button>

      <nav className="mt-2 flex flex-col gap-1.5 px-2 md:px-4">
        {items.map(({ id, label, icon: Icon }) => {
          const active = activeTab === id;
          const badge = id === 'approvals' && pendingCount > 0 ? pendingCount : null;
          return (
            <button
              key={id}
              type="button"
              onClick={() => onChange(id)}
              className={
                'relative flex items-center gap-3 rounded-xl px-3 py-3 text-sm transition ' +
                (active ? 'nav-active font-medium text-white' : 'text-white/75 hover:bg-white/10 hover:text-white')
              }
            >
              <Icon size={19} className="shrink-0" />
              <span className="hidden md:inline">{label}</span>
              {badge && (
                <span className="ml-auto hidden min-w-5 rounded-full bg-rose-500 px-1.5 py-0.5 text-center text-[11px] font-semibold text-white md:inline">
                  {badge}
                </span>
              )}
              {badge && (
                <span className="absolute right-1.5 top-1.5 h-2.5 w-2.5 rounded-full bg-rose-500 md:hidden" />
              )}
            </button>
          );
        })}
      </nav>

      <div className="flex-1" />

      <div className="mx-4 mb-4 hidden overflow-hidden rounded-2xl bg-gradient-to-br from-indigo-500/40 via-violet-600/40 to-blue-700/40 p-4 ring-1 ring-white/15 md:block">
        <div className="text-sm font-semibold text-white">Powered by AI</div>
        <div className="mt-1 text-xs leading-relaxed text-white/75">
          Work Smarter,
          <br />
          Grow Faster
        </div>
        <Rocket size={44} className="ml-auto mt-2 text-sky-200 drop-shadow-[0_0_12px_rgba(125,211,252,0.7)]" />
      </div>

      <div className="flex items-center gap-3 border-t border-white/10 px-3 py-4 md:px-4">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-violet-600 text-sm font-semibold text-white">
          {initials(user && user.name)}
        </div>
        <div className="hidden min-w-0 flex-1 md:block">
          <div className="truncate text-sm font-medium text-white">{user && user.name}</div>
          <div className="truncate text-xs text-white/55">{user && user.role}</div>
        </div>
        <button
          type="button"
          onClick={onLogout}
          title="Logout"
          className="hidden rounded-lg p-2 text-white/60 hover:bg-white/10 hover:text-white md:block"
        >
          <LogOut size={17} />
        </button>
      </div>
    </aside>
  );
}
