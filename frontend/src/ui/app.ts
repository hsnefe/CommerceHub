import { onApiResponse } from '../api/client';
import { getCurrentUser, isAuthenticated, mountHeader, onSessionChange } from './header';
import { mountAuthPanel } from './auth-panel';
import { mountResponsePanel, showResponse } from './response-panel';
import { onWorkflowEvent } from '../state/workflow';

export function mountApp(root: HTMLElement): void {
  root.innerHTML = `
    <div class="layout">
      <div id="header-slot"></div>
      <div id="workflow-slot" class="workflow-strip"></div>
      <div class="main">
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
  const panelSlot = root.querySelector('#panel-slot') as HTMLElement;

  const renderWorkflow = () => {
    const loggedIn = isAuthenticated();
    const user = getCurrentUser();
    workflowSlot.innerHTML = `
      <div class="workflow-step ${loggedIn ? 'done' : 'active'}">
        <span class="step-no">1</span>
        <span>Kayıt / Giriş ${loggedIn && user ? `(${user.email})` : ''}</span>
      </div>
    `;
  };

  mountAuthPanel(panelSlot);
  onSessionChange(() => renderWorkflow());
  onWorkflowEvent((event) => {
    if (event === 'logout') {
      mountAuthPanel(panelSlot);
    }
  });

  renderWorkflow();
}
