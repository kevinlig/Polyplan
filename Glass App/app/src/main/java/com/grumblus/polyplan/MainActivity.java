package com.grumblus.polyplan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.google.android.glass.media.Sounds;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.grumblus.polyplan.R;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;


public class MainActivity extends Activity  {

    private static final int SPEECH_REQUEST = 0;
    private ArrayList<ArrayList> spreadsheetData;
    private String[] columnNames = {"A","B","C"};
    private TableLayout tableView;
    private GestureDetector menuGestureDetector;
    private AudioManager menuAudioManager;

    private TextView commandText;
    private ImageView commandImage;

    private int menuType = 0;
    private int selectionMode = 0;
    private int selectionRow = 0;
    private int selectionColumn = 0;

    private int operationMode = 0;

    private int startRow = 0;
    private int startColumn = 0;


    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startActivityForResult(intent, SPEECH_REQUEST);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // keep screen on
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);


        menuGestureDetector = createGestureDetector(this);
        menuAudioManager = (AudioManager) getSystemService(this.AUDIO_SERVICE);

        setContentView(R.layout.activity_main);

        tableView = (TableLayout) findViewById(R.id.tableView);
        commandText = (TextView) findViewById(R.id.control_bar_text);
        commandImage = (ImageView) findViewById(R.id.control_bar_icon);

        // receive spreadsheet data from scanner activity
        Intent intent = getIntent();
        spreadsheetData = (ArrayList<ArrayList>) intent.getSerializableExtra("data");

        // populate the cells
        for (int i = 0; i < spreadsheetData.size(); i++) {

            ArrayList<Double> rowContents = spreadsheetData.get(i);

            for (int j = 0; j < rowContents.size(); j++) {
                String cellReference = columnNames[j] + (i+1);

                // populate UI
                TextView targetCell = (TextView)tableView.findViewWithTag("cell" + cellReference);
                targetCell.setText(rowContents.get(j).toString());
            }

        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (menuType == 1) {
            menuType = 0;
            getMenuInflater().inflate(R.menu.function_types, menu);
        }
        else if (menuType == 2) {
            menuType = 0;
            getMenuInflater().inflate(R.menu.chart_types, menu);
        }
        else{
            getMenuInflater().inflate(R.menu.spreadsheet_operations, menu);
        }
        return true;
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP && selectionMode == 0) {
                    // tapped, load menu
                    menuAudioManager.playSoundEffect(Sounds.TAP);
                    openOptionsMenu();
                    return true;
                }

                else if (selectionMode == 1 || selectionMode == 2) {
                    if (gesture == Gesture.SWIPE_DOWN) {
                        if (selectionRow < 4) {
                            String newReference = columnNames[selectionColumn - 1] + (selectionRow + 1);
                            highlightCell(newReference);
                            selectionRow += 1;
                            return true;
                        }
                    }
                    else if (gesture == Gesture.SWIPE_UP) {
                        if (selectionRow > 1) {
                            String newReference = columnNames[selectionColumn - 1] + (selectionRow - 1);
                            highlightCell(newReference);
                            selectionRow -= 1;
                            return true;
                        }
                    }
                    else if (gesture == Gesture.SWIPE_LEFT) {
                        if (selectionColumn < 3) {
                            String newReference = columnNames[selectionColumn] + selectionRow;
                            highlightCell(newReference);
                            selectionColumn += 1;
                            return true;
                        }
                    }
                    else if (gesture == Gesture.SWIPE_RIGHT) {
                        if (selectionColumn > 1) {
                            String newReference = columnNames[selectionColumn - 2] + selectionRow;
                            highlightCell(newReference);
                            selectionColumn -= 1;
                            return true;
                        }
                    }
                    else if (gesture == Gesture.TAP) {
                        // cell selected
                        if (operationMode > 0) {
                            if (selectionMode == 1) {
                                // selected the starting cell
                                selectionMode = 2;

                                startColumn = selectionColumn;
                                startRow = selectionRow;

                                menuAudioManager.playSoundEffect(Sounds.TAP);
                                askForRangeEnd();
                                return true;
                            }
                            else if (selectionMode == 2) {
                                // selected the ending cell

                                // unhighlight cell

                                String currentCellReference = "cell" + columnNames[selectionColumn - 1] + selectionRow;
                                TextView currentCell = (TextView)tableView.findViewWithTag(currentCellReference);
                                currentCell.setBackgroundResource(R.drawable.cell_border_right);

                                menuAudioManager.playSoundEffect(Sounds.TAP);

                                // perform operation
                                performFunction();

                                return true;
                            }
                        }
                    }
                }

                else if (selectionMode == 3 || selectionMode == 4) {
                    if (gesture == Gesture.SWIPE_LEFT) {
                        if (selectionColumn < 3) {
                            String newReference = columnNames[selectionColumn] + selectionRow;
                            highlightCell(newReference);
                            selectionColumn += 1;
                            return true;
                        }
                    }
                    else if (gesture == Gesture.SWIPE_RIGHT) {
                        if (selectionColumn > 1) {
                            String newReference = columnNames[selectionColumn - 2] + selectionRow;
                            highlightCell(newReference);
                            selectionColumn -= 1;
                            return true;
                        }
                    }
                    else if (gesture == Gesture.TAP) {
                        // cell selected
                        menuAudioManager.playSoundEffect(Sounds.TAP);

                        if (selectionMode == 3) {
                            // selected X axis
                            startColumn = selectionColumn;

                            askForYAxis();

                            return true;
                        } else {

                            // unhighlight column
                            String currentCellReference = "cell" + columnNames[selectionColumn - 1] + selectionRow;
                            TextView currentCell = (TextView)tableView.findViewWithTag(currentCellReference);
                            currentCell.setBackgroundColor(Color.WHITE);

                            requestChart();

                            return true;
                        }
                    }

                }
                return false;
            }
        });

        return gestureDetector;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (menuGestureDetector != null) {
            return menuGestureDetector.onMotionEvent(event);
        }
        return false;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.main_operation:
                Log.d("Glass app","Do operation");
                menuType = 1;

                invalidateOptionsMenu();

                // options menu must close after selection
                // but we'll fake it and trigger a re-open after a 100ms
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openOptionsMenu();
                    }
                }, 100);

                return true;
            case R.id.main_chart:
                Log.d("Glass app","Make chart");
                menuType = 2;
                invalidateOptionsMenu();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openOptionsMenu();
                    }
                }, 100);

                return true;
            case R.id.fct_avg:
                selectionMode = 1;
                operationMode = 1;
                return true;
            case R.id.fct_max:
                selectionMode = 1;
                operationMode = 2;
                return true;
            case R.id.fct_min:
                selectionMode = 1;
                operationMode = 3;
                return true;
            case R.id.fct_sum:
                selectionMode = 1;
                operationMode = 4;
                return true;
            case R.id.cht_bar:
                selectionMode = 3;
                operationMode = 5;
                return true;
            case R.id.cht_line:
                selectionMode = 3;
                operationMode = 6;
                return true;
            default:
                menuType = 0;
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        if (operationMode > 0 && operationMode < 5) {
            // start selection
            selectionRow = 1;
            selectionColumn = 1;
            TextView cellA1 = (TextView)findViewById(R.id.cellA1);
            cellA1.setBackgroundResource(R.drawable.cell_selection);
            commandText.setText("Select first cell in range");
            commandImage.setImageResource(R.drawable.move);
        }
        else if (operationMode >= 5) {
            // start selection for charts
            selectionColumn = 1;
            selectionRow = 0;
            TextView cellA0 = (TextView)findViewById(R.id.cellA0);
            cellA0.setBackgroundResource(R.drawable.cell_selection);

            commandText.setText("Select X axis column");
            commandImage.setImageResource(R.drawable.move_x);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
            );
            String spokenText = results.get(0);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void askForRangeEnd() {
        // unhighlight last cell
        String currentCellReference = "cell"+columnNames[selectionColumn - 1] + selectionRow;
        TextView currentCell = (TextView)tableView.findViewWithTag(currentCellReference);
        currentCell.setBackgroundResource(R.drawable.cell_border_right);

        // start selection
        selectionRow = 1;
        selectionColumn = 1;
        TextView cellA1 = (TextView)findViewById(R.id.cellA1);
        cellA1.setBackgroundResource(R.drawable.cell_selection);
        commandText.setText("Select last cell in range");
    }


    private void askForYAxis() {
        // unhighlight last column
        String currentCellReference = "cell"+columnNames[selectionColumn - 1] + selectionRow;
        TextView currentCell = (TextView)tableView.findViewWithTag(currentCellReference);
        currentCell.setBackgroundColor(Color.WHITE);

        // reselect first column
        TextView cellA0 = (TextView)findViewById(R.id.cellA0);
        cellA0.setBackgroundResource(R.drawable.cell_selection);

        selectionColumn = 1;
        selectionMode = 4;
        commandText.setText("Select Y axis column");
    }

    private void highlightCell(String cellReference) {
        // deselect the current cell
        String currentCellReference = "cell"+columnNames[selectionColumn - 1] + selectionRow;
        TextView currentCell = (TextView)tableView.findViewWithTag(currentCellReference);

        if (selectionRow > 0) {
            currentCell.setBackgroundResource(R.drawable.cell_border_right);
        }
        else {
            currentCell.setBackgroundColor(Color.WHITE);
        }

        // select the new cell
        TextView newCell = (TextView)tableView.findViewWithTag("cell"+cellReference);
        newCell.setBackgroundResource(R.drawable.cell_selection);
    }

    private void performFunction() {


        String outputValue = "";

        int error = 0;

        int maxRow = selectionRow;
        int minRow = startRow;
        int maxCol = selectionColumn;
        int minCol = startColumn;

        if (maxRow < minRow) {
            maxRow = startRow;
            minRow = selectionRow;
        }
        if (maxCol < minCol) {
            maxCol = startColumn;
            minCol = selectionColumn;
        }

        // get cell names for start and end cells
        String startCell = columnNames[minCol - 1] + minRow;
        String endCell = columnNames[maxCol - 1] + maxRow;

        if (operationMode == 1) {
            // average
            double sumValue = 0;
            double countValue = 0;

            // loop through cells in range
            for (int i = minRow; i <= maxRow; i++) {
                int arrayRow = i - 1;

                ArrayList<Double> spreadsheetRow = spreadsheetData.get(arrayRow);

                for (int j = minCol; j <= maxCol; j++) {
                    int arrayIndex = j - 1;

                    // get value at cell
                    double cellValue = spreadsheetRow.get(arrayIndex);

                    sumValue += cellValue;
                    countValue++;
                }
            }

            double calculatedValue = sumValue / countValue;

            DecimalFormat df = new DecimalFormat("#.####");
            String displayValue = df.format(calculatedValue);

            // check if integer
            if ((calculatedValue % 1) == 0) {
                DecimalFormat intFormat = new DecimalFormat("#");
                displayValue = intFormat.format(calculatedValue);
            }

            outputValue = "AVERAGE(" + startCell + ":" + endCell + ") = " + displayValue;

        }
        else if (operationMode == 2) {
            // max

            // get the first value
            ArrayList<Double> initialRow = spreadsheetData.get(minRow - 1);

            double maxValue = initialRow.get(minCol - 1);

            // loop through cells in range
            for (int i = minRow; i <= maxRow; i++) {
                int arrayRow = i - 1;

                ArrayList<Double> spreadsheetRow = spreadsheetData.get(arrayRow);

                for (int j = minCol; j <= maxCol; j++) {
                    int arrayIndex = j - 1;

                    // get value at cell
                    double cellValue = spreadsheetRow.get(arrayIndex);

                    if (cellValue > maxValue) {
                        maxValue = cellValue;
                    }

                }
            }

            DecimalFormat df = new DecimalFormat("#.####");
            String displayValue = df.format(maxValue);

            // check if integer
            if ((maxValue % 1) == 0) {
                DecimalFormat intFormat = new DecimalFormat("#");
                displayValue = intFormat.format(maxValue);
            }

            outputValue = "MAX(" + startCell + ":" + endCell + ") = " + displayValue;
        }
        else if (operationMode == 3) {
            // min

            // get the first value
            ArrayList<Double> initialRow = spreadsheetData.get(minRow - 1);

            double minValue = initialRow.get(minCol - 1);

            // loop through cells in range
            for (int i = minRow; i <= maxRow; i++) {
                int arrayRow = i - 1;

                ArrayList<Double> spreadsheetRow = spreadsheetData.get(arrayRow);

                for (int j = minCol; j <= maxCol; j++) {
                    int arrayIndex = j - 1;

                    // get value at cell
                    double cellValue = spreadsheetRow.get(arrayIndex);

                    if (cellValue < minValue) {
                        minValue = cellValue;
                    }

                }
            }


            DecimalFormat df = new DecimalFormat("#.####");
            String displayValue = df.format(minValue);

            // check if integer
            if ((minValue % 1) == 0) {
                DecimalFormat intFormat = new DecimalFormat("#");
                displayValue = intFormat.format(minValue);
            }

            outputValue = "MIN(" + startCell + ":" + endCell + ") = " + displayValue;
        }
        else if (operationMode == 4) {
            // sum
            double sumValue = 0;

            // loop through cells in range
            for (int i = minRow; i <= maxRow; i++) {
                int arrayRow = i - 1;

                ArrayList<Double> spreadsheetRow = spreadsheetData.get(arrayRow);

                for (int j = minCol; j <= maxCol; j++) {
                    int arrayIndex = j - 1;

                    // get value at cell
                    double cellValue = spreadsheetRow.get(arrayIndex);

                    sumValue += cellValue;
                }
            }


            DecimalFormat df = new DecimalFormat("#.####");
            String displayValue = df.format(sumValue);

            // check if integer
            if ((sumValue % 1) == 0) {
                DecimalFormat intFormat = new DecimalFormat("#");
                displayValue = intFormat.format(sumValue);
            }

            outputValue = "SUM(" + startCell + ":" + endCell + ") = " + displayValue;

        }
        else {
            error = 1;
            outputValue = "Invalid operation";
        }

        if (error == 0) {
            commandImage.setImageResource(R.drawable.calculator);
        }
        else {
            commandImage.setImageResource(R.drawable.warning);
        }
        commandText.setText(outputValue);

        operationMode = 0;
        selectionMode = 0;

    }

    private void requestChart() {

        String chartType = "bar";

        if (operationMode == 6) {
            // line chart
            chartType = "line";
        }

        ArrayList<Double> xData = new ArrayList<Double>();
        ArrayList<Double> yData = new ArrayList<Double>();



        // loop through data
        for (int i = 0; i < spreadsheetData.size(); i++) {
            ArrayList<Double> rowData = spreadsheetData.get(i);

            double xValue = rowData.get(startColumn - 1);
            double yValue = rowData.get(selectionColumn - 1);

            xData.add(xValue);
            yData.add(yValue);
        }


        operationMode = 0;
        selectionMode = 0;

        commandImage.setImageResource(R.drawable.command_chart);
        commandText.setText("Generating chart...");

        // go to chart activity
        Intent nextActivity = new Intent(getApplicationContext(), ChartActivity.class);
        nextActivity.putExtra("xData",xData);
        nextActivity.putExtra("yData", yData);
        nextActivity.putExtra("type",chartType);
        startActivity(nextActivity);

        commandImage.setImageResource(R.drawable.star);
        commandText.setText("Tap to perform operations");


    }



}
