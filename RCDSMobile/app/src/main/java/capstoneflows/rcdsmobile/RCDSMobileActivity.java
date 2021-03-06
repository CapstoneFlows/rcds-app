package capstoneflows.rcdsmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class RCDSMobileActivity extends AppCompatActivity{

    private final String TAG = "RCDSMobileActivity";

    private final static int WSTATE_CONNECT=0;
    private final static int WSTATE_SEARCH_SERVICES=1;
    private final static int WSTATE_NOTIFY_KEY=2;
    private final static int WSTATE_READ_KEY=3;
    private final static int WSTATE_DUMMY=4;
    private final static int WSTATE_WRITE_KEY=5;

    private TextView mID;
    private TextView mLoc;
    private TextView mDir;
    private TextView mComment;
    private TextView mState;
    private TextView mTerminal;
    private Spinner mCmdSelect;

    private Context mContext;
    private HM10BroadcastReceiver mBroadcastReceiver;
    private String mDeviceAddress;
    private int mStateM = 0;
    private String mCmd;
    private boolean mConnected = false;
    private int lastTime = 0;
    private StringBuilder msgBuilder;

    private TumakuBLE  mTumakuBLE=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate()");
        msgBuilder = new StringBuilder();
        setContentView(R.layout.activity_rcdsmobile);
        mContext=this;
        mBroadcastReceiver= new HM10BroadcastReceiver();
        mDeviceAddress=getIntent().getStringExtra(TumakuBLE.EXTRA_ADDRESS);
        if (mDeviceAddress==null) {
            finish();
        }
        mTumakuBLE=((TumakuBLEApplication)getApplication()).getTumakuBLEInstance(this);
        mTumakuBLE.setDeviceAddress(mDeviceAddress);
        mID = (TextView) findViewById(R.id.id_box);
        mLoc = (TextView) findViewById(R.id.loc_box);
        mDir = (TextView) findViewById(R.id.dir_box);
        mComment = (TextView) findViewById(R.id.comment_box);
        mState = (TextView) findViewById(R.id.state_box);
        mState.setEnabled(false);
        mTerminal = (TextView) findViewById(R.id.terminal);
        mTerminal.setMovementMethod(new ScrollingMovementMethod());
        mCmdSelect = (Spinner) findViewById(R.id.cmd_select);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reconnect_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_reconnect:
                mStateM=WSTATE_CONNECT;
                mTumakuBLE.resetTumakuBLE();
                mTumakuBLE.setDeviceAddress(mDeviceAddress);
                updateInfoText("Reset connection to device");
                clearUI();
                nextState();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        super.onResume();
        IntentFilter filter = new IntentFilter(TumakuBLE.WRITE_SUCCESS);
        filter.addAction(TumakuBLE.READ_SUCCESS);
        filter.addAction(TumakuBLE.DEVICE_CONNECTED);
        filter.addAction(TumakuBLE.DEVICE_DISCONNECTED);
        filter.addAction(TumakuBLE.SERVICES_DISCOVERED);
        filter.addAction(TumakuBLE.NOTIFICATION);
        filter.addAction(TumakuBLE.WRITE_DESCRIPTOR_SUCCESS);
        this.registerReceiver(mBroadcastReceiver, filter);
        if (mTumakuBLE.isConnected()){
            mStateM=WSTATE_NOTIFY_KEY;
            nextState();
            updateInfoText("Resume connection to device");
        } else {
            mStateM=WSTATE_CONNECT;
            nextState();
            updateInfoText("Start connection to device");
        }

    }

    @Override
    public void onStop(){
        super.onStop();
        this.unregisterReceiver(this.mBroadcastReceiver);
    }

    private void clearUI() {
        mID.setText("");
        mLoc.setText("");
        mDir.setText("");
        mComment.setText("");
        mState.setText("");
    }

    protected void nextState(){
        switch(mStateM) {
            case (WSTATE_DUMMY):
                if(!mConnected) {
                    int unixTime = (int) (System.currentTimeMillis() / 1000L);
                    if (unixTime > (lastTime+5)) {
                        lastTime = unixTime;
                        mCmd = "T" + Integer.toString(lastTime);
                        mStateM = WSTATE_WRITE_KEY;
                        nextState();
                    }
                }
                break;

            case (WSTATE_CONNECT):
                clearUI();
                mTumakuBLE.connect();
                break;

            case(WSTATE_SEARCH_SERVICES):
                mTumakuBLE.discoverServices();
                break;

            case(WSTATE_READ_KEY):
                mTumakuBLE.read(TumakuBLE.SENSORTAG_KEY_SERVICE,TumakuBLE.SENSORTAG_KEY_DATA);


            case(WSTATE_NOTIFY_KEY):
                mTumakuBLE.enableNotifications(TumakuBLE.SENSORTAG_KEY_SERVICE,TumakuBLE.SENSORTAG_KEY_DATA,true);
                break;

            case(WSTATE_WRITE_KEY):
                String tmpString = mCmd;
                switch(tmpString) {
                    case "Set Parameters":
                        tmpString = "SET_VARS";
                        break;
                    case "ID":
                        tmpString = "ID="+mID.getText().toString();
                        break;
                    case "LOC":
                        tmpString = "LOC="+mLoc.getText().toString();
                        break;
                    case "DIR":
                        tmpString = "DIR="+mDir.getText().toString();
                        break;
                    case "COMMENT":
                        tmpString = "COMMENT="+mComment.getText().toString();
                        break;
                    case "Start Running":
                        tmpString = "START_RUNNING";
                        break;
                    case "Stop Running":
                        tmpString = "STOP_RUNNING";
                        break;
                    case "Reset Device":
                        tmpString = "RESET_DEVICE";
                        break;
                    case "?":
                        tmpString = "?";
                        break;
                    default:
                }
                updateInfoText("Command sent: "+tmpString);
                byte tmpArray []= new byte[tmpString.length()];
                for (int i=0; i<tmpString.length();i++) tmpArray[i]=(byte)tmpString.charAt(i);
                mTumakuBLE.write(TumakuBLE.SENSORTAG_KEY_SERVICE, TumakuBLE.SENSORTAG_KEY_DATA, tmpArray);
                break;

            default:
        }
    }

    public void onRunBtn(View v) {
        if ((mStateM==WSTATE_DUMMY)) {
            mCmd = mCmdSelect.getSelectedItem().toString();
            mStateM=WSTATE_WRITE_KEY;
            nextState();
        } else
            Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
    }

    private void handleResponse(String data) {
        Log.d(TAG,"handleResponse:\""+data+"\"");
        if (data == null) {
            return;
        }
        else if (data.contains("TIME_ACK") || data.contains("TIME_SYNCED")) {
            if ((mStateM==WSTATE_DUMMY)) {
                mConnected = true;
                mCmd = "?";
                mStateM = WSTATE_WRITE_KEY;
                nextState();
            } else
                Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
            return;
        } else if (data.contains("SET_ACK")) {
            if ((mStateM==WSTATE_DUMMY)) {
                mCmd = "ID";
                mStateM = WSTATE_WRITE_KEY;
                nextState();
            } else
                Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
            return;
        } else if (data.contains("ID_ACK")) {
            if ((mStateM==WSTATE_DUMMY)) {
                mCmd = "LOC";
                mStateM = WSTATE_WRITE_KEY;
                nextState();
            } else
                Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
            return;
        } else if (data.contains("LOC_ACK")) {
            if ((mStateM==WSTATE_DUMMY)) {
                mCmd = "DIR";
                mStateM = WSTATE_WRITE_KEY;
                nextState();
            } else
                Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
            return;
        } else if (data.contains("DIR_ACK")) {
            if ((mStateM==WSTATE_DUMMY)) {
                mCmd = "COMMENT";
                mStateM = WSTATE_WRITE_KEY;
                nextState();
            } else
                Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
            return;
        } else if (data.contains("COMMENT_ACK")) {
            if ((mStateM==WSTATE_DUMMY)) {
                mCmd = "?";
                mStateM = WSTATE_WRITE_KEY;
                nextState();
            } else
                Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
            return;
        } else if (data.contains("START_ACK")) {
            if ((mStateM==WSTATE_DUMMY)) {
                mCmd = "?";
                mStateM = WSTATE_WRITE_KEY;
                nextState();
            } else
                Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
            return;
        } else if (data.contains("STOP_ACK")) {
            if ((mStateM==WSTATE_DUMMY)) {
                mCmd = "?";
                mStateM = WSTATE_WRITE_KEY;
                nextState();
            } else
                Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
            return;
        } else if (data.contains("RESET_COMPLETE")) {
            if ((mStateM==WSTATE_DUMMY)) {
                mCmd = "?";
                mStateM = WSTATE_WRITE_KEY;
                nextState();
            } else
                Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
            return;
        }
        else if ((data.endsWith(".") || data.endsWith("\r\n")) && !data.endsWith("@#\r\n")) {}
        else {
            msgBuilder.append(data);
            Log.d(TAG, "current msg is:" + msgBuilder.toString());
        }
        if (msgBuilder.toString().endsWith("@#\r\n")) {
            msgBuilder.append(data);
            String msg = msgBuilder.toString();
            Log.d(TAG, "full msg is :" + msg);
            parseInfo(msg.split("@#\r\n")[0]);
            msgBuilder = new StringBuilder();
        }
    }

    private void parseInfo(String info) {
        Log.d(TAG,"parseInfo:"+info);
        String[] temp = info.split(" COMMENT=");
        if (temp.length > 1)
            mComment.setText(temp[1]);
        temp = temp[0].split(" DIR=");
        if (temp.length > 1)
            mDir.setText(temp[1]);
        temp = temp[0].split(" LOC=");
        if (temp.length > 1)
            mLoc.setText(temp[1]);
        temp = temp[0].split(" ID=");
        if (temp.length > 1)
            mID.setText(temp[1]);
        mState.setText(temp[0]);
    }

    protected void updateInfoText(String text) {
        mTerminal.append("UPDATE:"+text+"\n");
    }
    protected void updateNotificationText(String text) {
        mTerminal.append("NOTIFICATION:"+text+"\n");
    }
    protected void displayText(String text) { mTerminal.append("DISPLAY:"+text+"\n"); }

    private class HM10BroadcastReceiver extends BroadcastReceiver {
        //YeelightCallBack.WRITE_SUCCESS);
        //YeelightCallBack.READ_SUCCESS);
        //YeelightCallBack.DEVICE_CONNECTED);

        public String bytesToString(byte[] bytes){
            StringBuilder stringBuilder = new StringBuilder(
                    bytes.length);
            for (byte byteChar : bytes)
                stringBuilder.append(String.format("%02X ", byteChar));
            return stringBuilder.toString();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TumakuBLE.DEVICE_CONNECTED)) {

                updateInfoText("Received connection event");
                mStateM=WSTATE_SEARCH_SERVICES;
                nextState();
                return;
            }
            if (intent.getAction().equals(TumakuBLE.DEVICE_DISCONNECTED)) {
                //This is an unexpected device disconnect situation generated by Android BLE stack
                //Usually happens on the service discovery step :-(
                //Try to reconnect
                mConnected = false;
                String fullReset=intent.getStringExtra(TumakuBLE.EXTRA_FULL_RESET);
                if (fullReset!=null){
                    Toast.makeText(mContext, "Unrecoverable BT error received. Launching full reset", Toast.LENGTH_SHORT).show();
                    mStateM=WSTATE_CONNECT;
                    mTumakuBLE.resetTumakuBLE();
                    mTumakuBLE.setDeviceAddress(mDeviceAddress);
                    mTumakuBLE.setup();
                    nextState();
                    return;
                } else {
                    if (mStateM!=WSTATE_CONNECT){
                        Toast.makeText(mContext, "Device disconnected unexpectedly. Reconnecting.", Toast.LENGTH_SHORT).show();
                        mStateM=WSTATE_CONNECT;
                        mTumakuBLE.resetTumakuBLE();
                        mTumakuBLE.setDeviceAddress(mDeviceAddress);
                        nextState();
                        return;
                    }
                }
            }
            if (intent.getAction().equals(TumakuBLE.SERVICES_DISCOVERED)) {

                updateInfoText("Received services discovered event");
                mStateM=WSTATE_NOTIFY_KEY;
                nextState();
                return;
            }

            if (intent.getAction().equals(TumakuBLE.READ_SUCCESS)) {
                String readValue= intent.getStringExtra(TumakuBLE.EXTRA_VALUE);
                byte [] readByteArrayValue= intent.getByteArrayExtra(TumakuBLE.EXTRA_VALUE_BYTE_ARRAY);

                if (readValue==null) updateInfoText("Received Read Success Event but no value in Intent"  );
                else {
                    updateInfoText("Received Read Success Event: " + readValue);
                }
                if (readValue==null) readValue="null";

                if (mStateM==WSTATE_READ_KEY) {
                    if (readByteArrayValue!=null) displayText(readValue);
                    mStateM=WSTATE_DUMMY;
                    nextState();
                    return;
                }
                return;
            }

            if (intent.getAction().equals(TumakuBLE.WRITE_SUCCESS)) {
                updateInfoText("Received Write Success Event");
                if (mStateM==WSTATE_WRITE_KEY) {
                    mStateM=WSTATE_DUMMY;
                    nextState();
                    return;
                }
                return;
            }

            if (intent.getAction().equals(TumakuBLE.NOTIFICATION)) {
                String notificationValue= intent.getStringExtra(TumakuBLE.EXTRA_VALUE);
                String characteristicUUID= intent.getStringExtra(TumakuBLE.EXTRA_CHARACTERISTIC);
                byte [] notificationValueByteArray =  intent.getByteArrayExtra(TumakuBLE.EXTRA_VALUE_BYTE_ARRAY);
                if (notificationValue==null) notificationValue="NULL";
                if (characteristicUUID==null) characteristicUUID="MISSING";
                updateNotificationText("Received Notification Event: Value: " + notificationValue +
                        " -  Characteristic UUID: " + characteristicUUID);
                if (!notificationValue.equalsIgnoreCase("null")) {
                    if (characteristicUUID.equalsIgnoreCase(TumakuBLE.SENSORTAG_KEY_DATA)) {
                        if (notificationValueByteArray==null) {
                            return;
                        }
                        String tmpString="";
                        for (int i=0; i<notificationValueByteArray.length; i++) tmpString+=(char)notificationValueByteArray[i];
                        handleResponse(tmpString);
                        displayText(tmpString);
                    }
                }
                return;
            }

            if (intent.getAction().equals(TumakuBLE.WRITE_DESCRIPTOR_SUCCESS)) {
                updateInfoText("Received Write Descriptor Success Event");
                if (mStateM==WSTATE_NOTIFY_KEY) {
                    mStateM=WSTATE_READ_KEY;
                    nextState();
                }
                return;
            }


        }

    }
}
