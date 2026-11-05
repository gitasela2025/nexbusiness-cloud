import { useEffect, useState } from 'react';
import { Check, X, RefreshCw } from 'lucide-react';
import { getPendingApprovals, updateApprovalStatus } from '../services/api';

const STATUS_STYLE = {
  PENDING: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-emerald-100 text-emerald-800',
  REJECTED: 'bg-rose-100 text-rose-800',
};

const TYPE_LABEL = {
  REFUND: 'Refund',
  PURCHASE_ORDER: 'Purchase order',
};

function parsePayload(raw) {
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

// Payload JSON -> readable title + detail lines
function describe(item, p) {
  if (!p) return { title: 'Request #' + item.id, lines: [] };

  if (item.actionType === 'REFUND') {
    return {
      title: `Refund order #${p.orderId}`,
      lines: [
        `${p.productName ?? 'Product'} × ${p.quantity ?? '?'}`,
        p.amount != null ? `Amount: $${p.amount}` : null,
        p.reason ? `Reason: ${p.reason}` : null,
      ],
    };
  }

  if (item.actionType === 'PURCHASE_ORDER') {
    return {
      title: `Purchase ${p.quantity}× ${p.productName}`,
      lines: [
        p.branch ? `Branch: ${p.branch}` : null,
        p.currentStock != null ? `Current stock: ${p.currentStock}` : null,
        p.reason ? `Reason: ${p.reason}` : null,
      ],
    };
  }

  return { title: item.actionType, lines: [] };
}

export default function ApprovalsPanel() {
  const [approvals, setApprovals] = useState([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = async () => {
    try {
      const res = await getPendingApprovals();
      const list = [...res.data].sort(
        (a, b) =>
          (a.status === 'PENDING' ? 0 : 1) - (b.status === 'PENDING' ? 0 : 1) || b.id - a.id
      );
      setApprovals(list);
      setError('');
    } catch (err) {
      console.error(err);
      setError('Could not load approvals from the backend.');
    }
  };

  useEffect(() => {
    load();
  }, []);

  const act = async (id, status) => {
    try {
      const res = await updateApprovalStatus(id, status);
      setNotice(typeof res.data === 'string' ? res.data : '');
      setError('');
    } catch (err) {
      console.error(err);
      setError('Could not update the approval.');
    }
    load();
  };

  const pendingCount = approvals.filter((a) => a.status === 'PENDING').length;

  return (
    <div className="h-full overflow-y-auto p-6">
      <div className="mx-auto max-w-3xl">
        <div className="mb-4 flex items-center justify-between gap-4">
          <p className="text-sm text-slate-500">
            The AI can only <em>request</em> refunds and purchase orders. Nothing runs until an Admin approves.
            {pendingCount > 0 && (
              <span className="ml-1 font-semibold text-amber-700">{pendingCount} waiting.</span>
            )}
          </p>
          <button
            type="button"
            onClick={load}
            className="flex shrink-0 items-center gap-1 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-50"
          >
            <RefreshCw size={14} /> Refresh
          </button>
        </div>

        {notice && (
          <div className="mb-4 flex items-start justify-between gap-3 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-800">
            <span>{notice}</span>
            <button type="button" onClick={() => setNotice('')} className="text-emerald-600 hover:text-emerald-800">
              <X size={14} />
            </button>
          </div>
        )}

        {error && (
          <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-2 text-sm text-rose-700">
            {error}
          </div>
        )}

        {approvals.length === 0 && !error && (
          <p className="text-sm text-slate-500">No approval requests yet.</p>
        )}

        <ul className="flex flex-col gap-3">
          {approvals.map((item) => {
            const payload = parsePayload(item.payloadJson);
            const { title, lines } = describe(item, payload);
            return (
              <li key={item.id} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-medium uppercase tracking-wide text-slate-400">
                        #{item.id} · {TYPE_LABEL[item.actionType] || item.actionType}
                      </span>
                      <span
                        className={'rounded-full px-2 py-0.5 text-xs font-medium ' + (STATUS_STYLE[item.status] || '')}
                      >
                        {item.status}
                      </span>
                    </div>
                    <h3 className="mt-1 text-base font-semibold text-slate-900">{title}</h3>
                    <ul className="mt-1 text-sm text-slate-600">
                      {lines.filter(Boolean).map((l) => (
                        <li key={l}>{l}</li>
                      ))}
                    </ul>
                    {item.createdAt && (
                      <div className="mt-1 text-xs text-slate-400">
                        Requested by AI · {new Date(/(Z|[+-]\d\d:?\d\d)$/.test(item.createdAt) ? item.createdAt : item.createdAt + 'Z').toLocaleString()}
                      </div>
                    )}
                  </div>

                  {item.status === 'PENDING' && (
                    <div className="flex shrink-0 gap-2">
                      <button
                        type="button"
                        onClick={() => act(item.id, 'APPROVED')}
                        className="flex items-center gap-1 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-700"
                      >
                        <Check size={14} /> Approve
                      </button>
                      <button
                        type="button"
                        onClick={() => act(item.id, 'REJECTED')}
                        className="flex items-center gap-1 rounded-lg bg-rose-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-rose-700"
                      >
                        <X size={14} /> Reject
                      </button>
                    </div>
                  )}
                </div>

                <details className="mt-2">
                  <summary className="cursor-pointer text-[11px] text-slate-400">raw payload</summary>
                  <pre className="mt-1 overflow-x-auto rounded bg-slate-50 p-2 text-[11px] text-slate-600">
                    {payload ? JSON.stringify(payload, null, 2) : item.payloadJson}
                  </pre>
                </details>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}
