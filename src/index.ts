import { registerPlugin } from '@capacitor/core';

import type { BluetoothPlugin } from './definitions';

/**
 * Registers the Bluetooth plugin with Capacitor.
 * This makes the plugin available to use in the app.
 * 
 * @remarks
 * The `BluetoothPlugin` interface is used to define the methods that the plugin exposes.
 * The `registerPlugin` function is used to register the native implementation of the plugin on mobile platforms 
 * (iOS and Android), and optionally provide a web implementation.
 */
const Bluetooth = registerPlugin<BluetoothPlugin>('Bluetooth', {
  // Register the web implementation of the Bluetooth plugin
  web: () => import('./web').then((m) => new m.BluetoothWeb()),
});

/**
 * Export the definitions and the registered plugin instance for use in other parts of the app.
 * This allows other components to interact with the Bluetooth plugin methods.
 */
export * from './definitions';
export { Bluetooth };
