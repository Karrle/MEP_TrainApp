package maerklin.mep_messwagen_final;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ArrayAdapter;

import java.util.Set;

/**
 * Created by Karrle on 03.11.2016.
 */

public class BtWorker extends IntentService {
//------------------------------------------------------------------------------------------------//
    /* General declarations */
    // Variables in Class

    BluetoothAdapter mBluetoothAdapter;
    ArrayAdapter mArrayAdapter;

    // Constants

    // Constructor
    public BtWorker() {
        super("HelloIntentService");
    }

//------------------------------------------------------------------------------------------------//
    /* onSomething methods */

    // All work is done here
    @Override
    protected void onHandleIntent(Intent intent) {
        getPairedDevices();
    }

    // onCreate() is a one-time-call once the Service is started. Here, we get the default BluetoothAdapter for later use.
    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }
    // Binds service to a component (e.g. Activity), which defines the Service's lifecycle
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//------------------------------------------------------------------------------------------------//
    /* Other methods */
    private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

}
