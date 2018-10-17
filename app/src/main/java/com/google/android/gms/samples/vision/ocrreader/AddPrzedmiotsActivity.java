package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class AddPrzedmiotsActivity extends AppCompatActivity {

    TextView PrzedmiotsResponseFromRequestTextView;
    Button PrzedmiotsbuttonGET,PrzedmiotsbuttonPOST,PrzedmiotsbuttonPowrot;
    TextInputEditText PrzedmiotsNazwaPrzedmiotu,PrzedmiotsNauczycielID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.przedmiots_layout);

        PrzedmiotsbuttonPowrot=(Button)findViewById(R.id.PrzedmiotbuttonPowrot);
        PrzedmiotsResponseFromRequestTextView=(TextView)findViewById(R.id.PrzedmiotResponseFromRequestTextView);
        PrzedmiotsbuttonGET=(Button)findViewById(R.id.PrzedmiotbuttonGET);
        PrzedmiotsbuttonPOST=(Button)findViewById(R.id.PrzedmiotbuttonPOST);
        PrzedmiotsNazwaPrzedmiotu=
                (TextInputEditText)findViewById(R.id.PrzedmiotNazwaPrzedmiotuEditText);
        PrzedmiotsNauczycielID=
                (TextInputEditText)findViewById(R.id.PrzedmiotNauczycielIDEditText);
    }
    public void PowrotDoMain(View v){
        startActivities(new Intent[]{new Intent(this,main_activity.class)});
    }

    static String url ="http://192.168.1.102:45455/api/PrzedmiotsWEB";
    public void GetPrzedmiots(View v){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(AddPrzedmiotsActivity.this);
        queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        // display response
                        PrzedmiotsResponseFromRequestTextView.setText("Get Response"+response.toString());
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        PrzedmiotsResponseFromRequestTextView.setText("Error Get Response "+ error.getMessage());
                    }
                }
        );
        // Add the request to the RequestQueue.
        queue.add(getRequest);
    }

    static final String REQ_TAG = "VACTIVITY";

    public void PostPrzedmiot(View v){
        RequestQueue queue = Volley.newRequestQueue(AddPrzedmiotsActivity.this);
        queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });

        String NazwaPrzedmiotuTMP=PrzedmiotsNazwaPrzedmiotu.getText().toString();
        String IDnauczycielaTMP=PrzedmiotsNauczycielID.getText().toString();

        JSONObject json = new JSONObject();
        try {
            json.put("NazwaPrzedmiotu", NazwaPrzedmiotuTMP);
            json.put("NauczycielID", IDnauczycielaTMP);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        PrzedmiotsResponseFromRequestTextView.setText("String response : "+ response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                PrzedmiotsResponseFromRequestTextView.setText("Error getting response"+error.getMessage().toString());
            }
        });
        //Solved problem with duplicated data/request
        /*jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));*/

        jsonObjectRequest.setTag(REQ_TAG);
        queue.add(jsonObjectRequest);

    }

}
