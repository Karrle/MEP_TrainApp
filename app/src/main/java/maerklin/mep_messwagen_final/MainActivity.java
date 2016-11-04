package maerklin.mep_messwagen_final;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    MepBluetoothClass bluetoothModule;
    final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Check if Bluetooth is available
        if (mBluetoothAdapter == null) {
            AlertDialog.Builder alertBuilder1 = new AlertDialog.Builder(this.getApplicationContext());
            alertBuilder1.setMessage("Device does not support Bluetooth");
            alertBuilder1.setTitle("Bluetooth Error");
            alertBuilder1.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            AlertDialog alert1 = alertBuilder1.create();
            alert1.show();
        }
        // Check if Bluetooth is activated. If it's not, start it. If it is, search for devices.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {

        }
        Intent startBt = new Intent(this, MepBluetoothClass.class);
        startActivity(startBt);
    }

    // Returned upon call startActivityForResult(...)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {// User would not start Bluetooth. App is closed
            } else if (resultCode == RESULT_OK) {// User started Bluetooth. Continue with finding devices

            }
        }
    }


    /*public void getBtData(View view) {
        int receivedData;
        String displayedData;

        receivedData = bluetoothModule.GetBtData();
        displayedData = Integer.toString(receivedData);

        TextView bluetoothTextView = new TextView(this);
        bluetoothTextView.setTextSize(32);
        bluetoothTextView.setText(displayedData);

        ViewGroup layout = (ViewGroup) findViewById(R.id.activity_main);
        layout.addView(bluetoothTextView);
    }*/
}
