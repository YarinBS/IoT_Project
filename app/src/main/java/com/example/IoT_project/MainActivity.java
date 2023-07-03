package com.example.IoT_project;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonDB = findViewById(R.id.db_button);
        Button buttonRecord = findViewById(R.id.record_button);
        Button buttonStats = findViewById(R.id.stats_button);
        Button buttonExit = findViewById(R.id.exit_button);

        buttonDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Enter Your Full Name");

                // Set up the input field
                final EditText input = new EditText(MainActivity.this);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String fullName = input.getText().toString();
                        fullName = TerminalFragment.convertUsername(fullName);

                        if (!fullName.isEmpty()) {
                            // User entered a name
                            LoadDB(fullName);
                        } else {
                            // User didn't enter a name
                            Toast.makeText(MainActivity.this, "Please enter your full name", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });


        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle record button click here
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(MainActivity.this, ConnectToBT.class);
                    startActivity(intent);
                }


            }
        });

        buttonStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Enter Your Full Name");

                // Set up the input field
                final EditText input = new EditText(MainActivity.this);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String fullName = input.getText().toString();
                        fullName = TerminalFragment.convertUsername(fullName);

                        if (!fullName.isEmpty()) {
                            // User entered a name
                            LoadStats(fullName);
                        } else {
                            // User didn't enter a name
                            Toast.makeText(MainActivity.this, "Please enter your full name", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        buttonExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Are you sure you want to exit?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finishAffinity(); // Close the app
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Do nothing, dismiss the dialog
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.bt_settings) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void LoadDB(String fullName) {
        Intent intent = new Intent(this, LoadDB.class);
        intent.putExtra("FULLNAME_KEY", fullName);
        startActivity(intent);
    }

    private void LoadStats(String fullName) {
        Intent intent = new Intent(this, LoadStats.class);
        intent.putExtra("FULLNAME_KEY", fullName);
        startActivity(intent);
    }
}