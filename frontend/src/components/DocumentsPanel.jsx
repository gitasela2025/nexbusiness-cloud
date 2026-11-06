import { useEffect, useRef, useState } from 'react';
import { FileText, Upload, Send, ShieldCheck, User, Trash2 } from 'lucide-react';
import { uploadDocument, askDocument, listDocuments, deleteDocument } from '../services/api';

export default function DocumentsPanel({ isAdmin }) {
  const fileRef = useRef(null);
  const role = isAdmin ? 'ADMIN' : 'USER';
  const [namespace, setNamespace] = useState('demo');

  const [file, setFile] = useState(null);
  const [accessRole, setAccessRole] = useState('PUBLIC');
  const [uploadMsg, setUploadMsg] = useState('');
  const [uploading, setUploading] = useState(false);

  const [question, setQuestion] = useState('');
  const [result, setResult] = useState(null);
  const [asking, setAsking] = useState(false);
  const [error, setError] = useState('');

  const [docs, setDocs] = useState([]);

  const loadDocs = async () => {
    try {
      const res = await listDocuments(namespace, role);
      setDocs(res.data);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadDocs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [role, namespace]);

  const handleDelete = async (d) => {
    if (!window.confirm('Delete ' + d.fileName + '? This cannot be undone.')) return;
    try {
      await deleteDocument(d.docId, role);
      loadDocs();
    } catch (e) {
      console.error(e);
      setUploadMsg('Delete failed.');
    }
  };

  const handleUpload = async () => {
    if (!file) return;
    setUploading(true);
    setUploadMsg('');
    try {
      const fd = new FormData();
      fd.append('file', file);
      fd.append('namespace', namespace);
      fd.append('accessRole', accessRole);
      const res = await uploadDocument(fd, role);
      setUploadMsg(res.data);
      loadDocs();
      setFile(null);
      if (fileRef.current) fileRef.current.value = '';
    } catch (e) {
      console.error(e);
      setUploadMsg(e.response && e.response.status === 403 ? 'Only ADMIN can upload documents.' : 'Upload failed. Is the backend running?');
    } finally {
      setUploading(false);
    }
  };

  const handleAsk = async () => {
    if (!question.trim()) return;
    setAsking(true);
    setError('');
    setResult(null);
    try {
      const res = await askDocument(namespace, question, role);
      setResult(res.data);
    } catch (e) {
      console.error(e);
      setError('Error connecting to backend API.');
    } finally {
      setAsking(false);
    }
  };

  return (
    <div className="h-full overflow-y-auto p-6">
      <div className="mx-auto max-w-3xl space-y-5">

        {/* Role label */}
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="text-sm font-semibold">Signed in as {role}</div>
          <div className="text-xs text-slate-500">
            ADMIN can see ADMIN_ONLY documents. Other users cannot. The filter runs in the SQL query.
          </div>
        </div>

        {role === 'ADMIN' ? (
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
            <Upload size={16} /> Upload a PDF
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <input
              ref={fileRef}
              type="file"
              accept="application/pdf"
              onChange={(e) => setFile(e.target.files[0] || null)}
              className="hidden"
            />
            <button
              type="button"
              onClick={() => fileRef.current && fileRef.current.click()}
              className="flex items-center gap-1.5 rounded-lg border border-rose-700 px-3 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-50"
            >
              <Upload size={14} /> Choose PDF
            </button>
            <span className="max-w-[12rem] truncate text-xs text-slate-500">
              {file ? file.name : 'No file chosen'}
            </span>
            <input
              value={namespace}
              onChange={(e) => setNamespace(e.target.value)}
              placeholder="project namespace"
              className="w-36 rounded-lg border border-slate-200 px-3 py-1.5 text-xs"
            />
            <select
              value={accessRole}
              onChange={(e) => setAccessRole(e.target.value)}
              className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs"
            >
              <option value="PUBLIC">PUBLIC</option>
              <option value="ADMIN_ONLY">ADMIN_ONLY</option>
            </select>
            <button
              type="button"
              onClick={handleUpload}
              disabled={!file || uploading}
              className="rounded-lg bg-rose-700 px-4 py-1.5 text-xs font-semibold text-white disabled:opacity-40"
            >
              {uploading ? 'Uploading...' : 'Upload'}
            </button>
          </div>
          {uploadMsg && <div className="mt-3 text-xs text-slate-600">{uploadMsg}</div>}
        </div>

        ) : (
          <div className="rounded-xl border border-dashed border-slate-300 bg-white p-4 text-xs text-slate-500">
            Uploading documents is limited to ADMIN. Switch to ADMIN above to upload.
          </div>
        )}

        {/* Documents list */}
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
            <FileText size={16} /> Documents in "{namespace}" ({docs.length})
          </div>
          {docs.length === 0 ? (
            <div className="text-xs text-slate-500">No documents yet.</div>
          ) : (
            <div className="divide-y divide-slate-100">
              {docs.map((d) => (
                <div key={d.docId} className="flex items-center justify-between gap-3 py-2 text-xs">
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium">{d.fileName}</div>
                    <div className="text-slate-500">
                      {d.docId} · {d.chunks} chunks ·{' '}
                      <span className={d.accessRole === 'ADMIN_ONLY' ? 'font-semibold text-amber-700' : 'text-emerald-700'}>
                        {d.accessRole}
                      </span>
                    </div>
                  </div>
                  {role === 'ADMIN' && (
                    <button
                      type="button"
                      onClick={() => handleDelete(d)}
                      title="Delete"
                      className="flex items-center gap-1 rounded-lg border border-red-200 px-2.5 py-1.5 font-semibold text-red-600 hover:bg-red-50"
                    >
                      <Trash2 size={13} /> Delete
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Ask */}
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
            <FileText size={16} /> Ask your documents
          </div>
          <div className="flex gap-2">
            <input
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleAsk()}
              placeholder="e.g. What is the salary of senior staff?"
              className="min-w-0 flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm"
            />
            <button
              type="button"
              onClick={handleAsk}
              disabled={asking || !question.trim()}
              className="flex items-center gap-1.5 rounded-lg bg-rose-700 px-4 py-2 text-sm font-semibold text-white disabled:opacity-40"
            >
              <Send size={14} /> {asking ? 'Asking...' : 'Ask'}
            </button>
          </div>

          {error && <div className="mt-3 text-xs text-red-600">{error}</div>}

          {result && (
            <div className="mt-4 space-y-3">
              <div className="rounded-lg bg-amber-50 p-3 text-sm">{result.answer}</div>
              {result.sources && result.sources.length > 0 && (
                <div>
                  <div className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Sources
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {result.sources.map((s, i) => (
                      <details key={i} className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs">
                        <summary className="cursor-pointer font-medium text-rose-700">
                          [{i + 1}] {s.fileName}, p. {s.page}
                        </summary>
                        <div className="mt-2 max-w-xl text-slate-600">{s.text}</div>
                      </details>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
