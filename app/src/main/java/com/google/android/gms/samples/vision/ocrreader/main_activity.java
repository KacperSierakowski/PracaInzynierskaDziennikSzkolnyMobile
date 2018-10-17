package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class main_activity  extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
       /* ButtonAddKlasas=(Button)findViewById(R.id.buttonAddKlasas);
        ButtonAddOcenas=(Button)findViewById(R.id.buttonAddOcenas);
        ButtonAddOcenasMobileVision=(Button)findViewById(R.id.buttonAddOcenasMobileVision);
        ButtonAssNieobecnoscs=(Button)findViewById(R.id.buttonAddNieobecnoscs);
        ButtonAssUczens=(Button)findViewById(R.id.buttonAddUczens);
        ButtonAddPrzedmiots=(Button)findViewById(R.id.buttonAddPrzedmiots);*/
    }

    public void PrzejdzDoDodajOcene(View v){
        Intent intent = new Intent(this, AddOcenasActivity.class);
        startActivity(intent);
    }
    public void PrzejdzDoDodajKlase(View v){
        Intent intent = new Intent(this, AddKlasasActivity.class);
        startActivity(intent);
    }
    public void PrzejdzDoDodajPrzedmiot(View v){
        Intent intent = new Intent(this, AddPrzedmiotsActivity.class);
        startActivity(intent);
    }
    public void PrzejdzDoDodajUcznia(View v){
        Intent intent = new Intent(this, AddUczensActivity.class);
        startActivity(intent);
    }
    public void PrzejdzDoDodajNauczyciela(View v){
        Intent intent = new Intent(this, AddNauczycielsActivity.class);
        startActivity(intent);
    }
    public void PrzejdzDoDodajNieobecnosc(View v){
        Intent intent = new Intent(this, AddNieobecnoscsActivity.class);
        startActivity(intent);
    }
    public void PrzejdzDoDodajOceneMobileVision(View v){
        Intent intent = new Intent(this, OcrCaptureActivity.class);
        startActivity(intent);
    }

}
