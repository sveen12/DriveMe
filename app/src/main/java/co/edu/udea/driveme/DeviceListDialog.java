package co.edu.udea.driveme;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DeviceListDialog extends DialogFragment {
        private static final String TAG = "DeviceListDialog.class";

    private CheckBox cbVerMac;
    public ListView lvApareados;
    private ProgressBar progressBar;
    private TextView tvConectando;

    private BluetoothAdapter btAdapter;
    public static ArrayAdapter dispositivosApareados;
    public static ArrayAdapter nombresDispositivos;
    public RecibirDatos recibirDatos;

    public interface RecibirDatos{
        public void deviceMac(String address);
    }

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
                view.setBackgroundColor(Color.parseColor("#CC666666"));
                progressBar.setVisibility(View.VISIBLE);

                String info = dispositivosApareados.getItem(position).toString();
                String address = info.substring(info.length() - 17);

                recibirDatos.deviceMac(address);
                dismiss();
        }
    };

    public static void addBTDevice(String device){
        dispositivosApareados.add(device);
        nombresDispositivos.add(device);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_device_list, container);
        view.setPadding(20,20,20,20);

        checkBTState();

        tvConectando = (TextView) view.findViewById(R.id.tvConectando);
        tvConectando.setTextSize(40);
        tvConectando.setText(" ");
        cbVerMac = (CheckBox) view.findViewById(R.id.cbMac);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBarDialog);

        cbVerMac.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(isChecked){
                    lvApareados.setAdapter(dispositivosApareados);
                }else{
                    lvApareados.setAdapter(nombresDispositivos);
                }
            }
        });

        // Initialize array adapter for paired devices
        dispositivosApareados = new ArrayAdapter(view.getContext(), R.layout.device_name);
        nombresDispositivos = new ArrayAdapter(view.getContext(), R.layout.device_name);

        // Find and set up the ListView for paired devices
        lvApareados = (ListView) view.findViewById(R.id.paired_devices);
        lvApareados.setAdapter(nombresDispositivos);
        lvApareados.setOnItemClickListener(mDeviceClickListener);

        // Get the local Bluetooth adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices and append to 'pairedDevices'
        final Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        // Add previosuly paired devices to the array
        if (pairedDevices.size() > 0) {
            view.findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);//make title viewable

            for (BluetoothDevice device : pairedDevices) {
                dispositivosApareados.add(device.getName()+ "\n" + device.getAddress());
                nombresDispositivos.add(device.getName());
            }
            if(MainActivity.dispositivos!=null && MainActivity.dispositivos.size()!=0){
                for(int i =0; i<MainActivity.dispositivos.size();i++){
                    dispositivosApareados.add(MainActivity.dispositivos.get(i));
                    nombresDispositivos.add(MainActivity.nombresDispositivos.get(i));
                }
            }
        } else {
            String noDevices = "Ningún dispositivo pudo ser emparejado";
            dispositivosApareados.add(noDevices);
            nombresDispositivos.add(noDevices);
        }
        return view;
    }

    private void checkBTState() {
        // Check device has Bluetooth and that it is turned on
        btAdapter = BluetoothAdapter.getDefaultAdapter(); // CHECK THIS OUT THAT IT WORKS!!!
        if(btAdapter ==null) {
            Toast.makeText(getContext(), "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth Activado...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);

            }
        }
    }
}


