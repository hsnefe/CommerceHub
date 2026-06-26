import { isAdmin, isAuthenticated } from './header';

export function renderSessionBanner(): string {
  if (!isAuthenticated()) {
    return `
      <div class="flow-banner flow-banner-error">
        Önce <strong>1. Auth</strong> sekmesinden kayıt ol veya giriş yap. Token olmadan product-service yazma işlemleri çalışmaz.
      </div>
    `;
  }
  if (!isAdmin()) {
    return `
      <div class="flow-banner flow-banner-info">
        Giriş yapıldı — listeleme ve detay çalışır. Oluşturma/güncelleme için kullanıcıya <strong>ADMIN</strong> rolü verip tekrar giriş yap (README SQL).
      </div>
    `;
  }
  return `
    <div class="flow-banner flow-banner-success">
      ADMIN oturumu aktif. Auth token'ın product-service isteklerine otomatik ekleniyor.
    </div>
  `;
}
