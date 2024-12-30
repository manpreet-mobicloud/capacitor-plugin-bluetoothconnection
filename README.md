# bluetooth

Plugin for Bluetooth Connection

## Install

```bash
npm install bluetooth
npx cap sync
```

## API

<docgen-index>

* [`checkPermissions()`](#checkpermissions)
* [`scanForDevices()`](#scanfordevices)
* [`connectToDevice(...)`](#connecttodevice)
* [`disconnectDevice()`](#disconnectdevice)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Interface for interacting with Bluetooth devices in a Capacitor plugin.

### checkPermissions()

```typescript
checkPermissions() => Promise<void>
```

Checks if Bluetooth permissions are granted on the device.
If not, it will request the necessary permissions.

--------------------


### scanForDevices()

```typescript
scanForDevices() => Promise<{ devices: { name: string; address: string; }[]; }>
```

Scans for nearby Bluetooth devices.
This method returns a list of discovered devices with their name and address.

**Returns:** <code>Promise&lt;{ devices: { name: string; address: string; }[]; }&gt;</code>

--------------------


### connectToDevice(...)

```typescript
connectToDevice(options: { deviceAddress: string; }) => Promise<{ batteryLevel: number; manufacturerName: string; }>
```

Connects to a Bluetooth device using its device address.
After successfully connecting, it retrieves the battery level and manufacturer name.

| Param         | Type                                    | Description                                                             |
| ------------- | --------------------------------------- | ----------------------------------------------------------------------- |
| **`options`** | <code>{ deviceAddress: string; }</code> | The options for connecting to a device, including the device's address. |

**Returns:** <code>Promise&lt;{ batteryLevel: number; manufacturerName: string; }&gt;</code>

--------------------


### disconnectDevice()

```typescript
disconnectDevice() => Promise<void>
```

Disconnects the currently connected Bluetooth device.

--------------------

</docgen-api>
