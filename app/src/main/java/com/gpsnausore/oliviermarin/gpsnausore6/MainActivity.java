package com.gpsnausore.oliviermarin.gpsnausore6;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import android.os.Message;

public class MainActivity extends AppCompatActivity implements DeviceList.OnFragmentInteractionListener, NavigationMap.OnFragmentInteractionListener, Settings.OnFragmentInteractionListener {

    private Fragment fragment;

    private static final String TAG = "MY_APP_DEBUG_TAG";
    private Handler handler = new Handler(); // handler that gets info from Bluetooth service

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    switchFragment(0, "TAG_NavigationFrag");
                    return true;
                case R.id.navigation_dashboard:
                    //mTextMessage.setText(R.string.title_dashboard);
                    switchFragment(1, "TAG_DeviceFrag");
                    return true;
                case R.id.navigation_notifications:
                    switchFragment(2, "TAG_SettingsFrag");
                    return true;
            }
            return false;
        }
    };

    private Fragment switchFragment(int pos, String tag) {
        fragment = new DeviceList();
        switch (pos) {
            case 0:
                fragment = new NavigationMap();
                break;
            case 1:
                fragment = new DeviceList();
                break;
            case 2:
                fragment = new Settings();
                break;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_fragmentholder, fragment, tag)
                .commit();

        return fragment;
    }

    public void startConnection(final String deviceName, String deviceMac, BluetoothAdapter myBluetooth){
        // Get the device MAC address, which is the last 17 chars in the View
        final String address = deviceMac;
        final String name = deviceName;
        final BluetoothAdapter mBluetooth = myBluetooth;



        BluetoothSocket mBTSocket = null;
        BluetoothDevice device = mBluetooth.getRemoteDevice(address);
        try {
            mBTSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
        }

        ConnectedThread mainThread = new ConnectedThread(mBTSocket);

        mainThread.write("Coucou".getBytes());

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"));
        //creates secure outgoing connection with BT device using UUID
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        switchFragment(0, "TAG_NavigationFrag");
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = handler.obtainMessage(
                            MainActivity.MessageConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = handler.obtainMessage(
                        MainActivity.MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        handler.obtainMessage(MainActivity.MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}
