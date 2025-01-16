package com.mobicloud.plugins.bleconnection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

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

// import java.security.keyFactory;
// import java.security.privateKey;
// import java.security.publicKey;
// import java.security.SecureRandom;
// import java.security.Signature;
// import java.security.spec.PKCS8EncodedKeySpec;
// import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

@CapacitorPlugin(
  name = "BleDeviceConnection",
  permissions = {
    @Permission(alias = "bluetoothPermissions", strings = {
      Manifest.permission.BLUETOOTH_SCAN,
      Manifest.permission.BLUETOOTH_CONNECT,
      Manifest.permission.ACCESS_FINE_LOCATION
    })
  }
)

public class BleDeviceConnectionPlugin extends Plugin
{
    private static final String TAG = "BleDeviceConnection";
    private BluetoothAdapter bluetoothAdapter;
    private final Handler handler = new Handler();
    private boolean scanning = false;
    private BluetoothGatt bluetoothGatt;

    // Define persistence
    MqttClientPersistence persistence = new MemoryPersistence();

    // Enable reconnect
    boolean useReconnect = true;

    // Define max inflight messages
    int maxInflight = 10;

    private Queue<BluetoothGattCharacteristic> readQueue = new LinkedList<>();
    // private static final UUID AUTH_SERVICE_UUID = UUID.fromString("0000auth-0000-1000-8000-00805f9b34fb");
    // private static final UUID CHALLENGE_CHARACTERISTIC_UUID = UUID.fromString("0000c001-0000-1000-8000-00805f9b34fb");
    // private static final UUID RESPONSE_CHARACTERISTIC_UUID = UUID.fromString("0000c002-0000-1000-8000-00805f9b34fb");

    private static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    private static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");

    private static final UUID DEVICE_INFO_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID Device_ID = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");
    private static final UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");
    private static final UUID SERIAL_NUMBER_UUID = UUID.fromString("00002A25-0000-1000-8000-00805F9B34FB");
    private static final UUID SOFTWARE_VERSION_UUID = UUID.fromString("00002A28-0000-1000-8000-00805F9B34FB");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private static final int REQUEST_ENABLE_BT = 1;

    // State to track values
    private Integer batteryLevel = null;
    private String manufacturerName = null;
    private String serialNumber = null;
    private String softwareVersion = null;

    private String modelNumber = null;
    private PluginCall call = null;

    private static final String BROKER_URL = "ssl://platform.iot.tatacommunications.com:8883"; // Replace with your MQTT broker URL and port
    private static final String USERNAME = "fe_regulator"; // Replace with your username
    private static final String PASSWORD = "FeRegulator@123"; // Replace with your password
    private static final String TOPIC_to_Subscribe = "send/command";
    private static final String TOPIC_to_Publish = "devices/status";
    private MqttAndroidClient mqttAndroidClient;
    private Context appcontext;

    @SuppressLint("MissingPermission")
    @Override
    public void load()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.appcontext = this.getActivity().getApplicationContext();

//        String clientId = "FE-Regulator-client";
        mqttAndroidClient = new MqttAndroidClient(this.appcontext, BROKER_URL, MqttClient.generateClientId(), Ack.AUTO_ACK,persistence,useReconnect,maxInflight);

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(USERNAME);
        mqttConnectOptions.setPassword(PASSWORD.toCharArray());
        mqttConnectOptions.setKeepAliveInterval(0);
        mqttConnectOptions.setAutomaticReconnect(true);

        connectToBroker(mqttConnectOptions);
    }

    private void connectToBroker(MqttConnectOptions options)
    {
        String message = "Hello MQTT Broker,from mobicloud!!!";

        mqttAndroidClient.connect(options, getContext(), new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "Connected to broker");
                System.out.println("Connected to MQTT Broker");
                subscribeToTopic(TOPIC_to_Subscribe);
                publishMessage(message);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "Failed to connect to broker", exception);
                System.out.println("Failed to connect to broker");
            }
        });

        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e(TAG, "Connection lost", cause);
                System.out.println("Connection Lost : "+cause);
                try
                {
                    mqttAndroidClient.reconnect();
                }
                catch (MqttException e)
                {
                    System.out.println("Exception Occurred While Reconnecting to MQTT Broker");
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Log.d(TAG, "Message received from topic: " + topic + " - " + new String(message.getPayload()));
                System.out.println("Message received from topic: "+ topic +" - "+new String(message.getPayload()));

                // Use Capacitor bridge to pass messages to JavaScript
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message delivery complete");
                System.out.println("Message Delivery Complete");
            }
        });
    }

    private void subscribeToTopic(String topic) {
        mqttAndroidClient.subscribe(topic, 1, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "Subscribed to topic: " + topic);
                System.out.println("Successfully Subscribed to topic " + topic);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "Failed to subscribe to topic: " + topic, exception);
                System.out.println("Exception occured while subsribing to topic"+exception);
            }
        });
    }

    private void publishMessage(String message) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(message.getBytes());
        mqttMessage.setQos(1); // QoS level (0, 1, or 2)
        mqttAndroidClient.publish(TOPIC_to_Publish, mqttMessage);
        Log.d(TAG, "Message published: " + message);
        System.out.println("Message successfully published "+mqttMessage);
    }

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

    // private PendingIntent getBluetoothSettingsPendingIntent() 
    // {
    //     Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
    //     return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    // }

    @SuppressLint("MissingPermission")
    @PluginMethod
    public void scanForDevices(PluginCall call)
    {
        if (getPermissionState("bluetoothPermissions") != PermissionState.GRANTED) {
            requestPermissionForAlias("bluetoothPermissions", call, "bluetoothPermissionsCallback");
            return;
        }

        if (bluetoothAdapter == null) {
            call.reject("Bluetooth not supported");
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) // Android version < 12
        { 
            if (!bluetoothAdapter.isEnabled()) 
            {
                boolean isEnabled = bluetoothAdapter.enable(); // Directly enable Bluetooth
                if (!isEnabled) {
                    Toast.makeText(getActivity(), "Unable to enable Bluetooth. Please enable it manually.", Toast.LENGTH_SHORT).show();
                    call.reject("Failed to enable Bluetooth. Please enable it manually.");
                    return;
                }
            }
        } 
        else 
        { // Android version >= 12
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                Toast.makeText(getActivity(), "Bluetooth is not enabled. Please enable it and try again.", Toast.LENGTH_SHORT).show();
                call.reject("Bluetooth is not enabled. Please enable it and try again.");
                return;
            }
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
        }, 20000);
    }

    @SuppressLint("MissingPermission")
    @PluginMethod
    public void connectToDevice(PluginCall call)
    {
        String deviceName = call.getString("deviceName");
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

        // Create a bond (pairing) with the device
        // boolean bondingInitiated = device.createBond();
        // if (!bondingInitiated) {
        //     call.reject("Failed to initiate bonding with the device");
        //     return;
        // }

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

                    // BluetoothGattService authService = gatt.getService(AUTH_SERVICE_UUID);
                    // if (authService != null)
                    // {
                    //     BluetoothGattCharacteristic challengeCharacteristic = authService.getCharacteristic(CHALLENGE_CHARACTERISTIC_UUID);

                    //     if (challengeCharacteristic != null)
                    //     {
                    //         generateAndSendChallenge(challengeCharacteristic);
                    //     }
                    // }
                    // else
                    // {
                    //     System.out.println("Authentication Service not found");
                    //     call.reject("Authentication service not found on device");
                    // }

                    if (batteryService != null)
                    {
                        BluetoothGattCharacteristic batteryCharacteristic = batteryService.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC_UUID);
                        if (batteryCharacteristic != null)
                        {
//                            enableNotifications(gatt,batteryCharacteristic);
                            readQueue.add(batteryCharacteristic);
                        }
                    }
                    // Read Manufacturer Name if the service/characteristic exists
                    // System.out.println("Device Info Service"+deviceInfoService.getCharacteristic(Manufacture_name_CHARACTERISTIC_UUID));
                    if (deviceInfoService != null)
                    {
                        BluetoothGattCharacteristic manufacturerCharacteristic = deviceInfoService.getCharacteristic(MANUFACTURER_NAME_CHARACTERISTIC_UUID);
                        BluetoothGattCharacteristic serialNumberCharacteristic = deviceInfoService.getCharacteristic(SERIAL_NUMBER_UUID);
                        BluetoothGattCharacteristic softwareVersionCharacteristic = deviceInfoService.getCharacteristic(SOFTWARE_VERSION_UUID);
                        BluetoothGattCharacteristic modelNumberCharacteristics = deviceInfoService.getCharacteristic(Device_ID);
                        if (manufacturerCharacteristic != null)
                        {
                            readQueue.add(manufacturerCharacteristic);
                        }

                        if (serialNumberCharacteristic != null)
                        {
//                          System.out.println("Firmware Characteristic found"+serialNumberCharacteristic);
                          readQueue.add(serialNumberCharacteristic);
                        }

                        if (softwareVersionCharacteristic != null)
                        {
                          readQueue.add(softwareVersionCharacteristic);
                        }

                        if(modelNumberCharacteristics != null)
                        {
                            readQueue.add(modelNumberCharacteristics);
                        }
                    }
                    readNextCharacteristic();
                }
                else
                {
                    Log.e(TAG, "Service discovery failed with status: " + status);

                }
            }

//            @Override
//            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
//            {
//                if (BATTERY_LEVEL_CHARACTERISTIC_UUID.equals(characteristic.getUuid()))
//                {
//                    int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//                    Log.d(TAG, "Battery Level Updated: " + batteryLevel);
//                    JSObject result = new JSObject();
//                    result.put("batteryLevel", batteryLevel);
//                    notifyListeners("onBatteryLevelChanged", result);
////                    call.resolve(result);
//                }
//            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    // if (RESPONSE_CHARACTERISTIC_UUID.equals(characteristic.getUuid()))
                    // {
                    //     byte[] response = characteristic.getValue();
                    //     if (verifyResponse(response))
                    //     {
                    //         Log.d(TAG, "Device authenticated successfully.");
                    //         JSObject result = new JSObject();
                    //         result.put("authenticated", true);
                    //         call.resolve(result);
                    //     }
                    //     else
                    //     {
                    //         call.reject("Device authentication failed");
                    //     }
                    // }
//                    System.out.println(characteristic);

                    if (BATTERY_LEVEL_CHARACTERISTIC_UUID.equals(characteristic.getUuid()))
                    {
                        batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        Log.d(TAG, "Battery Level: " + batteryLevel);
                    }

                    if (MANUFACTURER_NAME_CHARACTERISTIC_UUID.equals(characteristic.getUuid()))
                    {
                        manufacturerName = characteristic.getStringValue(0);
                        Log.d(TAG, "Manufacturer Name: " + manufacturerName);
                    }

                    if(SERIAL_NUMBER_UUID.equals(characteristic.getUuid()))
                    {
//                        System.out.println("Firmware characteristic found");
                        serialNumber = characteristic.getStringValue(0);
                        Log.d(TAG, "Serial Number: " + serialNumber);
                    }

                    if(SOFTWARE_VERSION_UUID.equals(characteristic.getUuid()))
                    {
                        softwareVersion = characteristic.getStringValue(0);
                        Log.d(TAG, "Software Version" + softwareVersion);
                    }

                    if(Device_ID.equals(characteristic.getUuid()))
                    {
                        modelNumber = characteristic.getStringValue(0);
                    }

                    if (readQueue.isEmpty())
                    {
                        // Send data to MQTT broker
                        try
                        {
//                            String clientId = "android-client-" + System.currentTimeMillis();
//                            mqttAndroidClient = new MqttAndroidClient(appcontext, BROKER_URL, clientId, Ack.AUTO_ACK);
//
//                            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
//                            mqttConnectOptions.setUserName(USERNAME);
//                            mqttConnectOptions.setPassword(PASSWORD.toCharArray());
//
//                            connectToBroker(mqttConnectOptions);

                            JSObject deviceData = new JSObject();
                            deviceData.put("deviceID",modelNumber);
                            deviceData.put("deviceName",deviceName);
                            deviceData.put("macAddress",deviceAddress);
                            deviceData.put("batteryLevel", batteryLevel);
                            deviceData.put("manufacturerName", manufacturerName);
                            deviceData.put("serialNumber",serialNumber);
                            deviceData.put("softwareVersion",softwareVersion);
                            // MqttMessage message = new MqttMessage(deviceData.toString().getBytes());
                            // mqttClient.publish("home/device/data", message);
                            System.out.println("data successfully sent to angular");
//                            JSObject result = new JSObject();
//                            result.put("true",true);
                            System.out.println(deviceData);
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
    }

    @SuppressLint("MissingPermission")
    private void enableNotifications(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        gatt.setCharacteristicNotification(characteristic, true);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
            call.resolve();
        } else {
            call.reject("Descriptor for enabling notifications not found.");
        }
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
