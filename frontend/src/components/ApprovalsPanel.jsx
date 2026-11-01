import { useEffect, useState } from 'react';
import { Check, X, RefreshCw } from 'lucide-react';
import { getPendingApprovals, updateApprovalStatus } from '../services/api';

const STATUS_STYLE = {
  PENDING: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-emerald-100 text-emerald-800',
  REJECTED: 'bg-rose-100 text-rose-800',
};

function prettyPayload(raw) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

export default function ApprovalsPanel() {
  const [approvals, setApprovals] = useState([]);
  const [error, setError] = useState('');

  const load = async () => {
    try {
      const res = await getPendingApprovals();
      setApprovals([...res.data].sort((a, b) => b.id - a.id));
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
      await updateApprovalStatus(id, status);
    } catch (err) {
      console.error(err);
      setError('Could not update the approval.');
    }
    load();
  };

  return (
    <div className="h-full overflow-y-auto p-6">
      <div className="mx-auto max-w-3xl">
        <div className="mb-4 flex items-center justify-between">
          <p className="text-sm text-slate-500">
            The AI can only request refunds and purchase orders. Nothing runs until an Admin approves.
          </p>
          <button
            type="button"
            onClick={load}
            className="flex items-center gap-1 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-50"
          >
            <RefreshCw size={14} /> Refresh
          </button>
        </div>

        {error && (
          <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-2 text-sm text-rose-700">
            {error}
          </div>
        )}

        {approvals.length === 0 && !error && (
          <p className="text-sm text-slate-500">No approval requests yet.</p>
        )}

        <ul className="flex flex-col gap-3">
          {approvals.map((item) => (
            <li key={item.id} className="rounded-xl border border-slate-200 bg-white p-4">
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold">#{item.id} {item.actionType}</span>
                  <span className={'rounded-full px-2 py-0.5 text-xs font-medium ' + (STATUS_STYLE[item.status] || '')}>
                    {item.status}
                  </span>
                </div>
                {item.status === 'PENDING' && (
                  <div className="flex gap-2">
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
              <pre className="mt-3 overflow-x-auto rounded-lg bg-slate-50 p-3 text-xs text-slate-600">
                {prettyPayload(item.payloadJson)}
              </pre>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
