import { WebPlugin } from '@capacitor/core';

import type { BluetoothPlugin } from './definitions';

export class BluetoothWeb extends WebPlugin implements BluetoothPlugin {
  checkPermissions(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  scanForDevices(): Promise<{ devices: { name: string; address: string; }[]; }> {
    throw new Error('Method not implemented.');
  }
  connectToDevice(_options: { deviceAddress: string; }): Promise<{ batteryLevel: number; manufacturerName: string; }> {
    throw new Error('Method not implemented.');
  }
  disconnectDevice(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
