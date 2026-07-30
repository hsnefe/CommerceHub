import { onApiResponse } from '../api/client';
import { getCurrentUser, isAdmin, isAuthenticated, mountHeader, onSessionChange } from './header';
import { mountAuthPanel } from './auth-panel';
import { mountProductsPanel } from './products-panel';
import { mountCategoriesPanel } from './categories-panel';
import { mountInventoryPanel } from './inventory-panel';
import { mountOrdersPanel } from './orders-panel';
import { mountNotificationsPanel } from './notifications-panel';
import { mountResponsePanel, showResponse } from './response-panel';
import { onWorkflowEvent } from '../state/workflow';

type TabId = 'auth' | 'categories' | 'products' | 'inventory' | 'orders' | 'notifications';

const tabs: { id: TabId; label: string }[] = [
  { id: 'auth', label: '1. Auth' },
  { id: 'categories', label: '2. Categories' },
  { id: 'products', label: '3. Products' },
  { id: 'inventory', label: '4. Inventory' },
  { id: 'orders', label: '5. Orders' },
  { id: 'notifications', label: '6. Notifications' },
];

export function mountApp(root: HTMLElement): void {
  root.innerHTML = `
    <div class="layout">
      <div id="header-slot"></div>
      <div id="workflow-slot" class="workflow-strip"></div>
      <div class="main">
        <nav class="tabs" id="tabs"></nav>
        <div class="workspace">
          <div id="panel-slot" class="panel"></div>
          <div id="response-slot"></div>
        </div>
      </div>
    </div>
  `;

  mountHeader(root.querySelector('#header-slot')!);
  mountResponsePanel(root.querySelector('#response-slot')!);
  onApiResponse(showResponse);

  const workflowSlot = root.querySelector('#workflow-slot')!;
  const tabsNav = root.querySelector('#tabs')!;
  const panelSlot = root.querySelector('#panel-slot') as HTMLElement;
  let activeTab: TabId = 'auth';

  const renderWorkflow = () => {
    const loggedIn = isAuthenticated();
    const admin = isAdmin();
    const user = getCurrentUser();
    workflowSlot.innerHTML = `
      <div class="workflow-step ${loggedIn ? 'done' : activeTab === 'auth' ? 'active' : ''}">
        <span class="step-no">1</span>
        <span>Kayıt / Giriş ${loggedIn && user ? `(${user.email})` : ''}</span>
      </div>
      <div class="workflow-arrow">→</div>
      <div class="workflow-step ${activeTab === 'categories' ? 'active' : loggedIn ? 'ready' : 'locked'}">
        <span class="step-no">2</span>
        <span>Kategoriler</span>
      </div>
      <div class="workflow-arrow">→</div>
      <div class="workflow-step ${activeTab === 'products' ? 'active' : loggedIn ? 'ready' : 'locked'}">
        <span class="step-no">3</span>
        <span>Ürünler</span>
      </div>
      <div class="workflow-arrow">→</div>
      <div class="workflow-step ${activeTab === 'inventory' ? 'active' : loggedIn ? 'ready' : 'locked'}">
        <span class="step-no">4</span>
        <span>Stok ${admin ? '(yazma açık)' : loggedIn ? '(sadece okuma)' : ''}</span>
      </div>
      <div class="workflow-arrow">→</div>
      <div class="workflow-step ${activeTab === 'orders' ? 'active' : loggedIn ? 'ready' : 'locked'}">
        <span class="step-no">5</span>
        <span>Siparişler</span>
      </div>
      <div class="workflow-arrow">→</div>
      <div class="workflow-step ${activeTab === 'notifications' ? 'active' : loggedIn ? 'ready' : 'locked'}">
        <span class="step-no">6</span>
        <span>Bildirimler</span>
      </div>
    `;
  };

  for (const tab of tabs) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'tab';
    button.dataset.tab = tab.id;
    button.textContent = tab.label;
    button.addEventListener('click', () => activateTab(tab.id));
    tabsNav.appendChild(button);
  }

  function activateTab(tabId: TabId): void {
    activeTab = tabId;
    tabsNav.querySelectorAll('.tab').forEach((el) => {
      el.classList.toggle('active', (el as HTMLElement).dataset.tab === tabId);
    });
    panelSlot.innerHTML = '';
    if (tabId === 'auth') {
      mountAuthPanel(panelSlot);
    } else if (tabId === 'products') {
      mountProductsPanel(panelSlot);
    } else if (tabId === 'inventory') {
      mountInventoryPanel(panelSlot);
    } else if (tabId === 'orders') {
      mountOrdersPanel(panelSlot);
    } else if (tabId === 'notifications') {
      mountNotificationsPanel(panelSlot);
    } else {
      mountCategoriesPanel(panelSlot);
    }
    renderWorkflow();
  }

  onSessionChange(() => renderWorkflow());
  onWorkflowEvent((event) => {
    if (event === 'login-success') {
      activateTab('categories');
      return;
    }
    if (event === 'logout') {
      activateTab('auth');
    }
  });

  activateTab('auth');
}
