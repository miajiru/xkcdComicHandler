package com.desarrollomovil.angel.xkcdcomichandler;

/**
 * Created by angel on 24/01/2017.
 */
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static com.desarrollomovil.angel.xkcdcomichandler.ComicManager.*;


public class DownloadHandler extends Handler {

    private HttpURLConnection conexion = null;
    private String urlJson = null;
    private boolean ultimoComic = true;
    public static int ultimoNum = -1;
    private Looper looperDownload;
    Bundle b;
    Handler mHandler;
    Context contexto;
    Message msg;

    public DownloadHandler(Looper looperDownload,Context contexto) {
        this.looperDownload = looperDownload;
        mHandler = new Handler(this.looperDownload);
        b = new Bundle();
        this.contexto = contexto;
    }

    @Override
    public void handleMessage(Message msg) {

        switch(msg.what) {

            case (DOWNLOAD):
                if (urlJson == null) {
                    urlJson = "https://xkcd.com/info.0.json";    //TODO ultmio comic publicado; el primero que cargaremos en la imageView
                } else {
                    int numAleatorio = (int) (Math.random() * ultimoNum) + 1;
                    urlJson = "https://xkcd.com/" + numAleatorio + "/info.0.json";    //TODO comics aleatorios
                }
                mHandler.post(new Runnable() {//TODO Encolamos el proceso de descarga
                    @Override
                    public void run() {
                        accederJSON();
                    }
                });
                break;

        }
    }

    public void accederJSON(){

        ComicManager.ImageHandler mHandlerStatico = new ComicManager.ImageHandler(getLooper(),contexto);

        try{
            URL urlObject = new URL(urlJson);           // TODO url del json
            conexion = (HttpURLConnection) urlObject.openConnection();
            conexion.setRequestMethod("HEAD");          //TODO Petición del recurso
            conexion.connect();         ////TODO 1ª conexion

            if(conexion.getResponseCode() == 200){//TODO Conexion correcta, existe el recurso o ha sido movido
                msg = mHandlerStatico.obtainMessage(PROGRESS);//TODO Actualizamos el progress bar
                mHandlerStatico.sendMessage(msg);

                String urlImagen = descargar(urlObject);//TODO Extraemos la url de la imagen del JSON
                File ficheroTemp = creaFicheroTemp(contexto, urlImagen); //TODO Pedimos la creacion del fichero temporal
                URI uri = escribirFicheroTemp(ficheroTemp,urlImagen);   //TODO Obtenemos la URI del archivo temp() con datos.

                if(uri != null){
                    //TODO descragamos una imagen, enviamos un mensaje LOAD_IMAGE al UI Thread y la URI del archivo descargado.
                    msg = mHandlerStatico.obtainMessage(LOAD_IMAGE);
                    b.putSerializable("uri",uri);
                    msg.setData(b);
                    mHandlerStatico.sendMessageDelayed(msg,2000);   //TODO manadamos un mensaje (carga imagen), y un bundle que contiene la uri de la imagen con retardo
                }else{  //TODO error al descargar la imagen
                    msg = mHandlerStatico.obtainMessage(ERROR);
                    b.putString("Error","Error al descargar la imagen.");
                    msg.setData(b);
                    mHandlerStatico.sendMessage(msg);
                    mHandler.sendMessage(msg);
                }

            } else {//TODO Error al solicitar el recurso JSON devolvemos codigo de respuesta
                Message msg = mHandlerStatico.obtainMessage(ERROR);
                b.putString("Error",String.valueOf(conexion.getResponseMessage()+" --> "+conexion.getResponseCode()));
                msg.setData(b);
                mHandlerStatico.sendMessage(msg);
                mHandler.sendMessage(msg);
            }
        }catch(MalformedURLException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String descargar(URL urlObject){
        String cadena = "";
        InputStream in = null;
        BufferedInputStream leer = null;

        try {
            in = urlObject.openStream();        //TODO Accedemos al flujo de datos
            leer = new BufferedInputStream(in);
            int n;
            while((n=leer.read())>0){
                cadena += (char)n;      //TODO Cadena con el JSON
            }
            String url = extraerUrlImagen(cadena);    //TODO Procesamos el JSON para extraer la url de la imagen
            return url;                     //TODO Devolvemos la url de la imagen a descargar
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;                    //TODO ha habido una excepción devolvemos null
    }

    public String extraerUrlImagen(String cadena){        //TODO Procesamos el JSON para extraer la url de la imagen
        String urlJson = "";
        try {
            JSONObject json = new JSONObject(cadena);
            urlJson = json.getString("img");
            if(ultimoNum == -1){    //TODO si el numero es -1 es que no tenemos el num del último comic
                extraerNum(cadena); //TODO capturamos el num del último comic
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return urlJson;
    }

    public void extraerNum(String cadena){  //TODO Extraer el número del último comic

        try {
            JSONObject json = new JSONObject(cadena);
            ultimoNum = json.getInt("num"); //TODO asignamos a la var estática el num de último comic
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private File creaFicheroTemp(Context context, String url){  //TODO creamos un fichero tem en la cache del dispositivo

        File outputDir = context.getExternalCacheDir();
        File outputFile = null;

        String fileName = Uri.parse(url).getLastPathSegment();
        try {
            outputFile = File.createTempFile(fileName, null, outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        outputDir.deleteOnExit();
        outputFile.deleteOnExit();

        return outputFile;
    }

    public URI escribirFicheroTemp(File ficheroTemp, String url){
        URI uri = null;
        try {

            URL urlObject = new URL(url);
            FileOutputStream fileOutput = new FileOutputStream(ficheroTemp);//TODO Escribir flujo en temp

            InputStream input = null;           //TODO Flujo de datos de lectura
            input = urlObject.openStream();     //TODO Abrimos conexion

            int n;
            byte[] buf = new byte[2048];

            while ((n = input.read(buf)) > 0) {     //TODO Leemos el flujo de datos y guardamos
                fileOutput.write(buf, 0, n);        //TODO Guardamos en el array de bytes
                fileOutput.flush();                 //TODO Vaciamos el buffer y garantizamos la escritura en el fichero
            }
            fileOutput.close();                     //TODO Cerramos flujo de escritura
            input.close();                          //TODO Cerramos flujo lectura

            uri = ficheroTemp.toURI();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return uri;     //TODO devolvemos la uri
    }
}
