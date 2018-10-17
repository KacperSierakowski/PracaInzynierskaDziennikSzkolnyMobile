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

public class AddKlasasActivity extends AppCompatActivity {

    TextView KlasasResponseFromRequestTextView;
    Button KlasasbuttonGET,KlasasbuttonPOST,KlasasbuttonPowrot;
    TextInputEditText KlasasNazwaKlasy,KlasasProfilKlasy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.klasas_layout);

        KlasasbuttonPowrot=(Button)findViewById(R.id.KlasasbuttonPowrot);
        KlasasResponseFromRequestTextView=(TextView)findViewById(R.id.KlasasResponseFromRequestTextView);
        KlasasbuttonGET=(Button)findViewById(R.id.KlasasbuttonGET);
        KlasasbuttonPOST=(Button)findViewById(R.id.KlasasbuttonPOST);
        KlasasNazwaKlasy=
                (TextInputEditText)findViewById(R.id.KlasasEditNazwaKlasy);
        KlasasProfilKlasy=
                (TextInputEditText)findViewById(R.id.KlasasEditProfilKlasy);
    }

    public void PowrotDoMain(View v){
        startActivities(new Intent[]{new Intent(this,main_activity.class)});
    }


    static String url ="http://192.168.1.102:45455/api/KlasasWEB";
    // Instantiate the RequestQueue.
    public void GetKlasas(View v){
        RequestQueue queue = Volley.newRequestQueue(this);  queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        // Request a string response from the provided URL.
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url,null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
                        KlasasResponseFromRequestTextView.setText("GET Response is: "+ response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                KlasasResponseFromRequestTextView.setText("GET -> That didn't work!"+error.getMessage().toString());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(jsonRequest);
    }

    static final String REQ_TAG = "VACTIVITY";

    public void PostKlasa(View v){
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });


        String NazwaKlasyTMP=KlasasNazwaKlasy.getText().toString();
        String ProfilKlasyTMP=KlasasProfilKlasy.getText().toString();

        JSONObject json = new JSONObject();
        try {
            json.put("NazwaKlasy", NazwaKlasyTMP);
            json.put("ProfilKlasy", ProfilKlasyTMP);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        KlasasResponseFromRequestTextView.setText("String response : "+ response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                KlasasResponseFromRequestTextView.setText("Error getting response"+error.getMessage().toString());
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
