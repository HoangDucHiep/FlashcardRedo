package com.cntt2.flashcard.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.repository.LearningSessionRepository;
import com.cntt2.flashcard.model.LearningSession;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class StatisticsFragment extends Fragment {

    private LineChart lineChart;
    private Spinner spinnerRange;
    private Spinner spinnerChartType;
    private LearningSessionRepository sessionRepository;
    private int deskId = -1;
    private int range = 5; // Mặc định hiển thị 5 phiên gần nhất

    public StatisticsFragment() {
        // Required empty public constructor
    }

    public static StatisticsFragment newInstance(int deskId) {
        StatisticsFragment fragment = new StatisticsFragment();
        Bundle args = new Bundle();
        args.putInt("deskId", deskId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deskId = getArguments().getInt("deskId", -1);
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
        rangeOptions.add("Last 5");
        rangeOptions.add("Last 10");
        rangeOptions.add("All");
        ArrayAdapter<String> rangeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, rangeOptions);
        rangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRange.setAdapter(rangeAdapter);
        spinnerRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: range = 5; break;
                    case 1: range = 10; break;
                    case 2: range = Integer.MAX_VALUE; break;
                }
                loadChartData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Chart Type Spinner (chỉ hỗ trợ Line chart trong ví dụ này)
        List<String> chartTypes = new ArrayList<>();
        chartTypes.add("Line");
        ArrayAdapter<String> chartTypeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, chartTypes);
        chartTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartType.setAdapter(chartTypeAdapter);
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false); // Ẩn mô tả mặc định
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(Color.TRANSPARENT);

        // Cấu hình trục X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setGranularity(1f); // Đặt khoảng cách giữa các giá trị là 1
        xAxis.setGranularityEnabled(true); // Bật chế độ granularity

        // Định dạng giá trị trục X để hiển thị số nguyên (bắt đầu từ 0)
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // Cấu hình trục Y
        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setTextColor(Color.WHITE);
        yAxisLeft.setGridColor(Color.LTGRAY);
        yAxisLeft.setAxisLineColor(Color.WHITE); // Đặt màu đường trục Y
        yAxisLeft.setTextSize(12f); // Đặt kích thước chữ của nhãn trục Y

        // Đảm bảo trục Y bắt đầu từ 0 và chỉ hiển thị số nguyên
        yAxisLeft.setAxisMinimum(0f); // Đặt giá trị tối thiểu của trục Y là 0
        yAxisLeft.setSpaceBottom(0f); // Loại bỏ khoảng trống bên dưới trục Y
        yAxisLeft.setGranularity(1f); // Đặt khoảng cách giữa các giá trị là 1
        yAxisLeft.setGranularityEnabled(true); // Bật chế độ granularity

        // Định dạng giá trị trục Y để hiển thị số nguyên
        yAxisLeft.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        lineChart.getAxisRight().setEnabled(false); // Ẩn trục Y bên phải
        lineChart.getLegend().setTextColor(Color.WHITE);
    }

    private void loadChartData() {
        List<LearningSession> sessions = sessionRepository.getSessionsByDeskId(deskId);
        if (sessions.isEmpty()) {
            lineChart.clear();
            return;
        }

        // Giới hạn số phiên theo range
        int startIndex = Math.max(0, sessions.size() - range);
        List<LearningSession> filteredSessions = sessions.subList(startIndex, sessions.size());

        // Cập nhật số lượng nhãn trên trục X dựa trên số phiên học
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setLabelCount(filteredSessions.size(), true); // Đặt số lượng nhãn bằng số phiên học

        // Tìm giá trị tối đa của dữ liệu để điều chỉnh trục Y
        float maxValue = 0;
        for (LearningSession session : filteredSessions) {
            int totalCards = session.getCardsStudied();
            int correct = (int) (session.getPerformance() * totalCards);
            int incorrect = totalCards - correct;
            maxValue = Math.max(maxValue, Math.max(correct, incorrect));
        }

        // Cập nhật trục Y dựa trên giá trị tối đa
        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setAxisMaximum(maxValue + 1); // Đặt giá trị tối đa của trục Y (thêm 1 để có khoảng trống)

        // Dữ liệu cho biểu đồ
        List<Entry> correctEntries = new ArrayList<>();
        List<Entry> incorrectEntries = new ArrayList<>();

        for (int i = 0; i < filteredSessions.size(); i++) {
            LearningSession session = filteredSessions.get(i);
            int totalCards = session.getCardsStudied();
            int correct = (int) (session.getPerformance() * totalCards);
            int incorrect = totalCards - correct;

            correctEntries.add(new Entry(i, correct));
            incorrectEntries.add(new Entry(i, incorrect));
        }

        // Tạo dataset
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

        // Gộp dữ liệu vào biểu đồ
        LineData lineData = new LineData(correctDataSet, incorrectDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate(); // Cập nhật biểu đồ
    }

    private void clearRecords() {
        //sessionRepository.deleteAllSessionsByDeskId(deskId);
        loadChartData();
    }
}