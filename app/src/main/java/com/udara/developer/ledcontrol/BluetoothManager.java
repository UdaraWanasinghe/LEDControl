package com.udara.developer.ledcontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;


class BluetoothManager {
    //global values
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final static int REQUEST_ENABLE_BLUETOOTH = 1;

    //bluetooth manager messages
    final static int CONNECTION_SUCCESS = 1;
    final static int CONNECTION_TIMEOUT = 2;
    final static int CONNECTION_CLOSED = 3;
    final static int BLUETOOTH_OFF = 4;

    //activity reference
    private Activity activity = null;

    //bluetooth adapter
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket = null;
    static BluetoothDevice deviceConnected;

    //bluetooth receiver message listener
    private BluetoothReceiveListener bluetoothReceiveListener;

    //bluetooth constructor
    BluetoothManager(Activity activity, BluetoothReceiveListener bluetoothReceiveListener) {
        this.activity = activity;
        this.bluetoothReceiveListener = bluetoothReceiveListener;
        initBluetooth();
    }

    //get bluetooth adapter if bluetooth available
    private void initBluetooth() {
        //get bluetooth adapter
        if (bluetoothAdapter == null) bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    //get list of paired devices
    ArrayList<BluetoothDevice> getBondedDeviceList() {
        if (bluetoothAdapter != null) return new ArrayList<>(bluetoothAdapter.getBondedDevices());
        return new ArrayList<>();
    }

    //check if bluetooth is on if not, request from system to turn on bluetooth
    boolean isBluetoothOn() {
        if (bluetoothAdapter != null) {

            //request to turn on bluetooth if bluetooth is turned off
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBluetooth, REQUEST_ENABLE_BLUETOOTH);
                return false;
            }
        }

        //return true if bluetooth on or not available
        return true;
    }

    //connect to given bluetooth device by opening RfcommSocket
    //Rfcomm - radio frequency communication, a widespread bluetooth protocol
    //Input stream handled by separate thread
    void connectToDevice(final BluetoothDevice deviceToConnect) {
        new Thread() {
            @Override
            public void run() {
                //scan for availability of the device
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceToConnect.getAddress());

                try {
                    //open bluetooth socket
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);

                    //cancel discovery to speed up connection
                    bluetoothAdapter.cancelDiscovery();

                    //connect to device
                    bluetoothSocket.connect();

                    deviceConnected = device;
                    bluetoothReceiveListener.onBluetoothMessage(CONNECTION_SUCCESS);

                    //start input stream
                    startBluetoothReceiver();

                } catch (IOException e) {
                    bluetoothReceiveListener.onBluetoothMessage(CONNECTION_TIMEOUT);
                    e.printStackTrace();
                }
            }
        }.start();

    }

    //close bluetooth socket and disconnect from the device
    void disconnectDevice() {
        new Thread() {
            @Override
            public void run() {
                if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    //method to transmit string
    void transmitString(final String stringToTransmit) {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {

            new Thread() {
                @Override
                public void run() {
                    try {
                        //transmit data
                        OutputStream outputStream = bluetoothSocket.getOutputStream();
                        outputStream.write(stringToTransmit.getBytes());
                        outputStream.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    //method to handle bluetooth socket input stream
    private void startBluetoothReceiver() {

        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            new Thread() {
                @Override
                public void run() {
                    byte[] dataBuffer = new byte[256];
                    int bufferSize;

                    try {
                        InputStream inputStream = bluetoothSocket.getInputStream();
                        while (true) {
                            try {

                                if (inputStream.available() > 0) {

                                    bufferSize = inputStream.read(dataBuffer);

                                    if (bufferSize > 0 && bluetoothReceiveListener != null) {
                                        bluetoothReceiveListener.onReceiveData(
                                                new String(dataBuffer, 0, bufferSize));
                                    }
                                }

                            } catch (IOException e) {
                                bluetoothReceiveListener.onBluetoothMessage(CONNECTION_CLOSED);
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    //get connected device
    BluetoothDevice getDeviceConnected() {
        return deviceConnected;
    }

    //interface to bluetooth receive listener
    public interface BluetoothReceiveListener {
        void onReceiveData(String data);

        void onBluetoothMessage(int message);

    }

}