export interface BluetoothPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
