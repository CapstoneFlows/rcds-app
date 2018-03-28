package capstoneflows.rcdstool;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Spinner;

import java.util.List;

public class RCDSTool extends AppCompatActivity {
    private final static String TAG = RCDSTool.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mUUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

    private String mDeviceAddress;
    private TextView mID;
    private TextView mLoc;
    private TextView mDir;
    private TextView mComment;
    private TextView mState;
    private TextView mTerminal;
    private Spinner mCmdSelect;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mGattCharacteristic;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Log.e(TAG, "Connected");
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Log.e(TAG, "Disconnected");
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                boolean ok = checkGattServices(mBluetoothLeService.getSupportedGattServices());
                if (ok == false) {
                    mConnected = false;
                    Log.e(TAG, "Not an HM-10 device");
                    invalidateOptionsMenu();
                    clearUI();
                } else {
                    int unixTime = (int) (System.currentTimeMillis() / 1000L);
                    mGattCharacteristic.setValue("T"+Integer.toString(unixTime));
                    mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                    mGattCharacteristic.setValue("T"+Integer.toString(unixTime));
                    mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                    mGattCharacteristic.setValue("?");
                    mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                    mGattCharacteristic.setValue("?");
                    mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                handleResponse(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    public void onRunBtn()
    {
        String cmd = mCmdSelect.getSelectedItem().toString();
        switch(cmd) {
            case "Set Parameters":
                mGattCharacteristic.setValue("SET_VARS");
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                mGattCharacteristic.setValue("ID="+mID.toString());
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                mGattCharacteristic.setValue("LOC="+mLoc.toString());
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                mGattCharacteristic.setValue("DIR="+mDir.toString());
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                mGattCharacteristic.setValue("COMMENT="+mComment.toString());
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                mGattCharacteristic.setValue("?");
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                return;
            case "Start Running":
                mGattCharacteristic.setValue("START_RUNNING");
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                mGattCharacteristic.setValue("?");
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                return;
            case "Stop Running":
                mGattCharacteristic.setValue("STOP_RUNNING");
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                mGattCharacteristic.setValue("?");
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                return;
            case "Reset Device":
                mGattCharacteristic.setValue("RESET_DEVICE");
                mBluetoothLeService.writeCharacteristic(mGattCharacteristic);
                return;

        }
    }

    private void clearUI() {
        mID.setText("");
        mLoc.setText("");
        mDir.setText("");
        mComment.setText("");
        mState.setText("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcdstool);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        mID = (TextView) findViewById(R.id.id_box);
        mLoc = (TextView) findViewById(R.id.loc_box);
        mDir = (TextView) findViewById(R.id.dir_box);
        mComment = (TextView) findViewById(R.id.comment_box);
        mState = (TextView) findViewById(R.id.state_box);
        mTerminal = (TextView) findViewById(R.id.terminal);
        mCmdSelect = (Spinner) findViewById(R.id.cmd_select);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleResponse(String data) {
        mTerminal.append(data+'\n');
        if (data != null) {
            return;
        }
        if (data.contains(" ID=")) {
            parseInfo(data);
            return;
        } else if (data.contains("RESET_COMPLETE")) {
            return;
        }
    }

    private void parseInfo(String info) {
        mID.setText(info.split(" ")[0]);
        mLoc.setText(info.split(" ID=")[1].split(" LOC=")[0]);
        mDir.setText(info.split(" LOC=")[1].split(" DIR=")[0]);
        mComment.setText(info.split(" DIR=")[1].split(" COMMENT=")[0]);
        mState.setText(info.split(" COMMENT=")[1].split("\r")[0]);
    }

    // Check that the UUID of HM-10 is available.
    private boolean checkGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return false;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                String uuid = gattCharacteristic.getUuid().toString();
                if (uuid == mUUID) {
                    mGattCharacteristic = gattCharacteristic;
                    mNotifyCharacteristic = gattCharacteristic;
                    mBluetoothLeService.setCharacteristicNotification(
                            gattCharacteristic, true);
                    return true;
                }
            }
        }
        return false;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
