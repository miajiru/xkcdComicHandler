package com.desarrollomovil.angel.xkcdcomichandler;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private ImageView imagen;
    private Button btnTimer;
    private Button btnSalir;
    private ProgressBar progreso;
    private ComicManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imagen = (ImageView) findViewById(R.id.imageView);
        btnTimer = (Button) findViewById(R.id.btnTimer);
        btnSalir = (Button) findViewById(R.id.btnSalir);
        progreso = (ProgressBar) findViewById(R.id.progressBar);
        progreso.setVisibility(View.INVISIBLE);//TODO Ocultamos el progres bar

        //TODO Inicializamos el Comic
        if(comprobarRed()){                 //TODO hay red
            manager = new ComicManager(imagen, 5, this);
        }else{                              //TODO NO hay red
            Toast.makeText(this, "No hay conexión de red", Toast.LENGTH_LONG).show();
        }

    }

    //TODO comprobamos que hay conexión a internet
    public boolean comprobarRed(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if(networkInfo != null && networkInfo.isConnected()){
            return true;
        }
        return false;
    }

    //TODO un botón de activar/desactivar Timer
    public void setTimer(View v) {
        if(manager.getTimerActive()){
            manager.stop();
            btnTimer.setText("Activar el timer");
        }else{
            manager.start();
            btnTimer.setText("Desactivar el timer");
        }
    }

    //TODO un botón para salir de la App
    public void exit(View v) {
        finish();
    }

}
