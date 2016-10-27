package co.edu.udea.driveme;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceListDialog extends DialogFragment {

    // Debugging for LOGCAT
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;
    private CheckBox checkBox;
    private ProgressBar progressBar;



    // declare button for launching website and textview for connection status
    TextView textView1;

    // EXTRA string to send on to mainactivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    public static ArrayAdapter mPairedDevicesArrayAdapter;
    public static ArrayAdapter nombresDispositivos;
    public ListView pairedListView;

    public interface RecibirDatos{
        public void deviceMac(String address);
    }
    RecibirDatos recibirDatos;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            recibirDatos = (RecibirDatos) activity;
        }catch (ClassCastException e){
            throw new ClassCastException(activity.toString() + " must implement Recibir Datos");
        }
    }

    // Set up on-click listener for the list (nicked this - unsure)
     private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View view, int position, long id) {


            /*
            textView1.setText("Conectando...");


            // Make an intent to start next activity while taking an extra which is the MAC address.
            //MainActivity.EXTRA_DEVICE_ADDRESS = address;
            if (MainActivity.DEVICE_CONNECTED==true) {*/
                view.setBackgroundColor(Color.parseColor("#CC666666"));
                progressBar.setVisibility(View.VISIBLE);
                // Get the device MAC address, which is the last 17 chars in the View


                String info = mPairedDevicesArrayAdapter.getItem(position).toString();
                String address = info.substring(info.length() - 17);

                recibirDatos.deviceMac(address);
                dismiss();
            /*
                Intent i = new Intent(getActivity(), MainActivity.class);
                i.putExtra(EXTRA_DEVICE_ADDRESS, address);
                getActivity().finish();
                startActivity(i);
            }else {
                textView1.setText("Error de conexión");
            }*/
        }
    };

    public static void addBTDevice(String device){
        mPairedDevicesArrayAdapter.add(device);
        nombresDispositivos.add(device);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_device_list, container);
        view.setPadding(20,20,20,20);

        checkBTState();

        textView1 = (TextView) view.findViewById(R.id.connecting);
        textView1.setTextSize(40);
        textView1.setText(" ");
        checkBox = (CheckBox) view.findViewById(R.id.cbMac);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBarDialog);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(isChecked){
                    pairedListView.setAdapter(mPairedDevicesArrayAdapter);
                }else{
                    pairedListView.setAdapter(nombresDispositivos);
                }
            }
        });

        // Initialize array adapter for paired devices
        mPairedDevicesArrayAdapter = new ArrayAdapter(view.getContext(), R.layout.device_name);
        nombresDispositivos = new ArrayAdapter(view.getContext(), R.layout.device_name);

        // Find and set up the ListView for paired devices
        pairedListView = (ListView) view.findViewById(R.id.paired_devices);
        pairedListView.setAdapter(nombresDispositivos);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices and append to 'pairedDevices'
        final Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Add previosuly paired devices to the array
        if (pairedDevices.size() > 0) {
            view.findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);//make title viewable

            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName()+ "\n" + device.getAddress());
                nombresDispositivos.add(device.getName());
            }
            if(MainActivity.dispositivos!=null && MainActivity.dispositivos.size()!=0){
                for(int i =0; i<MainActivity.dispositivos.size();i++){
                    mPairedDevicesArrayAdapter.add(MainActivity.dispositivos.get(i));
                    nombresDispositivos.add(MainActivity.nombresDispositivos.get(i));
                }
            }
        } else {
            String noDevices = "Ningún dispositivo pudo ser emparejado";
            mPairedDevicesArrayAdapter.add(noDevices);
            nombresDispositivos.add(noDevices);
        }

        return view;

    }

    private void checkBTState() {
        // Check device has Bluetooth and that it is turned on
        mBtAdapter=BluetoothAdapter.getDefaultAdapter(); // CHECK THIS OUT THAT IT WORKS!!!
        if(mBtAdapter==null) {
            Toast.makeText(getContext(), "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (mBtAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth Activado...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);

            }
        }
    }
}


