import { registerPlugin } from '@capacitor/core';

import type { BleDeviceConnectionPlugin } from './definitions';

const BleDeviceConnection = registerPlugin<BleDeviceConnectionPlugin>('BleDeviceConnection', {
  web: () => import('./web').then((m) => new m.BleDeviceConnectionWeb()),
});

export * from './definitions';
export { BleDeviceConnection };
