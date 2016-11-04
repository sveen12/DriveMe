package co.edu.udea.driveme;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import bluetooth.ConnectedThread;
import joystick.JoystickMovedListener;
import joystick.JoystickView;

public class MainActivity extends AppCompatActivity implements DeviceListDialog.RecibirDatos {

    private final String TAG= "MainActivity.class";
    private final int DURATION = 150;
    private JoystickView joystickView;
    private double radio = 0;
    private double angulo = 0;
    private Handler bluetoothIn;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private ConnectedThread connectedThread;
    private ProgressBar progressBar;
    private ImageView imageView;
    private Switch swActivarSensores;
    private TextView tvAndando,
            tvParado,
            tvFlecha,
            tvEnvuelto,
            tvRadioAngulo,
            tvTemperatura,
            tvDistancia,
            tvHumedad,
            tvMac,
            tvName;

    public static List<String> dispositivos;
    public static List<String> nombresDispositivos;
    private RelativeLayout rlEnvoltorio;
    //SPP UUID service
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //Aquí se guardara la dirección MAC
    private static String macAddress = null;
    private StringBuilder recDataString = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        encontrarGraficos();
        read();


        //Se ponen los marcadores de movimiento en la posición inicial
        tvParado.setAlpha(1);
        tvAndando.setAlpha(0);

        //Con esto buscamos los cambios que hayan en los dispositivos conectados o no con el cel
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);

        this.registerReceiver(BTReceiver, filter);
        checkBTState();
    }

    public void encontrarGraficos(){
        joystickView = (JoystickView) findViewById(R.id.joystickView);
        joystickView.setOnJostickMovedListener(_listenerLeft);
        joystickView.setAutoReturnToCenter(true);

        imageView = (ImageView) findViewById(R.id.image);
        swActivarSensores = (Switch) findViewById(R.id.swSensores);
        rlEnvoltorio = (RelativeLayout) findViewById(R.id.rlEnvoltorio);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tvRadioAngulo = (TextView) findViewById(R.id.tvRadioAngulo);
        tvAndando = (TextView) findViewById(R.id.tvAndando);
        tvParado = (TextView) findViewById(R.id.tvParado);
        tvEnvuelto = (TextView)findViewById(R.id.tvEnvuelto);
        tvDistancia =(TextView) findViewById(R.id.tvDistancia);
        tvHumedad =(TextView) findViewById(R.id.tvHumedad);
        tvTemperatura = (TextView) findViewById(R.id.tvTemperatura);
        tvEnvuelto = (TextView) findViewById(R.id.tvEnvuelto);
        tvFlecha = (TextView)findViewById(R.id.tvFlecha);
        tvMac = (TextView) findViewById(R.id.tvMac);
        tvName = (TextView) findViewById(R.id.tvName);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        dispositivos = new ArrayList<String>();
        nombresDispositivos = new ArrayList<String>();

        tvDistancia.setVisibility(View.GONE);
        tvHumedad.setVisibility(View.GONE);
        tvTemperatura.setVisibility(View.GONE);

        swActivarSensores.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tvDistancia.setVisibility(View.VISIBLE);
                    tvHumedad.setVisibility(View.VISIBLE);
                    tvTemperatura.setVisibility(View.VISIBLE);
                } else {
                    tvDistancia.setVisibility(View.GONE);
                    tvHumedad.setVisibility(View.GONE);
                    tvTemperatura.setVisibility(View.GONE);
                }
            }
        });


    }

    private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        public void OnMoved(int pan, int tilt) {
            radio = Math.sqrt((pan * pan) + (tilt * tilt));
            angulo = Math.atan2(-pan, -tilt);
            float grados = (float) (angulo * 180 / Math.PI);
            tvRadioAngulo.setText(String.format("( r%.0f, %.0f\u00B0 )", Math.min(radio, 10), grados));

            if (grados < 22.5 && grados > -22.5) {
                connectedThread.write(1);//adelante ---antes f
                imageView.animate().setDuration(DURATION).rotation(0);
                tvParado.setAlpha(0);
                tvAndando.setAlpha(1);
            }

            if (grados < -22.5 && grados > -67.5) {
                connectedThread.write(2);//diagonal derecha superior
                imageView.animate().setDuration(DURATION).rotation(45);
                tvParado.setAlpha(0);
                tvAndando.setAlpha(1);
            }

            if (grados < -67.5 && grados > -112.5) {
                connectedThread.write(3);//derecha --antes d
                imageView.animate().setDuration(DURATION).rotation(90);
                tvParado.setAlpha(0);
                tvAndando.setAlpha(1);

            }

            if (grados < -112.5 && grados > -157.5) {
                connectedThread.write(4);//diagonal derecha inferior
                imageView.animate().setDuration(DURATION).rotation(135);
                tvParado.setAlpha(0);
                tvAndando.setAlpha(1);

            }

            if (grados < -157.5 && grados >= -180) {
                imageView.animate().setDuration(DURATION).rotation(180);
                tvParado.setAlpha(0);
                tvAndando.setAlpha(1);
                connectedThread.write(5);//atras--antes r
            }

            if (grados <= 180 && grados > 157.5) {
                imageView.animate().setDuration(DURATION).rotation(180);
                tvParado.setAlpha(0);
                tvAndando.setAlpha(1);
                connectedThread.write(5);//atras--antes r
            }

            if (grados > 112.5 && grados < 157.5) {
                imageView.animate().setDuration(DURATION).rotation(225);
                tvParado.setAlpha(0);
                tvAndando.setAlpha(1);
                connectedThread.write(6);//diagonal izquierda inferior
            }

            if (grados > 67.5 && grados < 112.5) {
                imageView.animate().setDuration(DURATION).rotation(270);
                tvParado.setAlpha(0);
                tvAndando.setAlpha(1);
                connectedThread.write(7);//dizquierda --antes i
            }

            if (grados < 67.5 && grados > 22.5) {
                imageView.animate().setDuration(DURATION).rotation(315);
                tvParado.setAlpha(0);
                tvAndando.setAlpha(1);
                connectedThread.write(8);//diagonal izquierda superior
            }

            if (radio == 0) {
                imageView.animate().setDuration(DURATION).rotation(0);
                tvParado.setAlpha(1);
                tvAndando.setAlpha(0);
                connectedThread.write(0); //parar antes s
            }
        }

        public void OnReleased() {
        }

        public void OnReturnedToCenter() {
            radio = angulo = 0;
        }
    };

    public void checkBTState() {
        if (btAdapter == null) {
            Toast.makeText(this, "El disositivo no soporta bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (btAdapter.isEnabled()) {

            } else {
                Intent enaIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enaIntent, 1);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(BTReceiver);
        cerrarConexion();
    }

    @Override
    public void onResume() {
        super.onResume();

        connectDevice();
        read();
    }

    public void connectDevice(){
        if (macAddress != null) {
            BluetoothDevice device = btAdapter.getRemoteDevice(macAddress);

            try {
                btSocket = createBluetoothSocket(device);
            }catch (IOException e) {
                Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
            }

            // Se establece la conexión Bluetooth por medio del Socket
            try {
                btSocket.connect();
                Log.i(TAG, "El socket se conectó: "+btSocket.isConnected());

            } catch (IOException e) {
                cerrarConexion();
            }

            connectedThread = new ConnectedThread(btSocket, bluetoothIn);
            connectedThread.start();

            //Se manda un dato de prueba para saber si se hizo la conexión correctamente
            connectedThread.write(9);
        } else {
            Toast.makeText(this, "Debe seleccionar un dispositivo\n bluetooth primero.", Toast.LENGTH_SHORT).show();
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    //esto es para ejecutar la accion de los cuando el estado del dispositivos conectados cambia
    private final BroadcastReceiver BTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Do something if connected
                macAddress = device.getAddress();

                rlEnvoltorio.setVisibility(View.GONE);

                tvMac.setText(macAddress);
                tvName.setText(btSocket.getRemoteDevice().getName());

                Toast.makeText(getApplicationContext(), "Se estableció la conexión.", Toast.LENGTH_SHORT).show();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Do something if disconnected
                Toast.makeText(getApplicationContext(), "Se cerró la conexión.", Toast.LENGTH_SHORT).show();
            }
            // Cada vez que se descubra un nuevo dispositivo por Bluetooth, se ejecutara este fragmento de codigo
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Acciones a realizar al descubrir un nuevo dispositivo
                BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                dispositivos.add(dispositivo.getName() + "\n" + dispositivo.getAddress());
                nombresDispositivos.add(dispositivo.getName());
            }
            // Codigo que se ejecutara cuando el Bluetooth finalice la busqueda de dispositivos.
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Acciones a realizar al finalizar el proceso de descubrimiento
                progressBar.setVisibility(View.GONE);
                tvFlecha.setVisibility(View.VISIBLE);
                tvEnvuelto.setText("Seleccione un dispositivo Bluetooth, por favor.");
                DialogFragment dialog = new DeviceListDialog();
                dialog.show(getSupportFragmentManager(), "Lista de dispositivos.");
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.connect) {
            progressBar.setVisibility(View.GONE);
            tvFlecha.setVisibility(View.VISIBLE);
            rlEnvoltorio.setVisibility(View.VISIBLE);
            DialogFragment dialog = new DeviceListDialog();
            dialog.show(getSupportFragmentManager(), "Lista de dispositivos.");
        } else if (id == R.id.close) {
            finish();
        }else if (id == R.id.search){
            if(dispositivos != null)
                dispositivos.clear();
                nombresDispositivos.clear();
            // Comprobamos si existe un descubrimiento en curso. En caso afirmativo, se cancela.
            if(btAdapter.isDiscovering())
                btAdapter.cancelDiscovery();

            // Iniciamos la busqueda de dispositivos tvName mostramos el mensaje de que el proceso ha comenzado
            if(btAdapter.startDiscovery()) {

                rlEnvoltorio.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                tvFlecha.setVisibility(View.GONE);

                Toast.makeText(this, "Iniciando búsqueda de dispositivos bluetooth.\n " +
                        "El proceso tiene una duración de 12 segundos. ", Toast.LENGTH_SHORT).show();

                tvEnvuelto.setText("Buscando dispositivos. Espere 12 segundos.");
            }else
                Toast.makeText(this, "Error al iniciar búsqueda de dispositivos bluetooth", Toast.LENGTH_SHORT).show();
            }
        return super.onOptionsItemSelected(item);
    }


    public void read(){
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == 0) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~

                    int endOfLineIndex = recDataString.indexOf("\n");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~

                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        String[] variables = dataInPrint.split("~");
                        tvDistancia.setText(variables[0]);
                        Log.e(TAG,recDataString.toString());
                        Log.e(TAG,String.valueOf(variables.length));

                        if(variables.length>2) {
                            tvTemperatura.setText(variables[1]);
                            tvHumedad.setText(variables[2]);
                        }
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                    }
                }
            }
        };
    }

    @Override
    public void deviceMac(String address) {
       cerrarConexion();
        this.macAddress =address;
        connectDevice();
    }

    public boolean cerrarConexion(){
        try {
            btSocket.close();
            return true;
        } catch (IOException e){
            Log.e(TAG, e.getMessage());
        }catch (NullPointerException nu ){
            Log.e(TAG, "No existe un Socket para cerrar.");
        }
        return false;
    }
}