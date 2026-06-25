import type { ApiResult } from '../api/client';

let panel: HTMLElement | null = null;

export function mountResponsePanel(root: HTMLElement): void {
  panel = document.createElement('aside');
  panel.className = 'response-panel';
  panel.innerHTML = `
    <h2>Yanıt</h2>
    <div class="response-meta">
      <span id="response-status">—</span>
      <span id="response-duration">—</span>
    </div>
    <pre id="response-body" class="response-body">Henüz istek yapılmadı.</pre>
  `;
  root.appendChild(panel);
}

export function showResponse(result: ApiResult): void {
  if (!panel) {
    return;
  }
  const statusEl = panel.querySelector('#response-status');
  const durationEl = panel.querySelector('#response-duration');
  const bodyEl = panel.querySelector('#response-body');
  if (!statusEl || !durationEl || !bodyEl) {
    return;
  }

  statusEl.textContent = `${result.status} ${result.ok ? 'OK' : 'Hata'}`;
  statusEl.className = result.ok ? 'status-ok' : 'status-error';
  durationEl.textContent = `${result.durationMs} ms`;
  bodyEl.textContent = JSON.stringify(result.data, null, 2);
}
