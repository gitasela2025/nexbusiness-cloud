import { useEffect, useState } from 'react';
import { getUserMemories, saveUserMemory } from '../services/api';

const USER_ID = 1; // Day 15 (JWT) හිදී සැබෑ user id එකෙන් ආදේශ කරමු

export default function MemoryPanel() {
  const [memories, setMemories] = useState([]);
  const [newMemory, setNewMemory] = useState('');
  const [error, setError] = useState('');

  const load = async () => {
    try {
      const res = await getUserMemories(USER_ID);
      setMemories(res.data);
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

  return (
    <div className="h-full overflow-y-auto p-6">
      <div className="mx-auto max-w-3xl">
        <form onSubmit={save} className="mb-5 flex gap-2">
          <input
            type="text"
            value={newMemory}
            onChange={(e) => setNewMemory(e.target.value)}
            placeholder="Store a long-term fact (e.g. Preferred currency is USD)"
            className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100"
          />
          <button
            type="submit"
            className="rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700"
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
            <li key={mem.id} className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-700">
              {mem.memoryFact}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
