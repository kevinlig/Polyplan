package com.grumblus.polyplan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.grumblus.polyplan.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;

public class ChartActivity extends Activity {

    private ArrayList<Double> xData;
    private ArrayList<Double> yData;
    private String chartType;

    private LinearLayout chartWrap;
    private GraphicalView chartView;
    private XYSeries dataSeries;
    private XYMultipleSeriesDataset dataSet;
    private XYSeriesRenderer seriesRenderer;
    private XYMultipleSeriesRenderer multipleRenderer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // keep screen on
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);


        setContentView(R.layout.activity_chart);

        chartWrap = (LinearLayout)findViewById(R.id.chart_wrapper);

        // receive chart data from main activity
        Intent intent = getIntent();
        xData = (ArrayList<Double>) intent.getSerializableExtra("xData");
        yData = (ArrayList<Double>) intent.getSerializableExtra("yData");
        chartType = (String) intent.getSerializableExtra("type");

        // create the data set
        dataSeries = new XYSeries("data");
        dataSet = new XYMultipleSeriesDataset();
        dataSet.addSeries(dataSeries);

        seriesRenderer = new XYSeriesRenderer();
        multipleRenderer = new XYMultipleSeriesRenderer();
        multipleRenderer.addSeriesRenderer(seriesRenderer);

        seriesRenderer.setColor(Color.WHITE);
        seriesRenderer.setShowLegendItem(false);

        multipleRenderer.setLabelsTextSize(20);
        multipleRenderer.setYLabelsPadding(15);


        double minX = 0;
        double maxX = 0;
        double minY = 0;
        double maxY = 0;

        for (int i = 0; i < xData.size(); i++) {
            double currentX = xData.get(i);
            double currentY = yData.get(i);

            if (currentX < minX) {
                minX = currentX;
            }
            else if (currentX > maxX) {
                maxX = currentX;
            }

            if (currentY < minY) {
                minY = currentY;
            }
            else if (currentY > maxY) {
                maxY = currentY;
            }

            dataSeries.add(currentX, currentY);
        }

        multipleRenderer.setBarWidth(30);
        seriesRenderer.setLineWidth(5);

        multipleRenderer.setYAxisMax(maxY);
        multipleRenderer.setYAxisMin(minY);
        multipleRenderer.setXAxisMax(maxX + ((maxX - minX) / xData.size()));
        multipleRenderer.setXAxisMin(minX - ((maxX - minX) / xData.size()));




        if (chartType.equals("bar")) {
            drawBarChart();
        }
        else {
            drawLineChart();
        }
    }

    private void drawBarChart() {
        if (chartView == null) {
            chartView = ChartFactory.getBarChartView(this, dataSet, multipleRenderer, BarChart.Type.DEFAULT);

            chartWrap.addView(chartView);
        }
        else {
            chartView.repaint();
        }
    }

    private void drawLineChart() {
        if (chartView == null) {
            chartView = ChartFactory.getLineChartView(this, dataSet, multipleRenderer);

            chartWrap.addView(chartView);
        }
        else {
            chartView.repaint();
        }
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.chart, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}
