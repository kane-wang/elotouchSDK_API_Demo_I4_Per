package com.elotouch.sdk_api.demo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.elotouch.library.EloPeripheralManager;
import com.elotouch.library.ZBCRManager;
import com.elotouch.library.ZBCREventListener;

public class BCRDemoActivity extends Activity  {

    private static final String TAG = "BCRDemoActivity";
    
    private static final int MSG_SHOW_TEXT = 0;
    private static final int MSG_SHOW_STATUS = 1;
    private static final int MSG_SHOW_EVENT = 2;
    private static final int MSG_SHOW_IMAGE = 3;

    private static final int STATE_IDLE = 0;
    private static final int STATE_DECODE_MANUAL = 1;
    private static final int STATE_DECODE_HANDSFREE= 2;

    private TextView mTvOutput, mTvTips;
    private ImageView mBarcodeImg;
    private EditText mEditNum, mEditVal;
    private ZBCRManager mZBCRMgr;
    private int mState = STATE_IDLE;
    private EloPeripheralManager mEloManager;

    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            switch(msg.what)
            {
                case  MSG_SHOW_TEXT:
                    String result = (String)msg.obj;
                    Log.i(TAG, "MSG_SHOW_TEXT -" + result);
                    mTvOutput.setText(result);
                    mTvTips.setText(R.string.scan_again);
                  //  mMessage.setText("type: " + msg.arg1 + ", length: " + msg.arg2);
                    mHandler.sendEmptyMessage(MSG_SHOW_IMAGE);
                    break;
                
                case MSG_SHOW_STATUS:
                    int status = msg.arg1;
                    Log.i(TAG, "MSG_SHOW_STATUS - " +status);
                    switch (status)
                    {
                        case ZBCRManager.DECODE_STATUS_TIMEOUT:
                  //      mMessage.setText("decode timed out");
                        break;

                        case ZBCRManager.DECODE_STATUS_CANCELED:
                   //     mMessage.setText("decode cancelled");
                        break;

                        case ZBCRManager.DECODE_STATUS_ERROR:
                   //     mMessage.setText("decode failed");
                        default:
                        break;
                    }
                    break;
                case MSG_SHOW_EVENT:
                    int event = msg.arg1;
                    Log.i(TAG, "MSG_SHOW_EVENT - " + event);                                
                    switch (event)
                    {
                        case ZBCRManager.BCRDR_EVENT_SCAN_MODE_CHANGED:
                //            mMessage.setText("Scan mode changed");
                            break;

                        case ZBCRManager.BCRDR_EVENT_MOTION_DETECTED:
                 //           mMessage.setText("Motion detected");
                            break;

                        case ZBCRManager.BCRDR_EVENT_SCANNER_RESET:
                 //           mMessage.setText("Scanner reset");
                            break;

                        default:
                        // process any other events here
                            break;
                    }
                    break;
                case MSG_SHOW_IMAGE:
                    Log.i(TAG, "MSG_SHOW_IMAGE ");    
                    byte[] data = mZBCRMgr.getLastDecImage();
                    if(data != null) {
                        Log.i(TAG, "image size = " + data.length);
                        Bitmap bmSnap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        mBarcodeImg.setImageBitmap(bmSnap);
                    }
                    break;
                default:
                    break;
            }
        }
    };
    
    @Override 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bcr);

        if (ActivityManager.isUserAMonkey()) {
            finish();
        }

        mBarcodeImg = (ImageView) findViewById(R.id.decode_image);
        mTvOutput = (TextView) findViewById(R.id.code_output);
        mTvTips = (TextView) findViewById(R.id.scan_tip);
        mEditNum = (EditText) findViewById(R.id.edit_num);
        mEditVal = (EditText) findViewById(R.id.edit_val);
        mZBCRMgr = new ZBCRManager(this);
        mEloManager = new EloPeripheralManager(this, null);
    } 

    private void enableBCR() {
        try {
            if(!mZBCRMgr.open()) {
                Log.e(TAG,  "open BCR failed!");
            }
            
        }
        catch (Exception e)
        {
            Log.e(TAG, "open excp:" + e);
        }

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Log.i(TAG,  " onResume");

        enableBCR();
        mZBCRMgr.registerListener(new BCREventListener());
        mZBCRMgr.setInputMode(ZBCRManager.INPUT_MODE_HID);
    }

    @Override
    public void onPause() {
        Log.d(TAG,  " onPause");
        super.onPause();

        try {
            mZBCRMgr.release();
            mState = STATE_IDLE;
        }
        catch (Exception e) {
            Log.e(TAG, "release BCR exception:" + e);
        }
        mZBCRMgr.unregisterListener();
        mZBCRMgr.setInputMode(ZBCRManager.INPUT_MODE_KEYBOARD);
        
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_L1:
                Log.d(TAG, "left BCR trigger button down.");
                String leftTriggerEnabled = mEloManager.getSysProperty("persist.sys.triggerbutton.left.enabled");
                //if(SystemProperties.getBoolean("persist.sys.triggerbutton.left.enabled", true)){
                if (leftTriggerEnabled == null || leftTriggerEnabled.equals("")
                        || leftTriggerEnabled.equals("true") || leftTriggerEnabled.equals("1")) {
                    triggerButtonClicked();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_BUTTON_R1:
                Log.d(TAG, "right BCR trigger button down.");
                String rightTriggerEnabled = mEloManager.getSysProperty("persist.sys.triggerbutton.right.enabled");
                //if(SystemProperties.getBoolean("persist.sys.triggerbutton.right.enabled", true)){
                if (rightTriggerEnabled == null || rightTriggerEnabled.equals("")
                        || rightTriggerEnabled.equals("true") || rightTriggerEnabled.equals("1")) {
                    triggerButtonClicked();
                    return true;
                }
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void triggerButtonClicked() {
        Log.d(TAG, " triggerButtonClicked, state - " + mState);
        if(mState == STATE_IDLE) {
            mZBCRMgr.startDecode(ZBCRManager.TRIGGER_MODE_MANUAL);
            mState = STATE_DECODE_MANUAL;
        }
        else if (mState == STATE_DECODE_MANUAL || mState == STATE_DECODE_HANDSFREE) {
            mZBCRMgr.stopDecode();
            mState = STATE_IDLE;
        }
        
    }

    public void btnGet_click(View view) {
        Log.i(TAG, "btnGet_click()");
        
        String sn = mEditNum.getText().toString();
        try {
            int num = Integer.parseInt(sn);
            int val = mZBCRMgr.getParameter(num);
            if(val == ZBCRManager.BCR_ERROR){
                Toast.makeText(this, "get parameter " + sn + " failed!", Toast.LENGTH_SHORT).show();
            }
            mEditVal.setText(Integer.toString(val));
        }
        catch (NumberFormatException nx) {
            Toast.makeText(this, "Parameter number should be number format!", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnSet_click(View view) {
        Log.i(TAG, "btnSet_click()");

        String sn = mEditNum.getText().toString();
        String sv = mEditVal.getText().toString();
        try {
            int num = Integer.parseInt(sn);
            int val = Integer.parseInt(sv);
            int ret = mZBCRMgr.setParameter(num, val);
            if(ret == ZBCRManager.BCR_ERROR){
                Toast.makeText(this, "Set parameter " + sn + " failed!", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Set parameter " + sn + " to " + sv + " done!", Toast.LENGTH_SHORT).show();
            }
        }
        catch (NumberFormatException nx) {
            Toast.makeText(this, "Parameter number and value should be number format!", Toast.LENGTH_SHORT).show();
        }
    }
    
    private class BCREventListener implements ZBCREventListener {
        
        public void onDecodeComplete(int symbology, int length, byte[] data){
            Log.i(TAG, "onDecodeComplete(), symbology=" + symbology + ", length=" + length);

            if(mState == STATE_DECODE_MANUAL) {
                mState = STATE_IDLE;
            }
            
            if (length > 0)
            {
                String string = new String(data);
                Log.i(TAG, "decoded string = " + string + ", length = "+string.length());
                String result = string.substring(0, length);
            //    mResult.setText(result);
                Message msg = mHandler.obtainMessage(MSG_SHOW_TEXT, symbology, length, string);
                mHandler.sendMessage(msg);
            }
            else// no-decode
            {
                Message msg = mHandler.obtainMessage(MSG_SHOW_STATUS, length, 0);
                mHandler.sendMessage(msg);
            }
        }

        public void onEvent(int event, int info, byte[] data) {
            Log.i(TAG, "onEvent() -- " + event);
             Message msg = mHandler.obtainMessage(MSG_SHOW_EVENT, event, 0);
             mHandler.sendMessage(msg);
        }

        public void onError(int error) {
            Log.e(TAG, "onError(), error = " + error);
        }

    }
    
}
