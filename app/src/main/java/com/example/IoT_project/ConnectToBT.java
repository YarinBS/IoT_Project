package com.example.IoT_project;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


public class ConnectToBT extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_PERMISSION_BLUETOOTH_CONNECT = 1;
    private BluetoothAdapter bluetoothAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bt_connect);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        ImageButton connectButton = findViewById(R.id.connectButton);
        Button HomeButton = findViewById(R.id.home);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToDevice();
            }
        });

        HomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    protected void onResume() {
        super.onResume();
        showConnectButton();
    }

    private void connectToDevice() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            String defaultDeviceAddress = null;
            String defaultDeviceName = "Test Device YLG";
            defaultDeviceAddress = getDefaultDeviceAddress(defaultDeviceName);

            // Check if the default device is paired
            BluetoothDevice defaultDevice = bluetoothAdapter.getRemoteDevice(defaultDeviceAddress);
            if (defaultDevice != null && defaultDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                // Check if the default device is currently connected
                Toast.makeText(this, "Connecting to default device: " + defaultDevice.getName(), Toast.LENGTH_SHORT).show();

                // Open the new activity here
                Bundle args = new Bundle();
                args.putString("device", defaultDevice.getAddress());
                Fragment fragment = new TerminalFragment();
                fragment.setArguments(args);
                hideConnectButton();
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, "terminal").addToBackStack(null).commit();

            } else {
                if (defaultDevice == null) {
                    Toast.makeText(this, "Default device not found", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Default device is not paired", Toast.LENGTH_SHORT).show();
                }
                // Open the DevicesFragment to display available devices
                hideConnectButton();
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(android.R.id.content, new DevicesFragment());
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        }
    }

    private BluetoothProfile getA2dpProfile() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        final BluetoothProfile[] a2dpProfile = {null};
        adapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile bluetoothProfile) {
                if (profile == BluetoothProfile.A2DP) {
                    // A2DP profile is available
                    a2dpProfile[0] = (BluetoothA2dp) bluetoothProfile;
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.A2DP) {
                    // A2DP profile is disconnected
                    a2dpProfile[0] = null;
                }
            }
        }, BluetoothProfile.A2DP);
        return a2dpProfile[0];
    }


    private String getDefaultDeviceAddress(String defaultDeviceName) {
        if (bluetoothAdapter != null) {
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE && device.getName() != null && device.getName().equals(defaultDeviceName)) {
                    return device.getAddress();
                }
            }
        }
        return null; // Default device not found or not paired
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, continue with your Bluetooth-related logic
                connectToDevice();
            } else {
                // Permission denied, handle the case where the permission is not granted
                Toast.makeText(this, "Default device not found or not paired", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth enabled by the user
                connectToDevice();
            } else {
                // Bluetooth not enabled by the user
                // Handle the case when Bluetooth is not enabled
                Toast.makeText(this, "Bluetooth not enabled, unable to connect to default device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void hideConnectButton() {
        ImageButton connectButton = findViewById(R.id.connectButton);
        connectButton.setVisibility(View.GONE);
        TextView connectText = findViewById(R.id.connectText);
        connectText.setVisibility(View.GONE);

    }

    public void showConnectButton() {
        ImageButton connectButton = findViewById(R.id.connectButton);
        connectButton.setVisibility(View.VISIBLE);
        TextView connectText = findViewById(R.id.connectText);
        connectText.setVisibility(View.VISIBLE);
    }
}