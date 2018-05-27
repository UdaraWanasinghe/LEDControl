package com.udara.developer.ledcontrol;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private BluetoothManager bluetoothManager;
    private Button buttonConnect;
    private Switch switchLED;

    private boolean isConnected;
    private int selectedDevice = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonConnect = findViewById(R.id.buttonConnect);
        switchLED = findViewById(R.id.switchLED);

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isConnected) {
                    if (bluetoothManager.isBluetoothOn()) showConnectBluetoothDialog();

                } else {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Alert");
                    builder.setMessage("Are you sure to disconnect device?");

                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            isConnected = false;
                            bluetoothManager.disconnectDevice();
                        }
                    });

                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

                    builder.create().show();
                }
            }
        });

        switchLED.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isConnected) {
                    if (isChecked) {
                        bluetoothManager.transmitString("T");
                    } else {
                        bluetoothManager.transmitString("F");
                    }
                }
            }
        });

        bluetoothManager = new BluetoothManager(this, new BluetoothManager.BluetoothReceiveListener() {
            @Override
            public void onReceiveData(String data) {
                showToast("Received :" + data);
            }

            @Override
            public void onBluetoothMessage(int message) {
                if (message == BluetoothManager.CONNECTION_SUCCESS) {
                    isConnected = true;
                    buttonConnect.setText("Disconnect");
                    showToast("Connection successful");

                } else if (message == BluetoothManager.CONNECTION_TIMEOUT) {
                    showToast("Connection timeout");

                } else if (message == BluetoothManager.CONNECTION_CLOSED) {
                    showToast("Connection closed");
                    isConnected = false;
                    buttonConnect.setText("Connect");

                } else if (message == BluetoothManager.BLUETOOTH_OFF) {
                    showToast("Bluetooth is off");
                    isConnected = false;
                    buttonConnect.setText("Connect");
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            //bluetooth enabled
            showConnectBluetoothDialog();

        } else if (resultCode == RESULT_CANCELED) {
            //error enabling bluetooth
        }
    }

    //bluetooth connect dialog
    private void showConnectBluetoothDialog() {
        //get paired device list
        final ArrayList<BluetoothDevice> pairedDevices = bluetoothManager.getBondedDeviceList();

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Paired devices");

        builder.setSingleChoiceItems(getDevicesNameList(pairedDevices), selectedDevice, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                selectedDevice = i;
                dialogInterface.dismiss();
                BluetoothDevice device = pairedDevices.get(i);
                showToast("Connecting to : " + device.getName());
                bluetoothManager.connectToDevice(device);
            }
        });

        builder.setPositiveButton("Add devices", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent openBluetoothSettings = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                try {
                    startActivity(openBluetoothSettings);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.create().show();
    }

    //paired devices name list as string array
    private String[] getDevicesNameList(ArrayList<BluetoothDevice> devices) {
        ArrayList<String> nameListArr = new ArrayList<>();

        for (BluetoothDevice device : devices) {
            nameListArr.add(device.getName());
        }

        String[] nameList = new String[nameListArr.size()];
        return nameListArr.toArray(nameList);
    }

    //show toast
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
