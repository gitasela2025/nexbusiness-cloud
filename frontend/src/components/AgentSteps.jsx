import { useState } from 'react';
import {
  ChevronDown,
  ChevronRight,
  Wrench,
  CheckCircle2,
  Clock,
  AlertTriangle,
  XCircle,
} from 'lucide-react';

// Tool result -> one readable line + a tone
function summarize(step) {
  const r = step.result;
  if (r && typeof r === 'object') {
    if (r.error) return { text: r.error, tone: 'error' };
    if (r.requested === true) return { text: `Approval #${r.approvalId} created (${r.status})`, tone: 'pending' };
    if (r.alreadyPending) return { text: `Already waiting for approval (#${r.approvalId})`, tone: 'pending' };
    if (r.placed === true) return { text: `Order #${r.orderId} placed, ${r.remainingStock} left in stock`, tone: 'ok' };
    if (r.created === true) return { text: `Booking #${r.id} created`, tone: 'ok' };
    if (Array.isArray(r.products)) {
      return { text: r.products.map((p) => `${p.name}: ${p.stockQty} in stock`).join(', '), tone: 'ok' };
    }
    if (Array.isArray(r.bookings)) return { text: `${r.count} booking(s) found`, tone: 'ok' };
    if (r.found === false) return { text: r.message || 'Nothing found', tone: 'warn' };
  }
  return { text: typeof r === 'string' ? r : 'Done', tone: 'ok' };
}

function argsText(args) {
  const entries = Object.entries(args || {});
  if (entries.length === 0) return '';
  return entries.map(([k, v]) => `${k}: ${v}`).join(', ');
}

const TONE = {
  ok: { icon: CheckCircle2, text: 'text-emerald-700', dot: 'text-emerald-600' },
  pending: { icon: Clock, text: 'text-amber-700', dot: 'text-amber-600' },
  warn: { icon: AlertTriangle, text: 'text-amber-700', dot: 'text-amber-600' },
  error: { icon: XCircle, text: 'text-rose-700', dot: 'text-rose-600' },
};

export default function AgentSteps({ steps }) {
  const [open, setOpen] = useState(true);
  if (!steps || steps.length === 0) return null;

  return (
    <div className="mb-2 rounded-xl border border-amber-200 bg-amber-50/70 text-xs">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex w-full items-center gap-2 px-3 py-2 font-medium text-slate-700"
      >
        {open ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        <Wrench size={14} className="text-rose-700" />
        Agent steps ({steps.length})
      </button>

      {open && (
        <ol className="flex flex-col gap-2 border-t border-amber-200 px-3 py-2">
          {steps.map((s) => {
            const { text, tone } = summarize(s);
            const t = TONE[tone];
            const Icon = t.icon;
            return (
              <li key={s.step} className="flex gap-2">
                <Icon size={16} className={'mt-0.5 shrink-0 ' + t.dot} />
                <div className="min-w-0 flex-1">
                  <div className="font-mono text-[12px] text-slate-800">
                    {s.tool}
                    <span className="text-slate-500">({argsText(s.args)})</span>
                    {s.needsApproval && (
                      <span className="ml-2 rounded bg-amber-200 px-1.5 py-0.5 font-sans text-[10px] font-medium text-amber-900">
                        needs approval
                      </span>
                    )}
                  </div>
                  <div className={t.text}>{text}</div>
                  <details className="mt-0.5">
                    <summary className="cursor-pointer text-[11px] text-slate-400">raw result</summary>
                    <pre className="mt-1 overflow-x-auto rounded bg-white p-2 text-[11px] text-slate-600">
                      {JSON.stringify(s.result, null, 2)}
                    </pre>
                  </details>
                </div>
              </li>
            );
          })}
        </ol>
      )}
    </div>
  );
}
