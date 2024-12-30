/**
 * Interface for interacting with Bluetooth devices in a Capacitor plugin.
 */
export interface BluetoothPlugin {
  
  /**
   * Checks if Bluetooth permissions are granted on the device.
   * If not, it will request the necessary permissions.
   * 
   * @returns A promise that resolves if permissions are granted, or rejects if permissions are denied.
   */
  checkPermissions(): Promise<void>;

  /**
   * Scans for nearby Bluetooth devices.
   * This method returns a list of discovered devices with their name and address.
   * 
   * @returns A promise that resolves with an array of discovered devices.
   *   Example:
   *   ```typescript
   *   { devices: [{ name: 'Device 1', address: '00:11:22:33:44:55' }, ...] }
   *   ```
   */
  scanForDevices(): Promise<{ devices: { name: string; address: string }[] }>;

  /**
   * Connects to a Bluetooth device using its device address.
   * After successfully connecting, it retrieves the battery level and manufacturer name.
   * 
   * @param options The options for connecting to a device, including the device's address.
   * 
   * @returns A promise that resolves with an object containing:
   *   - `batteryLevel`: The battery level of the connected device.
   *   - `manufacturerName`: The manufacturer name of the connected device.
   * 
   * Example:
   * ```typescript
   * { batteryLevel: 85, manufacturerName: 'Device Manufacturer' }
   * ```
   */
  connectToDevice(options: { deviceAddress: string }): Promise<{ batteryLevel: number; manufacturerName: string }>;

  /**
   * Disconnects the currently connected Bluetooth device.
   * 
   * @returns A promise that resolves when the device is successfully disconnected.
   */
  disconnectDevice(): Promise<void>;
}
