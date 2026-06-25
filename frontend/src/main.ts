import { mountApp } from './ui/app';
import './styles.css';

const root = document.querySelector<HTMLDivElement>('#app');
if (!root) {
  throw new Error('App root not found');
}

mountApp(root);
