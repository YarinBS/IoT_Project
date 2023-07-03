package com.example.IoT_project;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadDB extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TableAdapter adapter;
    private List<TableAdapter.TableRow> tableData;
    private int currentPage = 0;
    private int rowsPerPage = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_db);
        String receivedUsername = getIntent().getStringExtra("FULLNAME_KEY");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String directoryPath = "/sdcard/csv_dir/databases/" + receivedUsername + ".csv";

        File tmpDir = new File(directoryPath);
        if (!tmpDir.exists()) {
            Toast.makeText(this, "No data for this user. Go to record your training!", Toast.LENGTH_SHORT).show();
            finish();
        }

        tableData = readCsvData(directoryPath); // Read CSV data here
        showTableData(currentPage);

        // Pagination
        findViewById(R.id.nextButton).setOnClickListener(v -> {
            if ((currentPage + 1) * rowsPerPage < tableData.size()) {
                currentPage++;
                showTableData(currentPage);
            }
        });

        findViewById(R.id.previousButton).setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                showTableData(currentPage);
            }
            else {
                finish();
            }
        });
    }

    private void showTableData(int page) {
        int startIndex = page * rowsPerPage;
        int endIndex = Math.min((page + 1) * rowsPerPage, tableData.size());
        List<TableAdapter.TableRow> currentPageData = tableData.subList(startIndex, endIndex);

        adapter = new TableAdapter(currentPageData);
        recyclerView.setAdapter(adapter);
    }

    private List<TableAdapter.TableRow> readCsvData(String path) {
        List<TableAdapter.TableRow> tableData = new ArrayList<>();
        try {
            File file = new File(path);
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] line;
            for (int i = 0; i < 9; i++) {
                reader.readNext();
            }
            while ((line = reader.readNext()) != null) {
                TableAdapter.TableRow row = new TableAdapter.TableRow(line[0], line[1], line[2], line[3], line[4]);
                // Set data for other columns as needed
                tableData.add(row);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
        return tableData;
    }
}