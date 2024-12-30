package com.bluetooth.plugins.bluetoothdevice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.Plugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

@CapacitorPlugin(
  name = "BluetoothPlugin",
  permissions = {
    @Permission(alias = "bluetoothPermissions", strings = {
      Manifest.permission.BLUETOOTH_SCAN,
      Manifest.permission.BLUETOOTH_CONNECT,
      Manifest.permission.ACCESS_FINE_LOCATION
    })
  }
)

public class BluetoothPlugin extends Plugin
{
    private static final String TAG = "BluetoothPlugin";
    private BluetoothAdapter bluetoothAdapter;
    private final Handler handler = new Handler();
    private boolean scanning = false;
    private BluetoothGatt bluetoothGatt;

    private Queue<BluetoothGattCharacteristic> readQueue = new LinkedList<>();
    private static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    private static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");

    private static final UUID DEVICE_INFO_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;

    // State to track values
    private Integer batteryLevel = null;
    private String manufacturerName = null;
    private PluginCall call = null;

    // MQTT Configuration
    private MqttClient mqttClient;
    private final String mqttBroker = "tcp://192.168.29.253:1883"; // Or use local IP like tcp://192.168.1.100:1883
//    private final String mqttBroker = "tcp://broker.hivemq.com:1883";
    @SuppressLint("MissingPermission")
    @Override
    public void load() {
      bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

//      // Check if Bluetooth is off and prompt the user to enable it
//      if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
//        // Request the user to enable Bluetooth directly
//        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//        getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//      }
    }

    // Handle the result of Bluetooth enable request in onActivityResult (if required)
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//      super.onActivityResult(requestCode, resultCode, data);
//
//      if (requestCode == REQUEST_ENABLE_BT) {
//        if (resultCode == Activity.RESULT_OK) {
//          Log.d(TAG, "Bluetooth enabled successfully");
//          // Bluetooth is enabled, you can continue with your Bluetooth operations
//        } else {
//          Log.d(TAG, "Bluetooth enabling failed");
//          // Handle the case where Bluetooth was not enabled, e.g., show a prompt to the user
//        }
//      }
//    }

    @PluginMethod
    public void checkPermissions(PluginCall call)
    {
        if(getPermissionState("bluetoothPermissions") != PermissionState.GRANTED)
        {
            requestPermissionForAlias("bluetoothPermissions", call, "bluetoothPermissionsCallback");
        }
        else
        {
            call.resolve();
        }
    }

    @PermissionCallback
    private void bluetoothPermissionsCallback(PluginCall call)
    {
        if (getPermissionState("bluetoothPermissions") == PermissionState.GRANTED)
        {
            call.resolve();
        }
        else
        {
            call.reject("Permissions denied");
        }
    }

    @SuppressLint("MissingPermission")
    @PluginMethod
    public void scanForDevices(PluginCall call) {
      if (getPermissionState("bluetoothPermissions") != PermissionState.GRANTED) {
        requestPermissionForAlias("bluetoothPermissions", call, "bluetoothPermissionsCallback");
        return;
      }

      if (bluetoothAdapter == null) {
        call.reject("Bluetooth not supported");
        return;
      }

      if (!bluetoothAdapter.isEnabled()) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        call.reject("Bluetooth is not enabled. Please enable it and try again.");
        return;
      }

      scanning = true;
      JSONArray devicesArray = new JSONArray();
      Set<String> deviceAddresses = new HashSet<>();

      // Register the broadcast receiver for found devices
      Context context = getContext();
      android.content.BroadcastReceiver receiver = new android.content.BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();
          if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
              try {
                if (!deviceAddresses.contains(device.getAddress())) {
                  JSONObject deviceInfo = new JSONObject();
                  if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    deviceInfo.put("name", device.getName());
                    deviceInfo.put("address", device.getAddress());
                    devicesArray.put(deviceInfo);
                    deviceAddresses.add(device.getAddress());
                  }
                }
              } catch (JSONException e) {
                Log.e(TAG, "JSON Exception: " + e.getMessage());
              }
            }
          }
        }
      };

      context.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

      if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        call.reject("Bluetooth scan permission not granted");
        return;
      }

      bluetoothAdapter.startDiscovery();

      // Stop discovery after 10 seconds
      handler.postDelayed(() -> {
        bluetoothAdapter.cancelDiscovery();
        context.unregisterReceiver(receiver);
        JSObject result = new JSObject();
        result.put("devices", devicesArray);
        call.resolve(result);
        scanning = false;
      }, 10000);
    }

    @SuppressLint("MissingPermission")
    @PluginMethod
    public void connectToDevice(PluginCall call)
    {
        String deviceAddress = call.getString("deviceAddress");
        this.batteryLevel = null;
        this.manufacturerName = null;
        if (deviceAddress == null)
        {
            call.reject("No device address provided");
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null)
        {
            call.reject("Device not found");
            return;
        }

        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        bluetoothGatt = device.connectGatt(getContext(), true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                if (newState == BluetoothGatt.STATE_CONNECTED)
                {
                    Log.d(TAG, "Connected to GATT server.");
                    if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                    {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    gatt.discoverServices();
                }
                else if(newState == BluetoothGatt.STATE_DISCONNECTED)
                {
                    Log.d(TAG, "Disconnected from GATT server.");
                    bluetoothGatt.close();
                    call.reject("Unable to connect to selected device");

                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status)
            {
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    Log.d(TAG,"Services Discovered");
                    for (BluetoothGattService service : gatt.getServices())
                    {
                        Log.d(TAG, "Discovered Service UUID: " + service.getUuid());
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics())
                        {
                            Log.d(TAG, "  Discovered Characteristic UUID: " + characteristic.getUuid());
                        }
                    }
                    BluetoothGattService batteryService = gatt.getService(BATTERY_SERVICE_UUID);
                    BluetoothGattService deviceInfoService = gatt.getService(DEVICE_INFO_SERVICE_UUID);

                    if (batteryService != null)
                    {
                        BluetoothGattCharacteristic batteryCharacteristic = batteryService.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC_UUID);
                        if (batteryCharacteristic != null)
                        {
                            readQueue.add(batteryCharacteristic);
                        }
                    }
                    // Read Manufacturer Name if the service/characteristic exists
                    // System.out.println("Device Info Service"+deviceInfoService.getCharacteristic(Manufacture_name_CHARACTERISTIC_UUID));
                    if (deviceInfoService != null)
                    {
                        BluetoothGattCharacteristic manufacturerCharacteristic = deviceInfoService.getCharacteristic(MANUFACTURER_NAME_CHARACTERISTIC_UUID);
                        if (manufacturerCharacteristic != null)
                        {
                            readQueue.add(manufacturerCharacteristic);
                        }
                    }
                    readNextCharacteristic();
                }
                else
                {
                    Log.e(TAG, "Service discovery failed with status: " + status);

                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    if (BATTERY_LEVEL_CHARACTERISTIC_UUID.equals(characteristic.getUuid()))
                    {
                        batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        Log.d(TAG, "Battery Level: " + batteryLevel);
                    }
                    else if (MANUFACTURER_NAME_CHARACTERISTIC_UUID.equals(characteristic.getUuid()))
                    {
                        manufacturerName = characteristic.getStringValue(0);
                        Log.d(TAG, "Manufacturer Name: " + manufacturerName);
                    }

                    if (manufacturerName != null)
                    {
                        // Send data to MQTT broker
                        try
                        {
                            if (mqttClient == null)
                            {
                                mqttClient = new MqttClient(mqttBroker, MqttClient.generateClientId(), null);
                                mqttClient.connect();
                                System.out.println("connected to mqtt");

                            }
                            JSObject deviceData = new JSObject();
                            deviceData.put("batteryLevel", batteryLevel);
                            deviceData.put("manufacturerName", manufacturerName);
                            MqttMessage message = new MqttMessage(deviceData.toString().getBytes());
                            mqttClient.publish("home/device/data", message);
                            System.out.println("data successfully sent to angular");
//                            JSObject result = new JSObject();
//                            result.put("true",true);
                            call.resolve(deviceData);
                        }
                        catch (Exception e)
                        {
                            Log.e(TAG, "Failed to send data to MQTT broker: " + e.getMessage());
                        }
                    }
                }
                readNextCharacteristic();
            }
        });

        //     @Override
        //     public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        //     {
        //         if (status == BluetoothGatt.GATT_SUCCESS)
        //         {
        //             if (BATTERY_LEVEL_CHARACTERISTIC_UUID.equals(characteristic.getUuid()))
        //             {
        //                 batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        //                 Log.d(TAG, "Battery Level: " + batteryLevel);
        //             }
        //             else if (MANUFACTURER_NAME_CHARACTERISTIC_UUID.equals(characteristic.getUuid()))
        //             {
        //                 manufacturerName = characteristic.getStringValue(0);
        //                 Log.d(TAG, "Manufacturer Name: " + manufacturerName);
        //             }

        //             if (batteryLevel != null && manufacturerName != null)
        //             {
        //                 JSObject result = new JSObject();
        //                 result.put("batteryLevel", batteryLevel);
        //                 result.put("manufacturerName", manufacturerName);
        //                 call.resolve(result);
        //             }
        //             else if (batteryLevel == null && manufacturerName != null)
        //             {
        //                 JSObject result = new JSObject();
        //                 result.put("error","Error: Battery Service not detected");
        //                 result.put("batteryLevel", batteryLevel);
        //                 result.put("manufacturerName", manufacturerName);
        //                 call.resolve(result);
        //             }
        //         }
        //         else
        //         {
        //             Log.e(TAG, "Failed to read characteristic: " + characteristic.getUuid());

        //         }

        //         // Proceed to the next characteristic in the queue
        //         readNextCharacteristic();
        //     }
        // });
    }

    @SuppressLint("MissingPermission")
    @PluginMethod
    public void disconnectDevice(PluginCall call)
    {
        if (bluetoothGatt != null)
        {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            {
                call.reject("Bluetooth connect permission not granted");
                return;
            }
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
            call.resolve();
        }
        else
        {
            call.reject("No device connected");
        }
    }

    @SuppressLint("MissingPermission")
    private void readNextCharacteristic()
    {
        if (!readQueue.isEmpty())
        {
            BluetoothGattCharacteristic characteristic = readQueue.poll();
            if (characteristic != null && bluetoothGatt != null)
            {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                bluetoothGatt.readCharacteristic(characteristic);
            }
        }
    }

    public Queue<BluetoothGattCharacteristic> getReadQueue()
    {
        return readQueue;
    }

    public void setReadQueue(Queue<BluetoothGattCharacteristic> readQueue)
    {
        this.readQueue = readQueue;
    }
}
