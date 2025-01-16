import { WebPlugin } from '@capacitor/core';

import type { BleDeviceConnectionPlugin } from './definitions';

export class BleDeviceConnectionWeb extends WebPlugin implements BleDeviceConnectionPlugin {
  checkPermissions(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  scanForDevices(): Promise<{ devices: { name: string; address: string; }[]; }> {
    throw new Error('Method not implemented.');
  }
  connectToDevice(_options: { deviceName: string; deviceAddress: string; }): Promise<{ batteryLevel: number; manufacturerName: string; }> {
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
