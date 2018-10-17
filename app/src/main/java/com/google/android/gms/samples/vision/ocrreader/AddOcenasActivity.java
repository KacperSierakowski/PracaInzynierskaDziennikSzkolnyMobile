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

public class AddOcenasActivity extends AppCompatActivity {

    TextView OcenasResponseFromRequestTextView;
    Button OcenasbuttonGET,OcenasbuttonPOST,OcenasbuttonPowrot;
    TextInputEditText OcenasWartoscOceny,OcenasIDucznia,OcenasIDprzedmiotu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ocenas_layout);
        OcenasbuttonPowrot=(Button)findViewById(R.id.OcenasbuttonPowrot);
        OcenasResponseFromRequestTextView=(TextView)findViewById(R.id.OcenasResponseFromRequestTextView);
        OcenasbuttonGET=(Button)findViewById(R.id.OcenasbuttonGET);
        OcenasbuttonPOST=(Button)findViewById(R.id.OcenasbuttonPOST);
        OcenasWartoscOceny=
                (TextInputEditText)findViewById(R.id.OcenasEditWartoscOceny);
        OcenasIDucznia=
                (TextInputEditText)findViewById(R.id.OcenasEditIDucznia);
        OcenasIDprzedmiotu=
                (TextInputEditText)findViewById(R.id.OcenasEditIDprzedmiotu);
    }

    public void PowrotDoMain(View v){
        startActivities(new Intent[]{new Intent(this,main_activity.class)});
    }

    static String url ="http://192.168.1.102:45455/api/OcenasWEB";

    public void Get(View v){

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(AddOcenasActivity.this);
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
                        OcenasResponseFromRequestTextView.setText("Get Response" + response.toString());
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String TrescBledu;
                        if(error==null){
                            TrescBledu="Brak treści błędu";
                        }
                        TrescBledu=error.getMessage();
                        OcenasResponseFromRequestTextView.setText("Error Get Response "+ TrescBledu);
                    }
                }
        );

        // Add the request to the RequestQueue.
        queue.add(getRequest);

    }

    static final String REQ_TAG = "VACTIVITY";

    public void Post(View v){

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(AddOcenasActivity.this);
        queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        String WartoscOcenyTMP=OcenasWartoscOceny.getText().toString();
        String IDuczniaTMP=OcenasIDucznia.getText().toString();
        String IDprzedmiotuTMP=OcenasIDprzedmiotu.getText().toString();

        JSONObject json = new JSONObject();
        try {
            json.put("WartoscOceny", WartoscOcenyTMP);
            json.put("UczenID", IDuczniaTMP);
            json.put("PrzedmiotID", IDprzedmiotuTMP);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, json, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        OcenasResponseFromRequestTextView.setText("String response : "+ response.toString());
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
                         OcenasResponseFromRequestTextView.setText("Error Get Response " + TrescBledu);
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
