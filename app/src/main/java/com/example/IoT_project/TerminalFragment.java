package com.example.IoT_project;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.lang.Math;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {
    int counter = 0;
    int steps = 0;

    Double N;

    String selectedStep;

    boolean StartFlag = false;
    boolean StopFlag = false;

    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    LineChart mpLineChart;
    LineDataSet lineDataSet;
    //    LineDataSet lineDataSet2;
//    LineDataSet lineDataSet3;
    ArrayList<ILineDataSet> dataSet = new ArrayList<>();
    LineData data;

    ArrayList<String[]> rows = new ArrayList<>();
    ;


    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");

    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        context.bindService(new Intent(context, SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        mpLineChart = (LineChart) view.findViewById(R.id.line_chart);
        lineDataSet = new LineDataSet(emptyDataValues(), "N");

        dataSet.add(lineDataSet);
        data = new LineData(dataSet);
        mpLineChart.setData(data);
        mpLineChart.invalidate();


        Button buttonStartRecording = (Button) view.findViewById(R.id.start);
        Button buttonStopRecording = (Button) view.findViewById(R.id.stop);
        Button buttonResetRecording = (Button) view.findViewById(R.id.reset);
        Button buttonSaveRecording = (Button) view.findViewById(R.id.save);


       Spinner stepsSpinner = view.findViewById(R.id.stepsSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this.getActivity(), R.array.steps_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stepsSpinner.setAdapter(adapter);
        stepsSpinner.setSelection(0);

        stepsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedStep = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        EditText fileName = view.findViewById(R.id.fileName);
        EditText numberOfSteps = view.findViewById(R.id.NumberOfSteps);


        buttonSaveRecording.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!fileName.getText().toString().isEmpty() && !numberOfSteps.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Save", Toast.LENGTH_SHORT).show();
                    writeToCsv(rows, fileName.getText().toString(), numberOfSteps.getText().toString());

                    // Also reset
                    LineData data = mpLineChart.getData();
                    ILineDataSet set = data.getDataSetByIndex(0);
                    data.getDataSetByIndex(0);
                    while (set.removeLast()) {
                    }

                    lineDataSet.notifyDataSetChanged(); // let the data know a dataSet changed
                    mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                    mpLineChart.invalidate(); // refresh

                    counter = 0;
                    steps = 0;
                    // Clear saved records
                    ArrayList<String[]> rows = new ArrayList<>();
                    receiveText.setText("");
                    fileName.setText("");
                    numberOfSteps.setText("");
                    stepsSpinner.setSelection(0);
                } else if (new File(fileName.getText().toString() + ".csv").exists()){
                    Toast.makeText(getContext(), "A file with this name already exists. Please select a different file name", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getContext(), "Fill all the necessary values", Toast.LENGTH_SHORT).show();
                }
            }
        });

//        buttonCsvShow.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                OpenLoadCSV(fileName.getText().toString());
//
//            }
//        });

        buttonStartRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StartFlag = true;
                StopFlag = false;
            }
        });

        buttonStopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StartFlag = false;
                StopFlag = true;
            }
        });

        buttonResetRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear graph
                Toast.makeText(getContext(), "Reset", Toast.LENGTH_SHORT).show();
                LineData data = mpLineChart.getData();
                ILineDataSet set = data.getDataSetByIndex(0);
                data.getDataSetByIndex(0);
                while (set.removeLast()) {
                }


                lineDataSet.notifyDataSetChanged(); // let the data know a dataSet changed
                mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                mpLineChart.invalidate(); // refresh

                counter = 0;
                steps = 0;
                // Clear saved records
                rows = new ArrayList<>();
                receiveText.setText("");

                // Resetting text fields and mode button
                fileName.setText("");
                numberOfSteps.setText("");
                stepsSpinner.setSelection(0);
            }
        });

        return view;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private String[] clean_str(String[] stringsArr) {
        for (int i = 0; i < stringsArr.length; i++) {
            stringsArr[i] = stringsArr[i].replaceAll(" ", "");
        }


        return stringsArr;
    }

    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }


    private void receive(byte[] message) {  // This part gets the Arduino reading

        if (hexEnabled) {
            receiveText.append(TextUtil.toHexString(message) + '\n');
        } else {
            if (StopFlag) {
            }
            if (StartFlag) {


                // All the stuff in receive() goes here
                String msg = new String(message);
                if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                    // don't show CR as ^M if directly before LF
                    String msg_to_save = msg;
                    msg_to_save = msg.replace(TextUtil.newline_crlf, TextUtil.emptyString);
                    // check message length
                    if (msg_to_save.length() > 1) {
                        // split message string by ',' char
                        String[] parts = msg_to_save.split(",");
                        // function to trim blank spaces
                        parts = clean_str(parts);


                        // In case we get a reading like '8.02-0.01' or '8.158.14', we take only the first 4 characters
                        if (parts[2].length() > 5) {
                            parts[2] = parts[2].substring(0, 4);
                        }

                        // parse string values, in this case [0] is tmp & [1] is count (t)
                        String row[] = new String[]{String.valueOf((double) counter / 10), parts[0], parts[1], parts[2]};
                        N = Math.sqrt(Math.pow(Double.parseDouble(parts[0]), 2) + Math.pow(Double.parseDouble(parts[1]), 2) + Math.pow(Double.parseDouble(parts[2]), 2));


                        rows.add(row);
                        double threshold = 10.0;

                        if (N > threshold) {
                            steps++;
                        }


                        // add received values to line dataset for plotting the linechart
                        data.addEntry(new Entry(counter, Float.parseFloat(String.valueOf(N))), 0);
                        lineDataSet.notifyDataSetChanged(); // let the data know a dataSet changed
                        mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                        mpLineChart.invalidate(); // refresh
                        counter += 1;

                    }

                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                    // send msg to function that saves it to csv
                    // special handling if CR and LF come in separate fragments
                    if (pendingNewline && msg.charAt(0) == '\n') {
                        Editable edt = receiveText.getEditableText();
                        if (edt != null && edt.length() > 1)
                            edt.replace(edt.length() - 2, edt.length(), "");
                    }
                    pendingNewline = msg.charAt(msg.length() - 1) == '\r';
                }

            }

        }
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    private void writeToCsv(ArrayList<String[]> rows, String fileName, String numberOfSteps) {
        try {
            //     create new csv unless file already exists
            File file = new File("/sdcard/csv_dir/");
            file.mkdirs();
            String csv = "/sdcard/csv_dir/" + fileName + ".csv";
            CSVWriter csvWriter = new CSVWriter(new FileWriter(csv, true));
            String row1[] = new String[]{"NAME: ", fileName + ".csv"};
            csvWriter.writeNext(row1);
            SimpleDateFormat datetime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            String row2[] = new String[]{"EXPERIMENT TIME: ", datetime.format(date)};
            csvWriter.writeNext(row2);

            String row3[] = new String[]{"STEP TYPE: ", selectedStep};
            csvWriter.writeNext(row3);


            String row4[] = new String[]{"COUNT OF ACTUAL STEPS: ", numberOfSteps};
            csvWriter.writeNext(row4);
            csvWriter.writeNext(new String[]{});
            String row5[] = new String[]{"Time[sec]", "ACC X", "ACC Y", "ACC Z"};
            csvWriter.writeNext(row5);
            for (int i = 0; i < rows.size(); i++) {
                csvWriter.writeNext(rows.get(i));
            }

            csvWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        try {
            receive(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private ArrayList<Entry> emptyDataValues() {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        return dataVals;
    }

    private void OpenLoadCSV(String fileName) {
        Intent intent = new Intent(getContext(), LoadCSV.class);
        intent.putExtra("FILENAME_KEY", fileName);
        startActivity(intent);
    }

}
