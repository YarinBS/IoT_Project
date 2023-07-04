package com.example.IoT_project;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadStats extends AppCompatActivity {


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_stats);
        String receivedUsername = getIntent().getStringExtra("FULLNAME_KEY");
        String directoryPath = "/sdcard/csv_dir/databases/" + receivedUsername + ".csv";

        String name = receivedUsername.replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2");

        File tmpDir = new File(directoryPath);
        if (!tmpDir.exists()) {
            Toast.makeText(this, "No data for this user. Go to record your training!", Toast.LENGTH_SHORT).show();
            finish();
        }

        ArrayList<String[]> statsData = new ArrayList<>();

        statsData = CsvReadStats(directoryPath); // Read CSV data here

        TextView title = findViewById(R.id.titleStats);
        title.setText("Training statistics for " + name);

        TextView totalMeanDur = findViewById(R.id.total_mean_dur);
        TextView totalStdDur = findViewById(R.id.total_std_dur);
        TextView totalMeanSteps = findViewById(R.id.total_mean_steps);
        TextView step1MeanDur = findViewById(R.id.step1);
        TextView step2MeanDur = findViewById(R.id.step2);
        TextView step3MeanDur = findViewById(R.id.step3);
        TextView step4MeanDur = findViewById(R.id.step4);

        totalMeanDur.setText(statsData.get(0)[1]);
        totalStdDur.setText(statsData.get(1)[1]);
        totalMeanSteps.setText(statsData.get(2)[1]);
        step1MeanDur.setText(statsData.get(3)[1]);
        step2MeanDur.setText(statsData.get(4)[1]);
        step3MeanDur.setText(statsData.get(5)[1]);
        step4MeanDur.setText(statsData.get(6)[1]);


        Button homeButton = (Button) findViewById(R.id.homeButton);

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();;
            }
        });
    }


    private ArrayList<String[]> CsvReadStats(String path) {
        ArrayList<String[]> csvData = new ArrayList<>();
        try {
            File file = new File(path);
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] nextLine;
            int count = 0;
            while ((nextLine = reader.readNext()) != null && count < 7) {
                if (nextLine.length > 0) {
                    csvData.add(nextLine);
                    count++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return csvData;
    }

}