package com.cntt2.flashcard.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.dto.SessionDto;
import com.cntt2.flashcard.data.repository.LearningSessionRepository;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsFragment extends Fragment {

    private static final String RANGE_LAST_5 = "Last 5";
    private static final String RANGE_LAST_10 = "Last 10";
    private static final String RANGE_ALL = "All";
    private static final int RANGE_VALUE_5 = 5;
    private static final int RANGE_VALUE_10 = 10;
    private static final int RANGE_VALUE_ALL = Integer.MAX_VALUE;
    private static final String CHART_TYPE_LINE = "Line";

    private LineChart lineChart;
    private Spinner spinnerRange;
    private Spinner spinnerChartType;
    private LearningSessionRepository sessionRepository;
    private String deskId;
    private int range = RANGE_VALUE_5;

    public StatisticsFragment() {
        // Required empty public constructor
    }

    public static StatisticsFragment newInstance(String deskId) {
        StatisticsFragment fragment = new StatisticsFragment();
        Bundle args = new Bundle();
        args.putString("deskId", deskId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deskId = getArguments().getString("deskId");
        }
        sessionRepository = App.getInstance().getLearningSessionRepository();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        lineChart = view.findViewById(R.id.lineChart);
        spinnerRange = view.findViewById(R.id.spinnerRange);
        spinnerChartType = view.findViewById(R.id.spinnerChartType);
        view.findViewById(R.id.btnClearRecords).setOnClickListener(v -> clearRecords());

        setupSpinners();
        setupChart();
        loadChartData();

        return view;
    }

    private void setupSpinners() {
        // Range Spinner
        List<String> rangeOptions = new ArrayList<>();
        rangeOptions.add(RANGE_LAST_5);
        rangeOptions.add(RANGE_LAST_10);
        rangeOptions.add(RANGE_ALL);
        ArrayAdapter<String> rangeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, rangeOptions);
        rangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRange.setAdapter(rangeAdapter);
        spinnerRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        range = RANGE_VALUE_5;
                        break;
                    case 1:
                        range = RANGE_VALUE_10;
                        break;
                    case 2:
                        range = RANGE_VALUE_ALL;
                        break;
                }
                loadChartData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });

        // Chart Type Spinner
        List<String> chartTypes = new ArrayList<>();
        chartTypes.add(CHART_TYPE_LINE);
        ArrayAdapter<String> chartTypeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, chartTypes);
        chartTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartType.setAdapter(chartTypeAdapter);
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(Color.TRANSPARENT);

        // Configure X-axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // Configure Y-axis
        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setTextColor(Color.WHITE);
        yAxisLeft.setGridColor(Color.LTGRAY);
        yAxisLeft.setAxisLineColor(Color.WHITE);
        yAxisLeft.setTextSize(12f);
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setSpaceBottom(0f);
        yAxisLeft.setGranularity(1f);
        yAxisLeft.setGranularityEnabled(true);
        yAxisLeft.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setTextColor(Color.WHITE);
    }

    private void loadChartData() {
        if (deskId == null) {
            Toast.makeText(requireContext(), "Invalid desk ID", Toast.LENGTH_SHORT).show();
            lineChart.clear();
            return;
        }

        sessionRepository.getSessionsByDeskId(deskId, new Callback<List<SessionDto>>() {
            @Override
            public void onResponse(Call<List<SessionDto>> call, Response<List<SessionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SessionDto> sessions = response.body();
                    updateChart(sessions);
                } else {
                    Toast.makeText(requireContext(), "Failed to load sessions", Toast.LENGTH_SHORT).show();
                    lineChart.clear();
                }
            }

            @Override
            public void onFailure(Call<List<SessionDto>> call, Throwable t) {
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                lineChart.clear();
            }
        });
    }

    private void updateChart(List<SessionDto> sessions) {
        if (sessions.isEmpty()) {
            lineChart.clear();
            return;
        }

        // Limit sessions based on range
        int startIndex = Math.max(0, sessions.size() - range);
        List<SessionDto> filteredSessions = sessions.subList(startIndex, sessions.size());

        // Update X-axis label count
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setLabelCount(filteredSessions.size(), true);

        // Find maximum value for Y-axis
        float maxValue = 0;
        for (SessionDto session : filteredSessions) {
            int totalCards = session.getCardsStudied();
            int correct = (int) (session.getPerformance() * totalCards);
            int incorrect = totalCards - correct;
            maxValue = Math.max(maxValue, Math.max(correct, incorrect));
        }

        // Update Y-axis
        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setAxisMaximum(maxValue + 1);

        // Prepare chart data
        List<Entry> correctEntries = new ArrayList<>();
        List<Entry> incorrectEntries = new ArrayList<>();

        for (int i = 0; i < filteredSessions.size(); i++) {
            SessionDto session = filteredSessions.get(i);
            int totalCards = session.getCardsStudied();
            int correct = (int) (session.getPerformance() * totalCards);
            int incorrect = totalCards - correct;

            correctEntries.add(new Entry(i, correct));
            incorrectEntries.add(new Entry(i, incorrect));
        }

        // Create datasets
        LineDataSet correctDataSet = new LineDataSet(correctEntries, "Total Correct");
        correctDataSet.setColor(Color.GREEN);
        correctDataSet.setCircleColor(Color.GREEN);
        correctDataSet.setLineWidth(2f);
        correctDataSet.setCircleRadius(4f);
        correctDataSet.setDrawCircleHole(false);
        correctDataSet.setValueTextColor(Color.WHITE);
        correctDataSet.setValueTextSize(10f);
        correctDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        LineDataSet incorrectDataSet = new LineDataSet(incorrectEntries, "Total Incorrect");
        incorrectDataSet.setColor(Color.RED);
        incorrectDataSet.setCircleColor(Color.RED);
        incorrectDataSet.setLineWidth(2f);
        incorrectDataSet.setCircleRadius(4f);
        incorrectDataSet.setDrawCircleHole(false);
        incorrectDataSet.setValueTextColor(Color.WHITE);
        incorrectDataSet.setValueTextSize(10f);
        incorrectDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // Combine data into chart
        LineData lineData = new LineData(correctDataSet, incorrectDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    private void clearRecords() {
        if (deskId == null) {
            Toast.makeText(requireContext(), "Invalid desk ID", Toast.LENGTH_SHORT).show();
            return;
        }

        sessionRepository.deleteAllSessionsByDeskId(deskId, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Records cleared", Toast.LENGTH_SHORT).show();
                    loadChartData();
                } else {
                    Toast.makeText(requireContext(), "Failed to clear records", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}