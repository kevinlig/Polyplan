package com.grumblus.polyplan.QR;

// based on https://github.com/pif/glass-warehouse-automation/blob/master/src/com/eleks/rnd/warehouse/glass/qr/ScanQRConfigActivity.java

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.media.Sounds;
import com.grumblus.polyplan.MainActivity;
import com.grumblus.polyplan.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ScanActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.PreviewCallback mPreviewCB = createPreviewCallback();
    private Camera.AutoFocusCallback mAutoFocusCB = createAutoFocusCallback();
    private Handler autoFocusHandler;
    private ImageScanner mScanner;

    private TextView cameraLabel;

    private boolean mPreviewing = true;
    private boolean mProcessing = false;

    public String workbookId = "";


    private String serverUrl = "https://[server]";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);



        setContentView(R.layout.activity_scan);


        mScanner = new ImageScanner();
        mScanner.setConfig(0, Config.X_DENSITY, 3);
        mScanner.setConfig(0, Config.Y_DENSITY, 3);

        cameraLabel = (TextView)findViewById(R.id.scan_label);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initCamera();
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    public String getWorkbookId() {
        return workbookId;
    }



    // QR code callbacks
    private PreviewCallback createPreviewCallback() {
        PreviewCallback previewCallback = new Camera.PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mProcessing == true) {
                    return;
                }

                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();

                Image barcode = new Image(size.width, size.height, "NV21");
                barcode.setData(data);
                barcode = barcode.convert("Y800");

                int result = mScanner.scanImage(barcode);
                if (result != 0) {
                    mProcessing = true;

                    // play a sound
                    AudioManager audioManager = (AudioManager) getSystemService(getApplicationContext().AUDIO_SERVICE);
                    audioManager.playSoundEffect(Sounds.SUCCESS);

                    SymbolSet syms = mScanner.getResults();

                    for (Symbol sym : syms) {
                        workbookId = sym.getData();
                        cameraLabel.setText("Processing...");
                        cameraLabel.setTextColor(Color.parseColor("#ffffff"));
                        importWorksheet(workbookId);
                        break;
                    }

                }
            }
        };

        return previewCallback;
    }

    private AutoFocusCallback createAutoFocusCallback() {
        final Runnable doAutoFocus = new Runnable() {
            public void run() {
                if (mPreviewing)
                    mCamera.autoFocus(mAutoFocusCB);
            }
        };
        AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
            public void onAutoFocus(boolean success, Camera camera) {
                autoFocusHandler.postDelayed(doAutoFocus, 2000);
            }
        };
        return autoFocusCallback;
    }



    private void releaseCamera() {
        Log.d("Glass app","release");
        if (mCamera != null) {
            mPreviewing = false;
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void pauseCamera() {
        mPreviewing = false;
        mCamera.stopPreview();
    }

    private static Camera getCamera() {
        Camera c = null;
        try {
            c = Camera.open(0);
        } catch (Exception e) {
            Log.d("Glass app", "" + e);
        }
        Log.d("Glass app", "returning camera: " + c);
        return c;
    }

    private void initCamera() {
        mPreviewing = true;
        mCamera = getCamera();
        if (mCamera == null) {
            Toast.makeText(this, "Camera is not available at the moment. Restart Glass.", Toast.LENGTH_LONG).show();
            finish();
        }
        mPreview = (CameraPreview) findViewById(R.id.camera_preview);
        mPreview.init(mCamera, mPreviewCB, mAutoFocusCB);
    }


    // Worksheet import code
    private void importWorksheet(String databaseId){

        String restUrl = serverUrl + "/worksheets/" + databaseId;

        new HttpAsyncTask().execute(restUrl);
    }

    private void displayScanError() {
        cameraLabel.setText("Invalid barcode, try again.");
        cameraLabel.setTextColor(Color.parseColor("#cc3333"));
        mProcessing = false;
    }


    // based on http://hmkcode.com/android-parsing-json-data/
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            InputStream inputStream = null;
            String restOutput = "";

            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse httpResponse = httpClient.execute(new HttpGet(urls[0]));
                inputStream = httpResponse.getEntity().getContent();

                if (inputStream != null) {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        restOutput += line;
                    }
                    inputStream.close();
                }
                else {
                    displayScanError();
                }

            }
            catch (Exception e) {
                displayScanError();
            }

            return restOutput;
        }

        @Override
        protected void onPostExecute(String result) {
            // we've retrieved the JSON as a string from the server, parse it
            JSONObject json;
            try {
                json = new JSONObject(result);
                if (!json.getString("status").equals("success")) {
                    displayScanError();
                    return;
                }
                JSONArray spreadsheetData = json.getJSONArray("data");
                if (spreadsheetData == null) {
                    displayScanError();
                    return;
                }

                // we don't need the camera any more, kill it
                releaseCamera();

                // parse the JSONArray into a more usable array
                // for the time being, we're going to require everything be doubles
                ArrayList<ArrayList> spreadsheetRows = new ArrayList<ArrayList>();
                for (int i = 0; i < spreadsheetData.length(); i++) {
                    ArrayList<Double> rowContents = new ArrayList<Double>();

                    JSONArray rowData = spreadsheetData.getJSONArray(i);
                    for (int j = 0; j < rowData.length(); j++) {
                        rowContents.add(rowData.getDouble(j));
                    }

                    spreadsheetRows.add(rowContents);
                }

                // go to the next activity
                Intent nextActivity = new Intent(getApplicationContext(), MainActivity.class);
                nextActivity.putExtra("data",spreadsheetRows);
                startActivity(nextActivity);
            }
            catch (JSONException e) {
                displayScanError();
            }

        }
    }
}
