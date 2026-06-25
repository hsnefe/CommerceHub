import * as authApi from '../api/auth';
import { refreshSession, setSessionUser } from './header';
import { emitWorkflowEvent } from '../state/workflow';
import type { UserResponse } from '../api/auth';

function field(label: string, id: string, type = 'text', value = ''): string {
  return `
    <label for="${id}">
      ${label}
      <input id="${id}" name="${id}" type="${type}" value="${value}" />
    </label>
  `;
}

function section(title: string, body: string): string {
  return `
    <section class="panel-section">
      <h3>${title}</h3>
      ${body}
    </section>
  `;
}

async function completeLogin(email: string, password: string): Promise<boolean> {
  const result = await authApi.login(email, password);
  if (!result.ok) {
    return false;
  }
  await refreshSession();
  emitWorkflowEvent('login-success');
  return true;
}

export function mountAuthPanel(container: HTMLElement): void {
  container.innerHTML = `
    <div id="auth-status" class="flow-banner flow-banner-info">
      Adım 1: Kayıt ol veya giriş yap. Başarılı olunca otomatik olarak kategori adımına geçeceksin.
    </div>
    ${section(
      'Hızlı başlangıç',
      `
      <form id="quick-start-form" class="form-grid">
        ${field('E-posta', 'auth-email', 'email', 'user@example.com')}
        ${field('Şifre', 'auth-password', 'password', 'password123')}
        <div class="button-row">
          <button type="submit" name="action" value="register" class="btn-primary">Kayıt ol ve giriş yap</button>
          <button type="submit" name="action" value="login" class="btn-secondary">Sadece giriş yap</button>
        </div>
      </form>
      <p class="hint">Kayıt sonrası aynı bilgilerle otomatik giriş yapılır; token product-service isteklerinde kullanılır.</p>
    `,
    )}
    ${section(
      'Oturum',
      `
      <div class="button-row">
        <button id="refresh-token" type="button" class="btn-secondary">Token yenile</button>
        <button id="logout" type="button" class="btn-secondary">Çıkış yap</button>
        <button id="me" type="button" class="btn-secondary">Profilimi getir</button>
      </div>
    `,
    )}
  `;

  const statusEl = container.querySelector('#auth-status');

  const setStatus = (message: string, type: 'info' | 'success' | 'error') => {
    if (!statusEl) return;
    statusEl.className = `flow-banner flow-banner-${type}`;
    statusEl.textContent = message;
  };

  container.querySelector('#quick-start-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const submitter = (event as SubmitEvent).submitter as HTMLButtonElement | null;
    const email = (form.querySelector('#auth-email') as HTMLInputElement).value.trim();
    const password = (form.querySelector('#auth-password') as HTMLInputElement).value;
    const action = submitter?.value ?? 'login';

    if (action === 'register') {
      setStatus('Kayıt yapılıyor...', 'info');
      const { registerResult, loginResult } = await authApi.registerAndLogin(email, password);
      if (!registerResult.ok) {
        const message =
          typeof registerResult.data === 'object' &&
          registerResult.data &&
          'message' in registerResult.data
            ? String((registerResult.data as { message: string }).message)
            : 'Kayıt başarısız.';
        setStatus(message, 'error');
        return;
      }
      if (!loginResult?.ok) {
        setStatus('Kayıt tamam ama giriş başarısız. "Sadece giriş yap"ı deneyin.', 'error');
        return;
      }
      setStatus('Kayıt ve giriş tamam.', 'success');
      return;
    }

    setStatus('Giriş yapılıyor...', 'info');
    const ok = await completeLogin(email, password);
    setStatus(
      ok ? 'Giriş başarılı.' : 'Giriş başarısız. Bilgileri kontrol et.',
      ok ? 'success' : 'error',
    );
  });

  container.querySelector('#refresh-token')?.addEventListener('click', async () => {
    const result = await authApi.refresh();
    if (result.ok) {
      await refreshSession();
      setStatus('Token yenilendi.', 'success');
    }
  });

  container.querySelector('#logout')?.addEventListener('click', async () => {
    const result = await authApi.logout();
    if (result.ok) {
      setSessionUser(null);
      emitWorkflowEvent('logout');
      setStatus('Çıkış yapıldı. Yeniden giriş yapabilirsin.', 'info');
    }
  });

  container.querySelector('#me')?.addEventListener('click', async () => {
    const result = await authApi.me();
    if (result.ok) {
      setSessionUser(result.data as UserResponse);
      setStatus('Profil bilgisi güncellendi.', 'success');
    }
  });
}
