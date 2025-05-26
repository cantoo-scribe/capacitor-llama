import { registerPlugin } from '@capacitor/core';

import type { CapacitorLlamaPlugin } from './definitions';

const CapacitorLlama = registerPlugin<CapacitorLlamaPlugin>('CapacitorLlama', {
  web: () => import('./web').then((m) => new m.CapacitorLlamaWeb()),
});

export * from './definitions';
export { CapacitorLlama };
