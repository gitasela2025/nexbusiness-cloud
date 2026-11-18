import { useEffect, useMemo, useState } from 'react';
import {
  ShoppingCart,
  Brain,
  CheckCircle2,
  Sparkles,
  Send,
  Bot,
  Clock,
  Zap,
  Package,
  CalendarDays,
  ShieldCheck,
  FolderOpen,
  TrendingUp,
} from 'lucide-react';
import { getUserMemories, getStats } from '../services/api';

const RANGES = [
  { id: '7D', days: 7 },
  { id: '30D', days: 30 },
  { id: '90D', days: 90 },
];

const SUGGESTIONS = [
  'Check current stock levels',
  'Show pending approvals',
  'Find documents about contracts',
];

const TYPE_LABEL = { REFUND: 'Refund', PURCHASE_ORDER: 'Purchase order' };

// Backend timestamps may come without a zone; treat them as UTC
function toDate(value) {
  if (!value) return null;
  const iso = /(Z|[+-]\d\d:?\d\d)$/.test(value) ? value : value + 'Z';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d;
}

function relTime(date) {
  const mins = Math.max(0, Math.round((Date.now() - date.getTime()) / 60000));
  if (mins < 1) return 'just now';
  if (mins < 60) return mins + ' min ago';
  const hrs = Math.round(mins / 60);
  if (hrs < 24) return hrs + ' h ago';
  return Math.round(hrs / 24) + ' d ago';
}

function buildSeries(dates, days) {
  const BUCKETS = 7;
  const end = Date.now();
  const span = days * 86400000;
  const start = end - span;
  const size = span / BUCKETS;
  const counts = Array(BUCKETS).fill(0);
  dates.forEach((d) => {
    const t = d.getTime();
    if (t >= start && t <= end) counts[Math.min(BUCKETS - 1, Math.floor((t - start) / size))] += 1;
  });
  const labels = counts.map((_, i) =>
    new Date(start + i * size).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
  );
  return { counts, labels };
}

function StatCard({ tone, icon: Icon, label, value }) {
  return (
    <div className={tone + ' relative overflow-hidden rounded-3xl p-5 text-white shadow-xl ring-1 ring-white/15'}>
      <div className="flex items-center gap-4">
        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white/90 shadow-lg">
          <Icon size={26} className="text-slate-800" />
        </div>
        <div>
          <div className="text-sm text-white/85">{label}</div>
          <div className="text-3xl font-bold leading-tight">{value}</div>
        </div>
      </div>
      <svg viewBox="0 0 160 40" className="absolute -bottom-1 right-2 h-10 w-36 opacity-40" fill="none">
        <path d="M0 30 C 20 10, 35 36, 55 22 S 90 6, 110 20 S 140 30, 160 8" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
      </svg>
    </div>
  );
}

function ActivityChart({ counts, labels }) {
  const W = 600;
  const H = 220;
  const padL = 36;
  const padR = 14;
  const padT = 12;
  const padB = 28;
  const max = Math.max(4, ...counts);
  const innerW = W - padL - padR;
  const innerH = H - padT - padB;
  const pts = counts.map((v, i) => ({
    x: padL + (i * innerW) / (counts.length - 1),
    y: padT + innerH - (v / max) * innerH,
  }));

  let line = 'M ' + pts[0].x + ' ' + pts[0].y;
  for (let i = 1; i < pts.length; i += 1) {
    const mid = (pts[i - 1].x + pts[i].x) / 2;
    line += ` C ${mid} ${pts[i - 1].y}, ${mid} ${pts[i].y}, ${pts[i].x} ${pts[i].y}`;
  }
  const area = line + ` L ${pts[pts.length - 1].x} ${padT + innerH} L ${pts[0].x} ${padT + innerH} Z`;
  const ticks = [0, 1, 2, 3, 4].map((i) => Math.round((max * i) / 4));

  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="w-full">
      <defs>
        <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.55" />
          <stop offset="100%" stopColor="#6366f1" stopOpacity="0.05" />
        </linearGradient>
      </defs>
      {ticks.map((t, i) => {
        const y = padT + innerH - (i / 4) * innerH;
        return (
          <g key={i}>
            <line x1={padL} x2={W - padR} y1={y} y2={y} stroke="rgba(255,255,255,0.08)" />
            <text x={padL - 8} y={y + 4} textAnchor="end" fontSize="11" fill="rgba(255,255,255,0.6)">
              {t}
            </text>
          </g>
        );
      })}
      <path d={area} fill="url(#areaFill)" />
      <path d={line} fill="none" stroke="#38bdf8" strokeWidth="3" strokeLinecap="round" />
      {pts.map((p, i) => (
        <g key={i}>
          <circle cx={p.x} cy={p.y} r="4.5" fill="#0d1236" stroke="#7dd3fc" strokeWidth="2">
            <title>{labels[i] + ': ' + counts[i]}</title>
          </circle>
          <text x={p.x} y={H - 8} textAnchor="middle" fontSize="11" fill="rgba(255,255,255,0.6)">
            {labels[i]}
          </text>
        </g>
      ))}
    </svg>
  );
}

function Skyline() {
  const bars = [
    [0, 52, 22], [26, 78, 26], [56, 40, 20], [80, 92, 30], [114, 60, 24],
    [142, 104, 28], [174, 70, 22], [200, 88, 26], [230, 50, 22], [256, 72, 24],
  ];
  return (
    <svg viewBox="0 0 290 130" className="h-full w-full" preserveAspectRatio="xMaxYMax meet" fill="none">
      <defs>
        <linearGradient id="bld" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#60a5fa" stopOpacity="0.9" />
          <stop offset="100%" stopColor="#312e81" stopOpacity="0.9" />
        </linearGradient>
      </defs>
      {bars.map(([x, h, w], i) => (
        <rect key={i} x={x} y={130 - h} width={w} height={h} rx="2" fill="url(#bld)" opacity={0.55 + (i % 3) * 0.15} />
      ))}
      <path d="M 10 112 L 80 82 L 130 92 L 200 40 L 262 14" stroke="#67e8f9" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M 244 10 L 266 12 L 258 32" stroke="#67e8f9" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function Dashboard({ user, isAdmin, approvals, messages, onAsk, goTo }) {
  const [range, setRange] = useState('30D');
  const [memories, setMemories] = useState(null);
  const [stats, setStats] = useState(null);
  const [question, setQuestion] = useState('');

  const firstName = String(user.name || 'there').trim().split(/\s+/)[0];

  useEffect(() => {
    let alive = true;
    Promise.allSettled([getUserMemories(user.id), getStats()]).then(([mem, st]) => {
      if (!alive) return;
      setMemories(mem.status === 'fulfilled' && Array.isArray(mem.value.data) ? mem.value.data : null);
      setStats(st.status === 'fulfilled' && st.value.data ? st.value.data : null);
    });
    return () => { alive = false; };
  }, [user.id]);

  const pending = approvals.filter((a) => a.status === 'PENDING').length;

  // Real events: approval requests + saved memories
  const events = useMemo(() => {
    const list = [];
    approvals.forEach((a) => {
      const d = toDate(a.createdAt);
      if (d) {
        list.push({
          key: 'a' + a.id,
          date: d,
          icon: ShieldCheck,
          color: 'bg-orange-500',
          text: `${TYPE_LABEL[a.actionType] || a.actionType} request #${a.id} ${String(a.status).toLowerCase()}`,
        });
      }
    });
    (memories || []).forEach((m) => {
      const d = toDate(m.updatedAt);
      if (d) {
        const fact = String(m.memoryFact || '');
        list.push({
          key: 'm' + m.id,
          date: d,
          icon: Brain,
          color: 'bg-violet-500',
          text: 'Memory saved: ' + (fact.length > 46 ? fact.slice(0, 46) + '…' : fact),
        });
      }
    });
    return list.sort((x, y) => y.date - x.date);
  }, [approvals, memories]);

  const days = RANGES.find((r) => r.id === range).days;
  const series = useMemo(() => buildSeries(events.map((e) => e.date), days), [events, days]);
  const hasData = series.counts.some((c) => c > 0);

  const submit = (e) => {
    e.preventDefault();
    const text = question.trim();
    if (!text) return;
    setQuestion('');
    onAsk(text);
  };

  const quick = [
    { label: 'Check Stock', icon: Package, tone: 'bg-sky-100 text-sky-700', run: () => onAsk('Check current stock levels') },
    { label: 'Book Appointment', icon: CalendarDays, tone: 'bg-emerald-100 text-emerald-700', run: () => onAsk('I would like to book an appointment') },
    isAdmin
      ? { label: 'Manage Approvals', icon: ShieldCheck, tone: 'bg-violet-100 text-violet-700', run: () => goTo('approvals') }
      : { label: 'My Memory', icon: Brain, tone: 'bg-violet-100 text-violet-700', run: () => goTo('memories') },
    { label: 'Search Documents', icon: FolderOpen, tone: 'bg-orange-100 text-orange-700', run: () => goTo('documents') },
  ];

  return (
    <div className="h-full overflow-y-auto pr-1">
      <div className="flex flex-col gap-4 pb-2">
        {/* Welcome banner */}
        <section className="banner-bg relative overflow-hidden rounded-3xl px-8 py-7 text-white">
          <div className="pointer-events-none absolute inset-y-0 right-0 hidden w-1/2 items-end justify-end pr-6 pt-4 md:flex">
            <Skyline />
          </div>
          <div className="relative max-w-lg">
            <h2 className="text-3xl font-bold">Welcome back, {firstName}! 👋</h2>
            <p className="mt-2 text-white/85">Your business is growing. Let&apos;s achieve more together!</p>
            <span className="mt-4 inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs ring-1 ring-white/20">
              <TrendingUp size={14} /> Signed in as {user.role}
            </span>
          </div>
        </section>

        {/* Stat cards (live numbers) */}
        <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard tone="stat-blue" icon={ShoppingCart} label={isAdmin ? 'Total Orders' : 'My Orders'} value={stats ? stats.totalOrders : '—'} />
          <StatCard tone="stat-green" icon={Package} label="Units in Stock" value={stats ? stats.stockUnits : '—'} />
          <StatCard tone="stat-purple" icon={CalendarDays} label={isAdmin ? 'Appointments' : 'My Appointments'} value={stats ? stats.appointments : '—'} />
          <StatCard tone="stat-orange" icon={CheckCircle2} label="Pending Approvals" value={isAdmin ? pending : '—'} />
        </section>

        {/* Chart + AI assistant */}
        <section className="grid gap-4 xl:grid-cols-[1.25fr_1fr]">
          <div className="card-dark rounded-3xl p-5">
            <div className="mb-2 flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="flex items-center gap-2 text-lg font-semibold">
                  <TrendingUp size={20} className="text-sky-300" /> Activity Overview
                </div>
                <div className="text-xs text-white/55">Approval requests and saved memories</div>
              </div>
              <div className="flex rounded-xl bg-white/5 p-1 ring-1 ring-white/10">
                {RANGES.map((r) => (
                  <button
                    key={r.id}
                    type="button"
                    onClick={() => setRange(r.id)}
                    className={
                      'rounded-lg px-3 py-1 text-xs font-semibold transition ' +
                      (range === r.id ? 'bg-blue-600 text-white' : 'text-white/60 hover:text-white')
                    }
                  >
                    {r.id}
                  </button>
                ))}
              </div>
            </div>
            <ActivityChart counts={series.counts} labels={series.labels} />
            {!hasData && (
              <div className="-mt-1 text-center text-xs text-white/50">No activity in this period yet.</div>
            )}
          </div>

          <div className="card-dark relative flex flex-col overflow-hidden rounded-3xl p-5">
            <div className="pointer-events-none absolute -right-10 top-10 h-56 w-56 rounded-full bg-violet-500/25 blur-3xl" />
            <div className="relative flex items-start justify-between">
              <div>
                <div className="flex items-center gap-2 text-lg font-semibold">
                  <Sparkles size={20} className="text-violet-300" /> AI Assistant
                </div>
                <div className="mt-1 text-sm text-white/65">Ask anything about your business...</div>
              </div>
              <button
                type="button"
                onClick={() => goTo('chat')}
                className="rounded-xl bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-blue-500"
              >
                Open chat
              </button>
            </div>

            <div className="relative mt-4 flex flex-1 items-center gap-3">
              <div className="flex flex-1 flex-col gap-2">
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => onAsk(s)}
                    className="w-fit max-w-full rounded-xl bg-white/10 px-3 py-2 text-left text-xs text-white/90 ring-1 ring-white/10 hover:bg-white/20"
                  >
                    {s}
                  </button>
                ))}
              </div>
              <div className="hidden h-28 w-28 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-sky-400/30 to-violet-500/30 shadow-[0_0_40px_rgba(99,102,241,0.6)] ring-1 ring-white/20 sm:flex">
                <Bot size={56} className="text-sky-200" />
              </div>
            </div>

            <form onSubmit={submit} className="relative mt-4 flex items-center gap-2 rounded-full bg-white/5 p-1.5 pl-4 ring-1 ring-indigo-400/50">
              <input
                type="text"
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="Type your question..."
                className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-white/45"
              />
              <button
                type="submit"
                disabled={!question.trim()}
                className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-violet-600 text-white disabled:opacity-40"
              >
                <Send size={16} />
              </button>
            </form>
          </div>
        </section>

        {/* Quick actions + recent activity */}
        <section className="grid gap-4 xl:grid-cols-[1.25fr_1fr]">
          <div className="rounded-3xl bg-white/95 p-5 text-slate-900 shadow-xl">
            <div className="flex items-center gap-2 text-lg font-semibold">
              <Zap size={20} className="text-amber-500" /> Quick Actions
            </div>
            <div className="text-xs text-slate-500">Access your key tools instantly</div>
            <div className="mt-4 grid grid-cols-2 gap-3 md:grid-cols-4">
              {quick.map(({ label, icon: Icon, tone, run }) => (
                <button
                  key={label}
                  type="button"
                  onClick={run}
                  className={tone + ' flex flex-col items-center gap-2 rounded-2xl px-3 py-4 text-xs font-semibold transition hover:-translate-y-0.5 hover:shadow-md'}
                >
                  <Icon size={28} />
                  {label}
                </button>
              ))}
            </div>
          </div>

          <div className="rounded-3xl bg-white/95 p-5 text-slate-900 shadow-xl">
            <div className="flex items-center gap-2 text-lg font-semibold">
              <Clock size={20} className="text-slate-600" /> Recent Activity
            </div>
            <div className="text-xs text-slate-500">Live updates from your business</div>
            {events.length === 0 ? (
              <p className="mt-4 text-sm text-slate-500">Nothing yet. Activity shows up here as you use the agent.</p>
            ) : (
              <ul className="mt-3 divide-y divide-slate-100">
                {events.slice(0, 4).map((ev) => {
                  const Icon = ev.icon;
                  return (
                    <li key={ev.key} className="flex items-center gap-3 py-2.5">
                      <span className={ev.color + ' flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-white'}>
                        <Icon size={15} />
                      </span>
                      <span className="min-w-0 flex-1 truncate text-sm">{ev.text}</span>
                      <span className="shrink-0 text-xs text-slate-400">{relTime(ev.date)}</span>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
