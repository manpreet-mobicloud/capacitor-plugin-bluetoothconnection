import { BleDeviceConnection } from 'capacitor-plugin-bledeviceconnection';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    BleDeviceConnection.echo({ value: inputValue })
}
