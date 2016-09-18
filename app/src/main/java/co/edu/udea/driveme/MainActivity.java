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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import bluetooth.ConnectedThread;
import joystick.JoystickMovedListener;
import joystick.JoystickView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int DURATION = 150;
    private JoystickView mJoystick;
    private TextView mTxtDataL;
    private boolean mCenterL = true;
    private double mRadiusL = 0;
    private double mAngleL = 0;
    private Handler bluetoothIn;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private ConnectedThread connectedThread;
    private ImageView imageView;
    public TextView andando, parado, flechita;
    public static List<String> dispositivos;
    RelativeLayout relativeLayout;
    //SPP UUID service
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //Aquí se guardara la dirección MAC
    private static String address = null;
    public TextView txt_envoltorio;
    public ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        relativeLayout = (RelativeLayout) findViewById(R.id.envoltorio);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mJoystick = (JoystickView) findViewById(R.id.joystickView);
        mTxtDataL = (TextView) findViewById(R.id.txt_dataL);
        mJoystick.setOnJostickMovedListener(_listenerLeft);
        mJoystick.setAutoReturnToCenter(true);
        imageView = (ImageView) findViewById(R.id.image);
        andando = (TextView) findViewById(R.id.andando);
        parado = (TextView) findViewById(R.id.parado);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        dispositivos = new ArrayList<String>();
        txt_envoltorio = (TextView)findViewById(R.id.tvEnvuelto);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);


        //Se ponen los marcadores de movimiento en la posición inicial
        parado.setAlpha(1);
        andando.setAlpha(0);

        //Con esto buscamos los cambios que hayan en los dispositivos conectados o no con el cel
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter1.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter1.addAction(BluetoothDevice.ACTION_FOUND);

        this.registerReceiver(BTReceiver, filter1);
        checkBTState();
    }

    private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        public void OnMoved(int pan, int tilt) {
            mRadiusL = Math.sqrt((pan * pan) + (tilt * tilt));
            mAngleL = Math.atan2(-pan, -tilt);
            float grados = (float) (mAngleL * 180 / Math.PI);
            mTxtDataL.setText(String.format("( r%.0f, %.0f\u00B0 )", Math.min(mRadiusL, 10), grados));

            if (grados < 22.5 && grados > -22.5) {
                connectedThread.write(1);//adelante ---antes f
                imageView.animate().setDuration(DURATION).rotation(0);
                parado.setAlpha(0);
                andando.setAlpha(1);
            }

            if (grados < -22.5 && grados > -67.5) {
                connectedThread.write(2);//diagonal derecha superior
                imageView.animate().setDuration(DURATION).rotation(45);
                parado.setAlpha(0);
                andando.setAlpha(1);
            }

            if (grados < -67.5 && grados > -112.5) {
                connectedThread.write(3);//derecha --antes d
                imageView.animate().setDuration(DURATION).rotation(90);
                parado.setAlpha(0);
                andando.setAlpha(1);

            }

            if (grados < -112.5 && grados > -157.5) {
                connectedThread.write(4);//diagonal derecha inferior
                imageView.animate().setDuration(DURATION).rotation(135);
                parado.setAlpha(0);
                andando.setAlpha(1);

            }

            if (grados < -157.5 && grados >= -180) {
                imageView.animate().setDuration(DURATION).rotation(180);
                parado.setAlpha(0);
                andando.setAlpha(1);
                connectedThread.write(5);//atras--antes r
            }

            if (grados <= 180 && grados > 157.5) {
                imageView.animate().setDuration(DURATION).rotation(180);
                parado.setAlpha(0);
                andando.setAlpha(1);
                connectedThread.write(5);//atras--antes r
            }

            if (grados > 112.5 && grados < 157.5) {
                imageView.animate().setDuration(DURATION).rotation(225);
                parado.setAlpha(0);
                andando.setAlpha(1);
                connectedThread.write(6);//diagonal izquierda inferior

            }

            if (grados > 67.5 && grados < 112.5) {
                imageView.animate().setDuration(DURATION).rotation(270);
                parado.setAlpha(0);
                andando.setAlpha(1);
                connectedThread.write(7);//dizquierda --antes i

            }

            if (grados < 67.5 && grados > 22.5) {
                imageView.animate().setDuration(DURATION).rotation(315);
                parado.setAlpha(0);
                andando.setAlpha(1);
                connectedThread.write(8);//diagonal izquierda superior

            }
            if (mRadiusL == 0) {
                imageView.animate().setDuration(DURATION).rotation(0);
                parado.setAlpha(1);
                andando.setAlpha(0);
                connectedThread.write(0); //parar antes s
            }
            mCenterL = false;
        }

        public void OnReleased() {
        }

        public void OnReturnedToCenter() {
            mRadiusL = mAngleL = 0;
            mCenterL = true;
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
        unregisterReceiver(BTReceiver);

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        address = getIntent().getStringExtra(DeviceListDialog.EXTRA_DEVICE_ADDRESS);

        if (address != null) {
            relativeLayout = (RelativeLayout) findViewById(R.id.envoltorio);
            relativeLayout.setVisibility(View.GONE);


            TextView t = (TextView) findViewById(R.id.mac);
            TextView y = (TextView) findViewById(R.id.name);
            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            t.setText(address);

            try {
                btSocket = createBluetoothSocket(device);
                y.setText(btSocket.getRemoteDevice().getName());
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
            }

            // Se establece la conexión Bluetooth por medio del Socket
            try {
                btSocket.connect();
                System.out.println(btSocket.isConnected());
            } catch (IOException e) {
                try {
                    btSocket.close();
                    System.out.println("Error");
                } catch (IOException e2) {
                    System.out.println("Error del error");
                }
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

    @Override
    public void onPause() {
        super.onPause();
    }

    //esto es para ejecutar la accion de los cuando el estado del dispositivos conectados cambia
    private final BroadcastReceiver BTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Do something if connected
                // address=BluetoothDevice.
                address = device.getAddress();
                Toast.makeText(getApplicationContext(), "BT Connected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Do something if disconnected
                Toast.makeText(getApplicationContext(), "BT Disconnected", Toast.LENGTH_SHORT).show();
            }
            // Cada vez que se descubra un nuevo dispositivo por Bluetooth, se ejecutara este fragmento de codigo
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Acciones a realizar al descubrir un nuevo dispositivo
                BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                dispositivos.add(dispositivo.getName() + "\n" + dispositivo.getAddress());
            }
            // Codigo que se ejecutara cuando el Bluetooth finalice la busqueda de dispositivos.
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Acciones a realizar al finalizar el proceso de descubrimiento
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
            DialogFragment dialog = new DeviceListDialog();
            dialog.show(getSupportFragmentManager(), "Lista de dispositivos.");
        } else if (id == R.id.close) {
            finish();
        }else if (id == R.id.search){
            if(dispositivos != null)
                dispositivos.clear();
            // Comprobamos si existe un descubrimiento en curso. En caso afirmativo, se cancela.
            if(btAdapter.isDiscovering())
                btAdapter.cancelDiscovery();

            // Iniciamos la busqueda de dispositivos y mostramos el mensaje de que el proceso ha comenzado
            if(btAdapter.startDiscovery()) {
                relativeLayout = (RelativeLayout) findViewById(R.id.envoltorio);
                relativeLayout.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                txt_envoltorio = (TextView) findViewById(R.id.tvEnvuelto);
                flechita = (TextView)findViewById(R.id.flechita);
                flechita.setVisibility(View.GONE);

                Toast.makeText(this, "Iniciando búsqueda de dispositivos bluetooth.\n " +
                        "El proceso tiene una duración de 12 segundos. ", Toast.LENGTH_SHORT).show();

                txt_envoltorio.setText("Buscando dispositivos. Espere 12 segundos.");
            }
            else
                Toast.makeText(this, "Error al iniciar búsqueda de dispositivos bluetooth", Toast.LENGTH_SHORT).show();
            }
        return super.onOptionsItemSelected(item);
    }

    public void nothing(View view) {
    }

    @Override
    public void onClick(View v) {
    }

}