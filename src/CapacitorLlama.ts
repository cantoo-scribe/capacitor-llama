import { registerPlugin } from '@capacitor/core';

import type { CapacitorLlamaPlugin  } from './definitions';

const CapacitorLlama = registerPlugin<CapacitorLlamaPlugin>('CapacitorLlama', {
  web: () => import('./web').then((m) => new m.CapacitorLlamaWeb()),
  electron: () => (window as any).CapacitorCustomPlatform.plugins.ElectronLlama
});

export { CapacitorLlama };