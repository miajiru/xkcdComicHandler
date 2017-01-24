package com.desarrollomovil.angel.xkcdcomichandler;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by angel on 24/01/2017.
 */
public class ComicManager {

    private HandlerThread downloadHandlerThread;
    private DownloadHandler downloadHandler; // Funcionará asociado al Worker Thread (HandlerThread)
    private ImageHandler imageHandler;            // Funcionará asociado al UI Thread
    private boolean timerActive;             // Controlamos si el timer está activo o no
    private long seconds;                     // Segundos del timer
    Context contexto;
    private Handler mHandler;
    private Handler mHandlerMain;
    private Timer timer;
    private TimerTask temporizador = null;

    static final int DOWNLOAD = 0;//TODO Message descargar último comic
    static final int LOAD_IMAGE = 1;
    static final int PROGRESS = 2;
    static final int ERROR = 3;

    public ComicManager(ImageView imageView, int secondsTimer, Context contexto) {
        //TODO Aquí inicializamos el HandlerThread y el DownloadHandler usando el Looper de HandlerThread
        downloadHandlerThread = new HandlerThread("downloadLooper");
        //TODO Arrancamos el HandlerThread.
        downloadHandlerThread.start();
        mHandler = new Handler(downloadHandlerThread.getLooper());//TODO handler para 'HandlerThread'
        downloadHandler = new DownloadHandler(downloadHandlerThread.getLooper(),contexto);
        //TODO Inicializamos la imageHandler a partir de la static inner class definida posteriormente, asociandola al UI Looper
        imageHandler = new ImageHandler(Looper.getMainLooper(), contexto);
        mHandlerMain = new Handler(Looper.getMainLooper());//TODO handler para  'ImageHandler'
        //TODO Inicializamos la temporalización
        seconds = secondsTimer * 1000; //TODO Pasamos a milisegundos
        this.contexto = contexto;
        start();
    }

    public void start() {
        //TODO llamamos a downloadComic una vez
        timerActive = true;
        //downloadComic();
        startTimer();
    }

    public void stop() {
        //TODO Enviamos un Toast de que se está parando la aplicación
        mHandlerMain.post(new Runnable() {//TODO encolamos en el main looper(activity)
            @Override
            public void run() {
                Toast.makeText(contexto, "La aplicación se esta parando...", Toast.LENGTH_SHORT).show();
            }
        });

        timerActive = false;
        //TODO Desactivamos el timer para que evite enviar mensajes a un HandlerThread que ya no existirá.
        stopTimer();
        //TODO Paramos el HandlerThread, limpiando su cola de mensajes y esperando a que acabe su trabajo activo si lo tiene
        mHandler.removeCallbacks(temporizador);
    }

    public void startTimer() {
        // TODO activamos el timer y configuramos el time
        //TODO código sacado de 'http://www.tutorial-es.com/android-ejemplo-timer/'
        mHandler = new Handler(downloadHandlerThread.getLooper());
        temporizador = new TimerTask(){
            @Override
            public void run() {
                mHandler.post(new Runnable(){
                    public void run() {
                        Message msg = downloadHandler.obtainMessage(DOWNLOAD);
                        downloadHandler.sendMessage(msg);
                    }
                });
            }
        };

        timer = new Timer();
        timer.schedule(temporizador, 100,seconds);//TODO se crea un hilo
    }

    public void stopTimer() {
        //TODO desactivamos el timer
        timer.cancel();
        temporizador.cancel();
    }

    public boolean getTimerActive() {
        return timerActive;
    }

    //TODO Aquí declararemos una static inner class Handler

    public static class ImageHandler extends Handler {

        private ImageView image;
        private ProgressBar progreso;
        private Looper loop;
        private Activity activity;
        private URI uri;
        Bundle b;
        WeakReference<Context> weakRef = null;
        Handler mHandler;

        public ImageHandler(Looper mainLooper, final Context contexto) {
            loop = mainLooper;
            mHandler = new Handler(loop);   //TODO Main looper
            weakRef = new WeakReference<Context>(contexto);     //TODO referencia al contexto de la activity
            this.activity = (Activity) weakRef.get();
        }

        public void handleMessage(Message msg) {

            image = (ImageView) this.activity.findViewById(R.id.imageView);
            progreso = (ProgressBar) this.activity.findViewById(R.id.progressBar);

            switch (msg.what) {
                case (LOAD_IMAGE):
                    //TODO cargamos la imagen deltemporal a la activity
                    if(activity != null){
                        b = msg.getData();
                        uri = (URI) b.get("uri");       //TODO Obtenemos la URI del archivo temporal y cargamos el imageView
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {             //TODO Encolamos un Runable que actualizará la activity
                                image.setImageDrawable(Drawable.createFromPath(uri.getPath()));
                                progreso.setVisibility(View.INVISIBLE);//TODO Termina la descarga el progresbar se oculta

                            }
                        });
                    }
                    break;

                case (PROGRESS):
                    //TODO actualizaremos el progressBar
                    if(activity != null){           //TODO comprobamos que la activity sigue existiendo
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progreso.setVisibility(View.VISIBLE);//TODO Comienza la descarga el progresbar se hace visible
                            }
                        });
                    }
                    break;

                case (ERROR):
                    //TODO mostraremos un Toast del error.
                    if(activity != null){       //TODO comprobamos que la activity sigue existiendo
                        b = msg.getData();
                        String error = b.getString("Error");//TODO Obtenemos el mensaje de error concreto enviado
                        Toast.makeText(weakRef.get(), error, Toast.LENGTH_SHORT).show();
                    }
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

}


