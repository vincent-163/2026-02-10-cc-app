import type { Settings, BufferedEvent } from './types';

export type SseCallback = (event: BufferedEvent) => void;

export function connectSse(
  settings: Settings,
  sessionId: string,
  lastEventId: number,
  onEvent: SseCallback,
  onError: (err: unknown) => void,
): () => void {
  let aborted = false;
  const base = settings.apiUrl.replace(/\/+$/, '');
  const url = `${base}/sessions/${sessionId}/stream${lastEventId ? `?last_event_id=${lastEventId}` : ''}`;

  const controller = new AbortController();

  (async () => {
    try {
      const headers: Record<string, string> = {};
      if (settings.authToken) headers['Authorization'] = `Bearer ${settings.authToken}`;

      const res = await fetch(url, { headers, signal: controller.signal });
      if (!res.ok || !res.body) {
        onError(new Error(`SSE connect failed: ${res.status}`));
        return;
      }

      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let currentEvent = '';
      let currentData = '';
      let currentId = '';

      while (!aborted) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            currentEvent = line.slice(7);
          } else if (line.startsWith('id: ')) {
            currentId = line.slice(4);
          } else if (line.startsWith('data: ')) {
            currentData = line.slice(6);
          } else if (line === '') {
            if (currentEvent && currentData) {
              if (currentEvent !== 'ping') {
                try {
                  const data = JSON.parse(currentData);
                  onEvent({
                    id: parseInt(currentId, 10) || 0,
                    event: currentEvent,
                    data,
                    timestamp: Date.now() / 1000,
                  });
                } catch {
                  // skip malformed JSON
                }
              }
            }
            currentEvent = '';
            currentData = '';
            currentId = '';
          }
        }
      }
    } catch (err) {
      if (!aborted) onError(err);
    }
  })();

  return () => {
    aborted = true;
    controller.abort();
  };
}
