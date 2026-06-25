export type WorkflowEvent = 'login-success' | 'logout';

type WorkflowListener = (event: WorkflowEvent) => void;

const listeners = new Set<WorkflowListener>();

export function onWorkflowEvent(listener: WorkflowListener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function emitWorkflowEvent(event: WorkflowEvent): void {
  for (const listener of listeners) {
    listener(event);
  }
}
