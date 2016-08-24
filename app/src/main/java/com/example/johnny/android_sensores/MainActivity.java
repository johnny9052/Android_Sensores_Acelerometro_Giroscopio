package com.example.johnny.android_sensores;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // https://elbauldelprogramador.com/giroscopio-acelerometro-movimientos-android/

    /****************RECORDAR COLOCAR LOS PERMISOS***********************************/

    /*
    * GIROSCOPIO: Detecta movimiento y rotación del dispositivo (Es mas preciso)
    * ACELEROMETRO: Detecta movimiento (Aceleracion) pero no rotacion del dispositivo
    *
    */

    /**
     * Constants for sensors
     */

    //aumenta o disminuye la sensibilidad de una sacudida
    /*con la f se especifica que es un float (cosa que no pasa en los double), ademas con este se
    * omite esa gran cantidad de decimales que se pueden comparar contra el, redondeando cualquier
    * numero operado con el*/
    private static final float LIMITE_SENSIBILIDAD_SACUDIDA = 1.1f;
    /*Milisegundos para determinar si hubo sacudida (RETARDO ENTRE SACUDIDA Y SACUDIDA)*/
    private static final int LIMITE_TIEMPO_MS_SACUDIDA = 250;
    //aumenta o disminuye la sensibilidad de un movimiento (Subida, bajada, izq, der, adel, atras)
    /*con la f se especifica que es un float (cosa que no pasa en los double), ademas con este se
    * omite esa gran cantidad de decimales que se pueden comparar contra el, redondeando cualquier
    * numero operado con el*/
    private static final float LIMITE_SENSIBILIDAD_ROTACION = 2.0f;
    /*Milisegundos para determinar si hubo movimiento (RETARDO ENTRE MOVIMIENTO Y MOVIMIENTO)*/
    private static final int LIMITE_TIEMPO_MS_ROTACION = 100;

    /**
     * Referencia a audios que se reproduciran si se sacude o se meuve
     */
    private static MediaPlayer audioAcelerometro;
    private static MediaPlayer audioGiroscopio;

    /**
     * Sensors
     */

    //Administrador de sensores
    private SensorManager sensorManager;
    //Sensor acelerometro
    private Sensor sensorAcelerometro;
    //Sensor giroscopio
    private Sensor sensorGiroscopio;


    /*Almacena el tiempo actual de la sacudida, para cuando se haga una proxima, haya un retardo*/
    private long tiempoSacudida = 0;
    /*Almacena el tiempo actual del movimiento, para cuando se haga una proxima, haya un retardo*/
    private long tiempoRotacion = 0;

    /**
     * GUI
     */
    private TextView txtGiroscopioX;
    private TextView txtGiroscopioY;
    private TextView txtGiroscopioZ;
    private TextView txtAcelerometroX;
    private TextView txtAcelerometroY;
    private TextView txtAcelerometroZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SE INICIALIZAN LOS SENSORES

        //Administrador de los sensores
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //Acelerometro
        sensorAcelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Giroscopio
        sensorGiroscopio = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Instanciate the sound to use
        audioAcelerometro = MediaPlayer.create(this, R.raw.acc);
        audioGiroscopio = MediaPlayer.create(this, R.raw.gyro);

        /*Referencia a inputs*/
        txtGiroscopioX = (TextView) findViewById(R.id.gyro_x);
        txtGiroscopioY = (TextView) findViewById(R.id.gyro_y);
        txtGiroscopioZ = (TextView) findViewById(R.id.gyro_z);
        txtAcelerometroX = (TextView) findViewById(R.id.accele_x);
        txtAcelerometroY = (TextView) findViewById(R.id.accele_y);
        txtAcelerometroZ = (TextView) findViewById(R.id.accele_z);
    }

    /*Tan pronto se activan los sensores, se asocian al gestionador de sensores*/
    @Override
    protected void onResume() {
        super.onResume();
        /*Se asocia el acelerometro al administrador*/
        sensorManager.registerListener(this, sensorAcelerometro, SensorManager.SENSOR_DELAY_NORMAL);
        /*Se asocia el giroscopio al administrador*/
        sensorManager.registerListener(this, sensorGiroscopio, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /*Cuando se pausa un sensor*/
    @Override
    protected void onPause() {
        /*Si se pausa, se des-asocia del administrador de sensores*/
        super.onPause();
        /*El this es el sensor que se pauso*/
        sensorManager.unregisterListener(this);
    }


    /*Funcion que se ejecuta cuando cambia los valores del sensor*/
    @Override
    public void onSensorChanged(SensorEvent event) {

        /*Si el dispisitivo no tiene un sensor*/
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {

            /*Lo que no tiene es el acelerometro*/
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                /*Actualice las coordenadas*/
                txtAcelerometroX.setText("Sin Suficiente presición");
                txtAcelerometroY.setText("Sin Suficiente presición");
                txtAcelerometroZ.setText("Sin Suficiente presición");
                /*Lo que no tiene es giroscopio*/
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                txtGiroscopioX.setText("Sin Suficiente presición");
                txtGiroscopioY.setText("Sin Suficiente presición");
                txtGiroscopioZ.setText("Sin Suficiente presición");
            }
            return;
        }

        /*Se obtuvo datos del acelerometro?*/
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            /*Actualice los datos del acelerometro*/
            txtAcelerometroX.setText("x = " + Float.toString(event.values[0]));
            txtAcelerometroY.setText("y = " + Float.toString(event.values[1]));
            txtAcelerometroZ.setText("z = " + Float.toString(event.values[2]));
            /*Se determina si se tuvo una sacudida (Con otro componente que es la gravedad)*/
            detectShake(event);
            /**/
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            txtGiroscopioX.setText("x = " + Float.toString(event.values[0]));
            txtGiroscopioY.setText("y = " + Float.toString(event.values[1]));
            txtGiroscopioZ.setText("z = " + Float.toString(event.values[2]));
            /*Se determina si se tuvo una rotacion*/
            detectRotation(event);
        }

    }

    /**
     * Detecta una sacudida a partir del acelerometro
     */
    private void detectShake(SensorEvent event) {

        /*Se captura el tiempo actual*/
        long now = System.currentTimeMillis();

        /*Si esta sacudida supera el tiempo estipulado para la proxima sacudida*/
        if ((now - tiempoSacudida) > LIMITE_TIEMPO_MS_SACUDIDA) {

            /*Se almacena para ser utilizado en una proxima sacudida*/
            tiempoSacudida = now;

            /*Se obtiene el movimiento en X, Y, Z y se divide por la gravedad para detercar una
            *  sacudida*/
            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            /*Se calcula con la operacion matematica el cambio gravitacional*/
            double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            //Si supera el limite establecido, suena el audio
            if (gForce > LIMITE_SENSIBILIDAD_SACUDIDA) {
                audioAcelerometro.start();
            }
        }
    }

    /**
     *
     */
    private void detectRotation(SensorEvent event) {

        /*Se captura el tiempo actual*/
        long now = System.currentTimeMillis();

         /*Si esta rotacion supera el tiempo estipulado para la proxima rotacion*/
        if ((now - tiempoRotacion) > LIMITE_TIEMPO_MS_ROTACION) {

            /*Se almacena para ser utilizado en una proxima rotacion*/
            tiempoRotacion = now;

            /*El valor absoluto en X o Y o Z supera el limite establecido*/
            if (Math.abs(event.values[0]) > LIMITE_SENSIBILIDAD_ROTACION ||
                    Math.abs(event.values[1]) > LIMITE_SENSIBILIDAD_ROTACION ||
                    Math.abs(event.values[2]) > LIMITE_SENSIBILIDAD_ROTACION) {
                /*Sonar el audio*/
                audioGiroscopio.start();
            }
        }
    }

    // Metodo que escucha el cambio de sensibilidad de los sensores
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}