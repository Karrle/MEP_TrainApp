package maerklin.mep_messwagen_final;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by Karrle on 03.11.2016.
 */

public class BtWorker extends IntentService {
//------------------------------------------------------------------------------------------------//
    /* General declarations */
    // Variables in Class

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mBtDevice;
    ArrayAdapter mArrayAdapter;
    ArrayList<String> pairedDeviceStrings = new ArrayList<String>();

    // Constants
    final UUID MY_UUID = UUID.fromString("0829f6b6-a1ec-11e6-80f5-76304dec7eb7");
    final int MESSAGE_READ = 1;
    final int DATA_RECEIVED = 11;
    final int SOCKET_CONNECTED = 10;
    // Constructor
    public BtWorker() {
        super("HelloIntentService");
    }

//------------------------------------------------------------------------------------------------//
    /* onSomething methods */

    // All work is done here
    @Override
    protected void onHandleIntent(Intent workIntent) {
        selectDevice();
        becomeDiscoverable();

        AcceptThread acceptBtConnection = new AcceptThread();
        ConnectThread connectToBtServer = new ConnectThread(mBtDevice);
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
        unregisterReceiver(mReceiver);
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
    private void selectDevice() {
        getPairedDevices();
        discoverDevices();
    }


// Look for already registered devices
    private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                pairedDeviceStrings.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

// Discover all surrounding devices
    private void discoverDevices(){
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    private void becomeDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
        startActivity(discoverableIntent);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };

// Connect as server
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("MEP_Messwagen", MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    try {
                        // Do work to manage the connection (in a separate thread)
                        manageConnectedSocket(socket);
                        mmServerSocket.close();
                        break;
                    } catch (IOException e) {}
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

// Connect as client
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

// Manage the connection and receive data

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, Handler handler) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mHandler = handler;
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                   // mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            //.sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        ConnectedThread connectionEstablished = new ConnectedThread(socket, mHandler);

    }

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DATA_RECEIVED: {
                    String data = (String) msg.obj;
                }
            }
        }
        @Override
        public void publish(LogRecord record) {

        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {

        }
    };




}
