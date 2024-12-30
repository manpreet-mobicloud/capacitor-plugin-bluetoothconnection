import { WebPlugin } from '@capacitor/core';

import type { BluetoothPlugin } from './definitions';

export class BluetoothWeb extends WebPlugin implements BluetoothPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
