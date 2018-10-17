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

public class AddUczensActivity extends AppCompatActivity {


    TextView UczensResponseFromRequestTextView;
    Button UczensbuttonGET,UczensbuttonPOST,UczensbuttonPowrot;
    TextInputEditText UczensImie,UczensNazwisko,UczensTelefon,UczensKlasaID,UczenAdresZamieszkania,UczensLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uczens_layout);
        UczensbuttonPowrot=(Button)findViewById(R.id.UczenPowrotButton);
        UczensResponseFromRequestTextView=(TextView)findViewById(R.id.UczenResponseTextView);
        UczensbuttonGET=(Button)findViewById(R.id.UczenGetButton);
        UczensbuttonPOST=(Button)findViewById(R.id.UczenPostButton);
        UczensImie=
                (TextInputEditText)findViewById(R.id.UczenImieEditText);
        UczensNazwisko=
                (TextInputEditText)findViewById(R.id.UczenNazwiskoEditText);
        UczensTelefon=
                (TextInputEditText)findViewById(R.id.UczenNaumertelefonuEditText);
        UczensKlasaID=
                (TextInputEditText)findViewById(R.id.UczenKlasaIDEditTxt);
        UczenAdresZamieszkania=
                (TextInputEditText)findViewById(R.id.UczenAdreszamieszkaniaEditText);
        UczensLogin=
                (TextInputEditText)findViewById(R.id.UczenLoginEmailEditText);
    }
    public void PowrotDoMain(View v){
        startActivities(new Intent[]{new Intent(this,main_activity.class)});
    }

    static String url ="http://192.168.1.102:45455/api/UczensWEB";
    public void GetUczens(View v){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(AddUczensActivity.this);
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
                        UczensResponseFromRequestTextView.setText("Get Response"+response.toString());
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        UczensResponseFromRequestTextView.setText("Error Get Response "+ error.getMessage());
                    }
                }
        );

        // Add the request to the RequestQueue.
        queue.add(getRequest);

    }

    static final String REQ_TAG = "VACTIVITY";

    public void PostUczen(View v){
        RequestQueue queue = Volley.newRequestQueue(AddUczensActivity.this);
        queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });

        String ImieTMP=UczensImie.getText().toString();
        String NazwiskoTMP=UczensNazwisko.getText().toString();
        String TelefonTMP=UczensTelefon.getText().toString();
        String KlasaIDTMP=UczensKlasaID.getText().toString();
        String AdresZamieszkaniaTMP=UczenAdresZamieszkania.getText().toString();
        String LoginEmailTMP=UczensLogin.getText().toString();

        JSONObject json = new JSONObject();
        try {
            json.put("Imie", ImieTMP);
            json.put("Nazwisko", NazwiskoTMP);
            json.put("NumerTelefonu", TelefonTMP);
            json.put("Adres", AdresZamieszkaniaTMP);
            json.put("Email", LoginEmailTMP);
            json.put("KlasaID", KlasaIDTMP);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        UczensResponseFromRequestTextView.setText("String response : "+ response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                UczensResponseFromRequestTextView.setText("Error getting response" + error.getMessage().toString());
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
