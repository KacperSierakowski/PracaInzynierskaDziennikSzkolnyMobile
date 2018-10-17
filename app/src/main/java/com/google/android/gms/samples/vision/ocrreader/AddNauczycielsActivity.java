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

public class AddNauczycielsActivity extends AppCompatActivity {

    TextView NauczycielsResponseFromRequestTextView;
    Button NauczycielsbuttonGET,NauczycielsbuttonPOST,NauczycielsbuttonPowrot;
    TextInputEditText NauczycielsImie,NauczycielsNazwisko,NauczycielsTelefon,NauczycielsKlasaID,NauczycielsAdresZamieszkania,NauczycielsLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nauczyciels_layout);
        NauczycielsbuttonPowrot=(Button)findViewById(R.id.NauczycielPowrotButton);
        NauczycielsResponseFromRequestTextView=(TextView)findViewById(R.id.NauczycielResponseTextView);
        NauczycielsbuttonGET=(Button)findViewById(R.id.NauczycielGetButton);
        NauczycielsbuttonPOST=(Button)findViewById(R.id.NauczycielPostButton);
        NauczycielsImie=
                (TextInputEditText)findViewById(R.id.NauczycielImieEditText);
        NauczycielsNazwisko=
                (TextInputEditText)findViewById(R.id.NauczycielNazwiskoEditText);
        NauczycielsTelefon=
                (TextInputEditText)findViewById(R.id.NauczycielNaumertelefonuEditText);
        NauczycielsAdresZamieszkania=
                (TextInputEditText)findViewById(R.id.NauczycielAdreszamieszkaniaEditText);
        NauczycielsLogin=
                (TextInputEditText)findViewById(R.id.NauczycielLoginEmailEditText);
    }
    public void PowrotDoMain(View v){
        startActivities(new Intent[]{new Intent(this,main_activity.class)});
    }

    static String url ="http://192.168.1.102:45455/api/NauczycielsWEB";
    public void GetNauczyciels(View v){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(AddNauczycielsActivity.this);
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
                        NauczycielsResponseFromRequestTextView.setText("Get Response"+response.toString());
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NauczycielsResponseFromRequestTextView.setText("Error Get Response "+ error.getMessage());
                    }
                }
        );

        // Add the request to the RequestQueue.
        queue.add(getRequest);

    }

    static final String REQ_TAG = "VACTIVITY";

    public void PostNauczyciel(View v){
        RequestQueue queue = Volley.newRequestQueue(AddNauczycielsActivity.this);
        queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });

        String ImieTMP=NauczycielsImie.getText().toString();
        String NazwiskoTMP=NauczycielsNazwisko.getText().toString();
        String TelefonTMP=NauczycielsTelefon.getText().toString();
        String AdresZamieszkaniaTMP=NauczycielsAdresZamieszkania.getText().toString();
        String LoginEmailTMP=NauczycielsLogin.getText().toString();

        JSONObject json = new JSONObject();
        try {
            json.put("Imie", ImieTMP);
            json.put("Nazwisko", NazwiskoTMP);
            json.put("NumerTelefonu", TelefonTMP);
            json.put("Adres", AdresZamieszkaniaTMP);
            json.put("Email", LoginEmailTMP);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        NauczycielsResponseFromRequestTextView.setText("String response : "+ response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NauczycielsResponseFromRequestTextView.setText("Error getting response" + error.getMessage().toString());
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
