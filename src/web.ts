import { WebPlugin } from '@capacitor/core';

import type { CapacitorLlamaPlugin } from './definitions';

export class CapacitorLlamaWeb extends WebPlugin implements CapacitorLlamaPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
