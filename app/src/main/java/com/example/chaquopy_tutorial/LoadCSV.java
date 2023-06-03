package com.example.chaquopy_tutorial;


import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;


public class LoadCSV extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_csv);
        Button BackButton = (Button) findViewById(R.id.button_back);
        LineChart lineChart = (LineChart) findViewById(R.id.line_chart);
        String receivedFilename = getIntent().getStringExtra("FILENAME_KEY");

        ArrayList<String[]> csvData = new ArrayList<>();

        File tmpDir = new File("/sdcard/csv_dir/" + receivedFilename + ".csv");
        if (!tmpDir.exists()) {
            finish();
        }

        csvData = CsvRead("/sdcard/csv_dir/" + receivedFilename + ".csv");

        LineDataSet lineDataSet = new LineDataSet(DataValuesN(csvData), "N");
//        LineDataSet lineDataSet2 = new LineDataSet(DataValues(csvData, 1), "Y value");
//        LineDataSet lineDataSet3 = new LineDataSet(DataValues(csvData, 2), "Z value");
//        lineDataSet1.setColor(Color.RED);
//        lineDataSet2.setColor(Color.GREEN);
//        lineDataSet3.setColor(Color.BLUE);
//        lineDataSet1.setCircleColor(Color.RED);
//        lineDataSet2.setCircleColor(Color.GREEN);
//        lineDataSet3.setCircleColor(Color.BLUE);

        ArrayList<ILineDataSet> dataSet = new ArrayList<>();
        dataSet.add(lineDataSet);
        LineData data = new LineData(dataSet);
        lineChart.setData(data);
        lineChart.invalidate();


        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickBack();
            }
        });
    }

    private void ClickBack() {
        finish();

    }

    private ArrayList<String[]> CsvRead(String path) {
        ArrayList<String[]> CsvData = new ArrayList<>();
        try {
            File file = new File(path);
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] nextline;
            while ((nextline = reader.readNext()) != null) {
                if (nextline != null) {
                    CsvData.add(nextline);

                }
            }

        } catch (Exception e) {
        }
        return CsvData;
    }



    private ArrayList<Entry> DataValues(ArrayList<String[]> csvData, int idx) {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        int counter = 0;
        for (int i = 6; i < csvData.size(); i++) {

            try {
                dataVals.add(new Entry(counter, Float.parseFloat(csvData.get(i)[idx+1])));
            } catch (NumberFormatException e) {
                dataVals.add(new Entry(counter, Float.parseFloat(csvData.get(i)[idx+1].substring(0, 4))));
            }
            counter++;
        }
        return dataVals;
    }

    private ArrayList<Entry> DataValuesN(ArrayList<String[]> csvData) {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        int counter = 0;
        Double N;
        for (int i = 7; i < csvData.size(); i++) {
            N = Math.sqrt(Math.pow(Double.parseDouble(csvData.get(i)[1]), 2) + Math.pow(Double.parseDouble(csvData.get(i)[2]), 2) + Math.pow(Double.parseDouble(csvData.get(i)[3]), 2));

            try {
                dataVals.add(new Entry(counter, Float.parseFloat(String.valueOf(N))));
            } catch (NumberFormatException e) {
                dataVals.add(new Entry(counter, Float.parseFloat(String.valueOf(N).substring(0, 4))));
            }
            counter++;
        }
        return dataVals;
    }
}