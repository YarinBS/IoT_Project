package com.example.IoT_project;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.lang.Math;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    LineChart mpLineChart;
    LineDataSet lineDataSet;
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

        EditText userName = view.findViewById(R.id.userName);
        EditText numberOfSteps = view.findViewById(R.id.NumberOfSteps);


        buttonSaveRecording.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!userName.getText().toString().isEmpty() && !numberOfSteps.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Save", Toast.LENGTH_SHORT).show();
                    writeToCsv(rows, userName.getText().toString(), numberOfSteps.getText().toString(), stepsSpinner.getSelectedItem().toString());

                    // Also reset
                    LineData data = mpLineChart.getData();
                    ILineDataSet set = data.getDataSetByIndex(0);
                    data.getDataSetByIndex(0);
                    while (set.removeLast()) {
                    }

                    lineDataSet.notifyDataSetChanged(); // let the data know a dataSet changed
                    mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                    mpLineChart.invalidate(); // refresh

                    YAxis leftAxis = mpLineChart.getAxisLeft();
                    YAxis rightAxis = mpLineChart.getAxisRight();
                    XAxis xAxis = mpLineChart.getXAxis();

                    leftAxis.resetAxisMinimum(); // Reset the minimum value of the left y-axis
                    leftAxis.resetAxisMaximum(); // Reset the maximum value of the left y-axis
                    rightAxis.resetAxisMinimum(); // Reset the minimum value of the right y-axis
                    rightAxis.resetAxisMaximum(); // Reset the maximum value of the right y-axis
                    xAxis.resetAxisMinimum(); // Reset the minimum value of the x-axis
                    xAxis.resetAxisMaximum(); // Reset the maximum value of the x-axis

                    counter = 0;
                    steps = 0;
                    // Clear saved records
                    ArrayList<String[]> rows = new ArrayList<>();
                    userName.setText("");
                    numberOfSteps.setText("");
                    stepsSpinner.setSelection(0);
                } else{
                    Toast.makeText(getContext(), "Fill all the necessary values", Toast.LENGTH_SHORT).show();
                }
            }
        });


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

                YAxis leftAxis = mpLineChart.getAxisLeft();
                YAxis rightAxis = mpLineChart.getAxisRight();
                XAxis xAxis = mpLineChart.getXAxis();

                leftAxis.resetAxisMinimum(); // Reset the minimum value of the left y-axis
                leftAxis.resetAxisMaximum(); // Reset the maximum value of the left y-axis
                rightAxis.resetAxisMinimum(); // Reset the minimum value of the right y-axis
                rightAxis.resetAxisMaximum(); // Reset the maximum value of the right y-axis
                xAxis.resetAxisMinimum(); // Reset the minimum value of the x-axis
                xAxis.resetAxisMaximum(); // Reset the maximum value of the x-axis

                counter = 0;
                steps = 0;
                // Clear saved records
                rows = new ArrayList<>();

                // Resetting text fields and mode button
                userName.setText("");
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

                }

            }

        }
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @SuppressLint("DefaultLocale")
    private void writeToCsv(ArrayList<String[]> rows, String userName, String numberOfSteps, String selectedStep) {
        try {
            String path = "/sdcard/csv_dir/";
            String convertUsername = convertUsername(userName);
            String fileName = generateFileName(convertUsername, selectedStep);
            // Check if the directory exists, create if necessary
            File directory = new File(path + "recordings/");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create the new file if it doesn't exist
            File file = new File(directory, fileName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                    // Perform further file operations if needed

                    String csv = path + "recordings/" + fileName;
                    CSVWriter csvWriter = new CSVWriter(new FileWriter(csv, true));
                    String row1[] = new String[]{"NAME: ", userName};
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
                    for (String[] row : rows) {
                        csvWriter.writeNext(row);
                    }
                    csvWriter.close();
                } catch (IOException e) {
                    // Handle file creation error
                    e.printStackTrace();
                }
            }

            File db_directory = new File(path + "databases/");
            if (!db_directory.exists()) {
                db_directory.mkdirs();
            }
            // Create the new file if it doesn't exist
            File db_file = new File(db_directory, convertUsername + ".csv");
            if (!db_file.exists()) {
                try {
                    db_file.createNewFile();
                    String db_csv = path + "databases/" + convertUsername + ".csv";


                    CSVWriter dbWriter = new CSVWriter(new FileWriter(db_csv, true));

                    String[] row1 = new String[]{"Duration Mean: ", ""};
                    dbWriter.writeNext(row1);
                    String[] row2 = new String[]{"Duration STD: ", ""};
                    dbWriter.writeNext(row2);
                    String[] row3 = new String[]{"Estimated Steps Mean: ", ""};
                    dbWriter.writeNext(row3);
                    for (int i = 1; i < 5; i++){
                        String[] row = new String[]{"Step " + i + " Duration Mean: ", ""};
                    dbWriter.writeNext(row);
                    }
                    dbWriter.writeNext(new String[]{});

                    String[] db_columns = new String[]{"training time", "step", "entered steps", "estimated steps", "training duration"};
                    dbWriter.writeNext(db_columns);
                    dbWriter.close();
                } catch (IOException e) {
                    // Handle file creation error
                    e.printStackTrace();
                }


            }

            SimpleDateFormat datetime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();

            String db_csv = path + "databases/" + convertUsername + ".csv";


            CSVWriter dbWriter = new CSVWriter(new FileWriter(db_csv, true));


            // Get the start timestamp
            double startTimestamp = Double.parseDouble(rows.get(0)[0]);
            // Get the current timestamp
            double lastTimestamp = Double.parseDouble(rows.get(rows.size() - 1)[0]);

            double duration = lastTimestamp - startTimestamp;

            String[] new_row = new String[]{datetime.format(date), selectedStep, numberOfSteps, String.valueOf(steps), String.valueOf(duration)};
            dbWriter.writeNext(new_row);

            dbWriter.close();
            CSVReader dbReader = new CSVReader(new FileReader(db_csv));
            List<String[]> dbData = dbReader.readAll();
            dbReader.close();


            String[] row1 = dbData.get(0);
            String[] row2 = dbData.get(1);
            String[] row3 = dbData.get(2);

            row1[1] = String.format("%,.3f", computeMean(db_csv, 4, ""));
            row2[1] = String.format("%,.3f",computeStd(db_csv, computeMean(db_csv, 4, ""),4));
            row3[1] = String.format("%,.3f", computeMean(db_csv, 3,""));
            for (int i = 1; i < 5; i++){
                String[] row = dbData.get(i + 2);
                row[1] = String.format("%,.3f", computeMean(db_csv, 4, "Step " + i));
                dbData.set(i + 2, row);
            }

            CSVWriter writer = new CSVWriter(new FileWriter(db_csv, false));

            dbData.set(0, row1);
            dbData.set(1, row2);
            dbData.set(2, row3);
            writer.writeAll(dbData);
            writer.close();



        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
    }


    private static String generateFileName(String username, String step) {
        String baseFileName = username + "_" + step.toLowerCase().replaceAll(" ", "");;

        // Get the list of files with matching names
        File directory = new File("/sdcard/csv_dir/recordings/");
        File[] files = directory.listFiles();
        int count = 0;
        if (files != null) {
            for (File file : files) {
                String filename = file.getName();
                String pattern = "^" + Pattern.quote(baseFileName) + "_(\\d+)\\.csv$";
                Pattern regex = Pattern.compile(pattern);
                Matcher matcher = regex.matcher(filename);
                if (matcher.matches()) {
                    try {
                        int fileCount = Integer.parseInt(matcher.group(1));
                        count = Math.max(count, fileCount);
                    } catch (NumberFormatException e) {
                        // Handle parsing error
                        e.printStackTrace();
                    }
                }
            }
        }

        // Increment the count and append to the base file name
        count++;

        return baseFileName + "_" + count + ".csv";
    }

    public static String convertUsername(String username) {
        String[] nameParts = username.split(" ");
        StringBuilder convertedName = new StringBuilder();
        for (String namePart : nameParts) {
            String formattedPart = namePart.substring(0, 1).toUpperCase() + namePart.substring(1);
            convertedName.append(formattedPart);
        }
        return convertedName.toString();
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

    private double computeMean(String filePath, int index, String step) {
        double sum = 0;
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            for (int i=0; i<9; i++){
                br.readLine();
            }
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (!Objects.equals(step, "")){
                    if (Objects.equals(values[1].substring(1, values[1].length()-1), step)){
                        sum += Double.parseDouble(values[index].substring(1, values[index].length()-1));
                        count++;
                    }

                }
                else{
                    sum += Double.parseDouble(values[index].substring(1, values[index].length()-1));
                    count++;
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (count > 0) {
            return (sum / count);
        } else {
            return 0;
        }
    }

    private double computeStd(String filePath, double mean, int index) {
        double sum = 0;
        int rowCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String row;
            for (int i=0; i<9; i++){
                br.readLine();
            }
            while ((row = br.readLine()) != null) {
                String[] values = row.split(",");
                if (values.length > index) {
                    double value = Double.parseDouble(values[index].substring(1, values[index].length()-1));
                    sum += (value - mean) * (value - mean);
                    rowCount++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (float) Math.sqrt(sum / rowCount);
    }


}
