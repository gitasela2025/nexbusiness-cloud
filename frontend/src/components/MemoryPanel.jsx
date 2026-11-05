import { useEffect, useState } from 'react';
import { Trash2 } from 'lucide-react';
import { getUserMemories, saveUserMemory, deleteUserMemory } from '../services/api';

const USER_ID = 1; // Day 15 (JWT) හිදී සැබෑ user id එකෙන් ආදේශ කරමු

function formatTime(value) {
  if (!value) return '';
  const iso = /(Z|[+-]\d\d:?\d\d)$/.test(value) ? value : value + 'Z';
  return new Date(iso).toLocaleString();
}

export default function MemoryPanel() {
  const [memories, setMemories] = useState([]);
  const [newMemory, setNewMemory] = useState('');
  const [error, setError] = useState('');

  const load = async () => {
    try {
      const res = await getUserMemories(USER_ID);
      setMemories([...res.data].sort((a, b) => b.id - a.id));
      setError('');
    } catch (err) {
      console.error(err);
      setError('Could not load memories from the backend.');
    }
  };

  useEffect(() => {
    load();
  }, []);

  const save = async (e) => {
    e.preventDefault();
    if (!newMemory.trim()) return;
    try {
      await saveUserMemory(USER_ID, newMemory);
      setNewMemory('');
    } catch (err) {
      console.error(err);
      setError('Could not save the memory.');
    }
    load();
  };

  const remove = async (id) => {
    try {
      await deleteUserMemory(id);
    } catch (err) {
      console.error(err);
      setError('Could not delete the memory.');
    }
    load();
  };

  return (
    <div className="h-full overflow-y-auto p-6">
      <div className="mx-auto max-w-3xl">
        <p className="mb-4 text-sm text-slate-500">
          Facts the agent remembers about you. It saves new ones from chat automatically, and uses
          them in every conversation.
        </p>

        <form onSubmit={save} className="mb-5 flex gap-2">
          <input
            type="text"
            value={newMemory}
            onChange={(e) => setNewMemory(e.target.value)}
            placeholder="Add a fact yourself (e.g. Preferred currency is USD)"
            className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100"
          />
          <button
            type="submit"
            className="rounded-lg bg-rose-700 px-4 py-2 text-sm font-medium text-white hover:bg-rose-800"
          >
            Save fact
          </button>
        </form>

        {error && (
          <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-2 text-sm text-rose-700">
            {error}
          </div>
        )}

        {memories.length === 0 && !error && (
          <p className="text-sm text-slate-500">No memories saved yet.</p>
        )}

        <ul className="flex flex-col gap-2">
          {memories.map((mem) => (
            <li
              key={mem.id}
              className="flex items-start justify-between gap-3 rounded-lg border border-slate-200 bg-white px-4 py-2"
            >
              <div>
                <div className="text-sm text-slate-800">{mem.memoryFact}</div>
                {mem.updatedAt && (
                  <div className="text-xs text-slate-400">{formatTime(mem.updatedAt)}</div>
                )}
              </div>
              <button
                type="button"
                onClick={() => remove(mem.id)}
                title="Forget this fact"
                className="shrink-0 rounded p-1 text-slate-400 hover:bg-rose-50 hover:text-rose-700"
              >
                <Trash2 size={16} />
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
