/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.ocrreader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.samples.vision.ocrreader.ui.camera.CameraSource;
import com.google.android.gms.samples.vision.ocrreader.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.ocrreader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

/**
 * Activity for the Ocr Detecting app.  This app detects text and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and contents of each TextBlock.
 */
public final class OcrCaptureActivity extends AppCompatActivity {
    private static final String TAG = "OcrCaptureActivity";

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // Constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String TextBlockObject = "String";

    private CameraSource cameraSource;
    private CameraSourcePreview preview;
    private GraphicOverlay<OcrGraphic> graphicOverlay;

    // Helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    // A TextToSpeech engine for speaking a String value.
    private TextToSpeech tts;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.ocr_capture);

        preview = (CameraSourcePreview) findViewById(R.id.preview);
        graphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);

        // Set good defaults for capturing text.
        boolean autoFocus = true;
        boolean useFlash = false;

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        Snackbar.make(graphicOverlay, "Tap to Speak. Pinch/Stretch to zoom",
                Snackbar.LENGTH_LONG)
                .show();

        //  Set up the Text To Speech engine.
        TextToSpeech.OnInitListener listener =
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(final int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            Log.d("TTS", "Text to speech engine started successfully.");
                            tts.setLanguage(Locale.US);
                        } else {
                            Log.d("TTS", "Error starting the text to speech engine.");
                        }
                    }
                };
        tts = new TextToSpeech(this.getApplicationContext(), listener);
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(graphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        //  Create the TextRecognizer
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        //  Set the TextRecognizer's Processor.
        textRecognizer.setProcessor(new OcrDetectorProcessor(graphicOverlay));
        //  Check if the TextRecognizer is operational.
        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }
        // Create the cameraSource using the TextRecognizer.
        cameraSource =
                new CameraSource.Builder(getApplicationContext(), textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(15.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO : null)
                        .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (preview != null) {
            preview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (preview != null) {
            preview.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // We have permission, so create the camerasource
            boolean autoFocus = getIntent().getBooleanExtra(AutoFocus,false);
            boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
            createCameraSource(autoFocus, useFlash);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (cameraSource != null) {
            try {
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }
    public static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }
    /**
     * onTap is called to speak the tapped TextBlock, if any, out loud.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the tap was on a TextBlock
     */
    private boolean onTap(float rawX, float rawY) {
        //  Speak the text when the user taps on screen.
        OcrGraphic graphic = graphicOverlay.getGraphicAtLocation(rawX, rawY);

        TextBlock text = null;
        if (graphic != null) {
            text = graphic.getTextBlock();
            String CoPrzeczytałem=text.getValue().toString();
            AddOcenas(CoPrzeczytałem);
            /*
            if(CoPrzeczytałem.substring(0, Math.min(CoPrzeczytałem.length(), 3))=="WAR"){
                //Mamy WAR -> mozemy sprawdzić jaka wartosc oceny
                if(isNumeric(CoPrzeczytałem.substring(4, Math.min(CoPrzeczytałem.length(), 4)))){
                    //Mamy 1 cyfre wartosci oceny
                    WartoscOcenyDoPrzekazaniaTMP = CoPrzeczytałem.substring(4, Math.min(CoPrzeczytałem.length(), 4));
                }
                //Czy jest 2 cyfra po WAR?
                if(isNumeric(CoPrzeczytałem.substring(5, Math.min(CoPrzeczytałem.length(), 5)))){
                    //Wartość oceny nie może być 2-cyfrowa
                    //ERROR
                }
                //Czy jest spacja po cyfrze?
                if((CoPrzeczytałem.substring(5, Math.min(CoPrzeczytałem.length(), 5))).matches(".*\\w.*")){
                    //Mamy spacje -> mozemy przejść do Przedmiotu
                    if(CoPrzeczytałem.substring(6, Math.min(CoPrzeczytałem.length(), 8))=="PRZ"){
                        //Mamy PRZ -> mozemy sprawdzic ID przedmiotu
                        //Pierwsza mamy 1 cyfre IDprzedmiotu
                        if(isNumeric(CoPrzeczytałem.substring(9, Math.min(CoPrzeczytałem.length(), 9)))){
                            //Mamy 1 cyfre IDprzedmiotu
                        }
                        //Czy mamy 2 cyfre IDprzedmiotu?
                        if(isNumeric(CoPrzeczytałem.substring(10, Math.min(CoPrzeczytałem.length(), 10)))){
                            //Mamy 2 cyfre -> Dodaj Do 1 cyfry IDprzedmiotu (TERAZ mamy 2-cyfrowe IDprzedmiotu)
                            IDprzedmiotuDoPrzekazaniaTMP=IDprzedmiotuDoPrzekazaniaTMP+CoPrzeczytałem.substring(10, Math.min(CoPrzeczytałem.length(), 10));
                            //Czy mamy 3 cyfre IDprzedmiotu?
                            if(isNumeric(CoPrzeczytałem.substring(11, Math.min(CoPrzeczytałem.length(), 11)))) {
                                //Mamy 3 cyfre -> Dodaj do 1 i 2 cyfry IDprzedmiotu (TERAZ mamy 3-cyfrowe IDprzedmiotu)
                                IDprzedmiotuDoPrzekazaniaTMP=IDprzedmiotuDoPrzekazaniaTMP+CoPrzeczytałem.substring(11, Math.min(CoPrzeczytałem.length(), 11));
                                //Czy mamy spacje po 3 cyfrze
                                if((CoPrzeczytałem.substring(12, Math.min(CoPrzeczytałem.length(), 12))).matches(".*\\w.*"))
                                {
                                    if(CoPrzeczytałem.substring(13, Math.min(CoPrzeczytałem.length(), 15))=="UCZ"){
                                        if(isNumeric(CoPrzeczytałem.substring(16, Math.min(CoPrzeczytałem.length(), 16)))) {
                                            //Mamy pierwsza cyfre ID ucznia
                                            IDuczniaDoPrzekazaniaTMP=(CoPrzeczytałem.substring(16, Math.min(CoPrzeczytałem.length(), 16)));
                                            //Czy mamy 2 cyfre ID ucznia?
                                            if(isNumeric(CoPrzeczytałem.substring(17, Math.min(CoPrzeczytałem.length(), 17)))){
                                                //Mamy 2 cyfre -> Dodaj do 1 cyfry IDucznia (Teraz mamy 2-cyfrowe IDucznia)
                                                IDuczniaDoPrzekazaniaTMP=IDuczniaDoPrzekazaniaTMP+(CoPrzeczytałem.substring(17, Math.min(CoPrzeczytałem.length(), 17)));
                                                //Czy mamy spacje po 2 cyfrze IDucznia
                                                if((CoPrzeczytałem.substring(18, Math.min(CoPrzeczytałem.length(), 18))).matches(".*\\w.*")){
                                                    //Mamy spacje po 2 cyfrze ->  Wyslij Request
                                                    POST(WartoscOcenyDoPrzekazaniaTMP,IDprzedmiotuDoPrzekazaniaTMP,IDuczniaDoPrzekazaniaTMP);
                                                }
                                                //Czy mamy 3 cyfre?
                                                if(isNumeric(CoPrzeczytałem.substring(17, Math.min(CoPrzeczytałem.length(), 17)))){
                                                    IDuczniaDoPrzekazaniaTMP=IDuczniaDoPrzekazaniaTMP+CoPrzeczytałem.substring(17,Math.min((CoPrzeczytałem.length()),17));
                                                    if((CoPrzeczytałem.substring(18, Math.min(CoPrzeczytałem.length(), 18))).matches(".*\\w.*")){
                                                        //Mamy spacje po 3 cyfrze IDucznia ->  Wyslij Request
                                                        POST(WartoscOcenyDoPrzekazaniaTMP,IDprzedmiotuDoPrzekazaniaTMP,IDuczniaDoPrzekazaniaTMP);
                                                    }
                                                }
                                            }
                                            //Czy mamy spacje po 1 cyfrze?
                                            else if((CoPrzeczytałem.substring(17, Math.min(CoPrzeczytałem.length(), 17))).matches(".*\\w.*")){
                                                //Mamy spacje  ->  Wyslij Request
                                                POST(WartoscOcenyDoPrzekazaniaTMP,IDprzedmiotuDoPrzekazaniaTMP,IDuczniaDoPrzekazaniaTMP);
                                            }
                                        }else if((CoPrzeczytałem.substring(16, Math.min(CoPrzeczytałem.length(), 16))).matches(".*\\w.*")){
                                            //Znak po UCZ jest spacja -> BRAK OCENY
                                        }else{
                                            //Znak po UCZ nie jest cyfra ani spacja
                                        }
                                    }
                                }else{
                                    //Znak po 3 cyfrze nie jest spacja! -> Przwidujemy tylko 100 Przedmiotów
                                }
                            }
                            //Czy po 2 cyfrze mamy spacje?
                            else if((CoPrzeczytałem.substring(11, Math.min(CoPrzeczytałem.length(), 11))).matches(".*\\w.*"))
                            {
                                if(CoPrzeczytałem.substring(12, Math.min(CoPrzeczytałem.length(), 14))=="UCZ"){
                                    if(isNumeric(CoPrzeczytałem.substring(15, Math.min(CoPrzeczytałem.length(), 15)))) {
                                        //Mamy pierwsza cyfre ID ucznia
                                        IDuczniaDoPrzekazaniaTMP=(CoPrzeczytałem.substring(15, Math.min(CoPrzeczytałem.length(), 15)));
                                        //Czy mamy 2 cyfre ID ucznia?
                                        if(isNumeric(CoPrzeczytałem.substring(16, Math.min(CoPrzeczytałem.length(), 16)))){
                                            //Mamy 2 cyfre -> Dodaj do 1 cyfry IDucznia (Teraz mamy 2-cyfrowe IDucznia)
                                            IDuczniaDoPrzekazaniaTMP=IDuczniaDoPrzekazaniaTMP+(CoPrzeczytałem.substring(16, Math.min(CoPrzeczytałem.length(), 16)));
                                            //Czy mamy spacje po 2 cyfrze IDucznia
                                            if((CoPrzeczytałem.substring(17, Math.min(CoPrzeczytałem.length(), 17))).matches(".*\\w.*")){
                                                //Mamy spacje po 2 cyfrze ->  Wyslij Request
                                                POST(WartoscOcenyDoPrzekazaniaTMP,IDprzedmiotuDoPrzekazaniaTMP,IDuczniaDoPrzekazaniaTMP);
                                            }
                                            //Czy mamy 3 cyfre?
                                            else if(isNumeric(CoPrzeczytałem.substring(17, Math.min(CoPrzeczytałem.length(), 17)))){
                                                IDuczniaDoPrzekazaniaTMP=IDuczniaDoPrzekazaniaTMP+CoPrzeczytałem.substring(17,Math.min((CoPrzeczytałem.length()),17));
                                                if((CoPrzeczytałem.substring(18, Math.min(CoPrzeczytałem.length(), 18))).matches(".*\\w.*")){
                                                    //Mamy spacje po 3 cyfrze IDucznia ->  Wyslij Request
                                                    POST(WartoscOcenyDoPrzekazaniaTMP,IDprzedmiotuDoPrzekazaniaTMP,IDuczniaDoPrzekazaniaTMP);
                                                }
                                            }else{
                                                //Znak po 2 cyfrze IDucznia nie jest spacja ani cyfra -> ERROR
                                            }
                                        }
                                        //Czy mamy spacje po 1 cyfrze?
                                        else if((CoPrzeczytałem.substring(17, Math.min(CoPrzeczytałem.length(), 17))).matches(".*\\w.*")){
                                            //Mamy spacje  ->  Wyslij Request
                                            POST(WartoscOcenyDoPrzekazaniaTMP,IDprzedmiotuDoPrzekazaniaTMP,IDuczniaDoPrzekazaniaTMP);
                                        }
                                    }else if((CoPrzeczytałem.substring(16, Math.min(CoPrzeczytałem.length(), 16))).matches(".*\\w.*")){
                                        //Znak po UCZ jest spacja -> BRAK OCENY
                                    }else{
                                        //Znak po UCZ nie jest cyfra ani spacja
                                    }
                                }else{
                                    //Nie mozemy rozpoznac UCZ
                                }
                            }
                            else{
                                //3 znak nie jest cyfra ani spacja
                            }
                        }
                        //Czy mamy spacje po pierwszej cyfrze?
                        else if((CoPrzeczytałem.substring(10, Math.min(CoPrzeczytałem.length(), 10))).matches(".*\\w.*")){
                            if(CoPrzeczytałem.substring(11, Math.min(CoPrzeczytałem.length(), 11))=="UCZ"){

                            }
                        }else{
                            //Drugi znak po PRZ nie jest cyfra ani spacja
                        }
                    }else{
                      //Nie wykrywa PRZ
                    }
                }
                else{
                    //Po 1 cyfrze po WAR nie ma cyfry ani spacji
                    //ERROR
                }
            }else{
                //Nie wykrywa WAR
            }
            */
            if (text != null && text.getValue() != null) {
                Log.d(TAG, "text data is being spoken! " + text.getValue());
                // TODO: Speak the string.
                tts.speak(text.getValue(), TextToSpeech.QUEUE_ADD, null, "DEFAULT");
            }
            else {
                Log.d(TAG, "text data is null");
            }
        }
        else {
            Log.d(TAG,"no text detected");
        }
        return text != null;
    }

    static String url ="http://192.168.1.102:45455/api/OcenasWEB";
    static final String REQ_TAG = "VACTIVITY";

    public void AddOcenas(String CoPrzeczytałem ){

        String WartoscOcenyDoPrzekazaniaTMP="";
        String IDuczniaDoPrzekazaniaTMP="";
        String IDprzedmiotuDoPrzekazaniaTMP="";
        String[] PodzielonyText = CoPrzeczytałem.split("\\s+");

        WartoscOcenyDoPrzekazaniaTMP=PodzielonyText[0];
        IDprzedmiotuDoPrzekazaniaTMP=PodzielonyText[1];
        IDuczniaDoPrzekazaniaTMP=PodzielonyText[2];

        if(WartoscOcenyDoPrzekazaniaTMP.substring(0, Math.min(WartoscOcenyDoPrzekazaniaTMP.length(), 3))=="WAR" || WartoscOcenyDoPrzekazaniaTMP.substring(0, Math.min(WartoscOcenyDoPrzekazaniaTMP.length(), 3))=="WAP"||
                WartoscOcenyDoPrzekazaniaTMP.substring(0, Math.min(WartoscOcenyDoPrzekazaniaTMP.length(), 3))=="wAR"|| WartoscOcenyDoPrzekazaniaTMP.substring(0, Math.min(WartoscOcenyDoPrzekazaniaTMP.length(), 3))=="wAP"
                ){
            //Wykrył WAR
        }
        String SamaWartoscDoPrzekazania=WartoscOcenyDoPrzekazaniaTMP.substring(3, Math.min(WartoscOcenyDoPrzekazaniaTMP.length(), (WartoscOcenyDoPrzekazaniaTMP.length())));
        SamaWartoscDoPrzekazania.replaceAll("S", "5");
        SamaWartoscDoPrzekazania.replaceAll("s", "5");
        SamaWartoscDoPrzekazania.replaceAll("B", "8");
        SamaWartoscDoPrzekazania.replaceAll("b", "6");
        SamaWartoscDoPrzekazania.replaceAll("O", "0");
        SamaWartoscDoPrzekazania.replaceAll("o", "0");

        if(IDprzedmiotuDoPrzekazaniaTMP.substring(0, Math.min(IDprzedmiotuDoPrzekazaniaTMP.length(), 3))=="PRZ" || IDprzedmiotuDoPrzekazaniaTMP.substring(0, Math.min(IDprzedmiotuDoPrzekazaniaTMP.length(), 3))=="RRZ" ||
                IDprzedmiotuDoPrzekazaniaTMP.substring(0, Math.min(IDprzedmiotuDoPrzekazaniaTMP.length(), 3))=="RPZ" || IDprzedmiotuDoPrzekazaniaTMP.substring(0, Math.min(IDprzedmiotuDoPrzekazaniaTMP.length(), 3))=="PPZ" ||
                IDprzedmiotuDoPrzekazaniaTMP.substring(0, Math.min(IDprzedmiotuDoPrzekazaniaTMP.length(), 3))=="PRz" || IDprzedmiotuDoPrzekazaniaTMP.substring(0, Math.min(IDprzedmiotuDoPrzekazaniaTMP.length(), 3))=="RPz" ||
                IDprzedmiotuDoPrzekazaniaTMP.substring(0, Math.min(IDprzedmiotuDoPrzekazaniaTMP.length(), 3))=="PPz"
                ){
            //Wykrył PRZ
        }
        String SamaWartoscIDprzedmiotuDoPrzekazania=IDprzedmiotuDoPrzekazaniaTMP.substring(3, Math.min(IDprzedmiotuDoPrzekazaniaTMP.length(), (IDprzedmiotuDoPrzekazaniaTMP.length())));
        SamaWartoscIDprzedmiotuDoPrzekazania.replaceAll("S", "5");
        SamaWartoscIDprzedmiotuDoPrzekazania.replaceAll("s", "5");
        SamaWartoscIDprzedmiotuDoPrzekazania.replaceAll("B", "8");
        SamaWartoscIDprzedmiotuDoPrzekazania.replaceAll("b", "6");
        SamaWartoscIDprzedmiotuDoPrzekazania.replaceAll("O", "0");
        SamaWartoscIDprzedmiotuDoPrzekazania.replaceAll("o", "0");

        if(IDuczniaDoPrzekazaniaTMP.substring(0, Math.min(IDuczniaDoPrzekazaniaTMP.length(), 3))=="UCZ" || IDuczniaDoPrzekazaniaTMP.substring(0, Math.min(IDuczniaDoPrzekazaniaTMP.length(), 3))=="UCz" ||
                IDuczniaDoPrzekazaniaTMP.substring(0, Math.min(IDuczniaDoPrzekazaniaTMP.length(), 3))=="uCZ" || IDuczniaDoPrzekazaniaTMP.substring(0, Math.min(IDuczniaDoPrzekazaniaTMP.length(), 3))=="UcZ"){
            //Wykrył UCZ
        }
        String SamaWartoscIDuczniaDoPrzekazania=IDuczniaDoPrzekazaniaTMP.substring(3, Math.min(IDuczniaDoPrzekazaniaTMP.length(), (IDuczniaDoPrzekazaniaTMP.length())));
        SamaWartoscIDuczniaDoPrzekazania.replaceAll("S", "5");
        SamaWartoscIDuczniaDoPrzekazania.replaceAll("s", "5");
        SamaWartoscIDuczniaDoPrzekazania.replaceAll("B", "8");
        SamaWartoscIDuczniaDoPrzekazania.replaceAll("b", "6");
        SamaWartoscIDuczniaDoPrzekazania.replaceAll("O", "0");
        SamaWartoscIDuczniaDoPrzekazania.replaceAll("o", "0");

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(OcrCaptureActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_addocenas,null);

        final EditText DialogEditTextWartoscOcenyDoPrzekazania = (EditText) mView.findViewById(R.id.DialogMobileVisionEditTextWartoscOceny);
        final EditText DialogEditTextIDuczniaDoPrzekazania = (EditText) mView.findViewById(R.id.DialogMobileVisionEditTextIDucznia);
        final EditText DialogEditTextIDprzedmiotuDoPrzekazania = (EditText) mView.findViewById(R.id.DialogMobileVisionEditTextIDprzedmiotu);
        Button ButtonAddOcenas = (Button) mView.findViewById(R.id.buttonAddOcenasDialog);
        final TextView ResponseMobileVisionTextView = (TextView)mView.findViewById(R.id.DialogMobileVisionResponse);

        DialogEditTextIDprzedmiotuDoPrzekazania.setText(SamaWartoscIDprzedmiotuDoPrzekazania);
        DialogEditTextIDuczniaDoPrzekazania.setText(SamaWartoscIDuczniaDoPrzekazania);

        ButtonAddOcenas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RequestQueue queue = Volley.newRequestQueue(OcrCaptureActivity.this);
                queue.cancelAll(new RequestQueue.RequestFilter() {
                    @Override
                    public boolean apply(Request<?> request) {
                        return true;
                    }
                });
                String WartoscOcenyTMP = DialogEditTextWartoscOcenyDoPrzekazania.getText().toString();
                String IDuczniaTMP=DialogEditTextIDuczniaDoPrzekazania.getText().toString();
                String IDprzedmiotuTMP=DialogEditTextIDprzedmiotuDoPrzekazania.getText().toString();
                JSONObject json = new JSONObject();
                try {
                    json.put("WartoscOceny", WartoscOcenyTMP);
                    json.put("UczenID", IDuczniaTMP);
                    json.put("PrzedmiotID", IDprzedmiotuTMP);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ResponseMobileVisionTextView.setText("WartoscOceny:" + WartoscOcenyTMP + ", UczenID" + IDuczniaTMP + ", PrzedmiotID" + IDprzedmiotuTMP);

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, json,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                ResponseMobileVisionTextView.setText("String response : "+ response.toString());
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String TrescBledu;
                        if(error==null){
                            TrescBledu="Brak treści błędu -> Jesteś podłączony do tej samej sieci?";
                        }else{
                            TrescBledu=error.getMessage();
                        }
                        //Toast.makeText(OcrCaptureActivity.this,"Error getting response"+error.getMessage().toString(),Toast.LENGTH_LONG);
                       ResponseMobileVisionTextView.setText("Error getting response" + TrescBledu);
                    }
                });
                jsonObjectRequest.setTag(REQ_TAG);
                queue.add(jsonObjectRequest);

            }
        });
        mBuilder.setView(mView);
        AlertDialog dialog =mBuilder.create();
        dialog.show();

    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (cameraSource != null) {
                cameraSource.doZoom(detector.getScaleFactor());
            }
        }
    }
}
