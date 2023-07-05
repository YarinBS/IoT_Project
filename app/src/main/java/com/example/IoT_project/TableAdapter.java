package com.example.IoT_project;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TableAdapter extends RecyclerView.Adapter<TableAdapter.ViewHolder> {

    private List<TableRow> tableData;

    public TableAdapter(List<TableRow> tableData) {
        this.tableData = tableData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.db_items_layout, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == 0) {
            // Set the text for column names (title)
            holder.titleTextViewView.setVisibility(View.VISIBLE);
            holder.titleTextViewView.setText("Your Salsa Training");
            holder.trainingTimeTextView.setText("Time");
            holder.stepTextView.setText("Step");
            holder.enteredStepsTextView.setText("Entered Steps");
            holder.classificationTextView.setText("Classification");
            holder.trainingDurationTextView.setText("Training Duration");
            holder.separatorView.setVisibility(View.VISIBLE);
        } else {
            // Set the data for regular rows
            holder.titleTextViewView.setVisibility(View.GONE);
            holder.separatorView.setVisibility(View.GONE);
            TableRow row = tableData.get(position - 1);
            holder.trainingTimeTextView.setText(row.getTrainingTime());
            holder.stepTextView.setText(row.getStep());
            holder.enteredStepsTextView.setText(row.getEnteredSteps());
            holder.classificationTextView.setText(row.getCalculatedSteps());
            holder.trainingDurationTextView.setText(row.getTrainingDuration());
        }
    }

    @Override
    public int getItemCount() {
        return tableData.size() + 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextViewView;
        TextView trainingTimeTextView;
        TextView stepTextView;
        TextView enteredStepsTextView;
        TextView classificationTextView;
        TextView trainingDurationTextView;
        LinearLayout separatorView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextViewView = itemView.findViewById(R.id.titleTextView);
            trainingTimeTextView = itemView.findViewById(R.id.training_time);
            stepTextView = itemView.findViewById(R.id.step);
            enteredStepsTextView = itemView.findViewById(R.id.entered_steps);
            classificationTextView = itemView.findViewById(R.id.step_class);
            trainingDurationTextView = itemView.findViewById(R.id.training_duration);
            separatorView = itemView.findViewById(R.id.separator);
        }
    }

    public static class TableRow {
        private final String trainingTime;
        private final String step;
        private final String enteredSteps;
        private final String calculatedSteps;
        private final String trainingDuration;

        public TableRow(String trainingTime, String step, String enteredSteps, String calculatedSteps, String trainingDuration) {
            this.trainingTime = trainingTime;
            this.step = step;
            this.enteredSteps = enteredSteps;
            this.calculatedSteps = calculatedSteps;
            this.trainingDuration = trainingDuration;
        }

        public String getTrainingTime() {
            return trainingTime;
        }

        public String getStep() {
            return step;
        }

        public String getEnteredSteps() {
            return enteredSteps;
        }

        public String getCalculatedSteps() {
            return calculatedSteps;
        }

        public String getTrainingDuration() {
            return trainingDuration;
        }
    }
}
