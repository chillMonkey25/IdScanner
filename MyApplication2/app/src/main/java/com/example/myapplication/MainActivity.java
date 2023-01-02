package com.example.myapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import android.content.Context;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;



public class MainActivity extends AppCompatActivity {
    // Declare a button object
    Button btn_scan;
    ProgressDialog progressDialog;
    boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // Set the content view to the activity_main layout
        setContentView(R.layout.activity_main);
        // Find the button in the layout and store it in the btn_scan object
        btn_scan = findViewById(R.id.btn_scan);
        // Loads a loading screen to send the data to Google sheets
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please wait a few moments");
        // Set an onClickListener for the button
        btn_scan.setOnClickListener(v ->
        {
            // When the button is clicked, call the scanCode() method
            scanCode();
        });
    }

    // Method to start the scan process
    private void scanCode()
    {

        // Create a ScanOptions object to configure the scan
        ScanOptions options = new ScanOptions();
        // Specifies it can only scan code_39 barcodes
        options.setDesiredBarcodeFormats(String.valueOf(BarcodeFormat.CODE_39));
        // Set the camera ID to 1 (front-facing camera)
        options.setCameraId(1);
        // Directions when the user is trying to scan id
        options.setPrompt("Move barcode slowly forwards and backwards if it has not scanned");
        // Enable the beep sound when a scan is successful
        options.setBeepEnabled(true);
        // Lock the orientation of the scan screen
        options.setOrientationLocked(true);
        // Set the capture activity class to be used for the scan screen
        options.setCaptureActivity(CaptureAct.class);
        // Start the scan using the ScanOptions object
        barLauncher.launch(options);
    }

    // Declare an ActivityResultLauncher object to handle the scan results
    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result ->
    {
        // Check if the scan returned a result
        if (result.getContents() != null && result.getContents().length() == 6 && Pattern.matches("^\\d+$", result.getContents())){
            addId(result.getContents());
            progressDialog.show();
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            loading = true;
            hideDialogue();
        }
        else{
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            Toast toastError = Toast.makeText(getApplicationContext(), "Error please scan again", Toast.LENGTH_SHORT);
            toastError.setGravity(Gravity.CENTER, 0, 0);
            toastError.show();
        }
    });

    private void hideDialogue(){
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                // This line of code will be executed after 5 seconds
                if (loading = true){
                    progressDialog.hide();
                    Toast toastSuccess = Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT);
                    toastSuccess.setGravity(Gravity.CENTER, 0, 0);
                    toastSuccess.show();
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

    public void addId(String idNumber){
        String id = idNumber;
        // Displays a success message after the data is sent to google sheets
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toastSuccess = Toast.makeText(context, "Success", duration);
        Toast toastError = Toast.makeText(context, "Error please scan again", duration);
        toastSuccess.setGravity(Gravity.CENTER, 0, 0);
        toastError.setGravity(Gravity.CENTER, 0, 0);
        progressDialog.hide();
        loading = false;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbxwp9h1a7WH8UOeF2UQY5Nz9SjDoClNBaa88jcj6bnaxWX2wftXf5FHYrDerChVJ2qe1A/exec", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                toastSuccess.show();
            }
        }, error -> {
            toastError.show();
        }){

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "studentId");
                params.put("idNumber", id);
                return params;
            }
        };

        int socketTimeout = 50000;
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeout, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);

    }


}