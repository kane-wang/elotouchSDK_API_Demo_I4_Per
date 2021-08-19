package com.elotouch.sdk_api.demo;

import android.Manifest;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceActivity;
import androidx.preference.PreferenceFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.preference.SeekBarPreference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.elotouch.library.EloBuild;
import com.elotouch.library.EloPeripheralManager;
import com.elotouch.library.EloPeripheralManager.PoE;
import com.elotouch.library.EloPeripheralManager.UsbPort;
import com.elotouch.library.poe.PoeCustomBundle;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public final class PoeSettingsActivity extends PreferenceActivity {
    private static final String TAG = "SDK_API_Poe_demo";

    /** The xml file that is store the poe custom data which is used to restore POE settings*/
    private static final String SDCARD_POE_CUST_PATH  = "/sdcard/Download/poe-cust.xml";

    // String keys for preference lookup
    private static final String KEY_USB_PORT1 = "key_poe_usb_port1";
    private static final String KEY_USB_PORT2 = "key_poe_usb_port2";
    private static final String KEY_USB_PORT3 = "key_poe_usb_port3";
    private static final String KEY_USB_PORT4 = "key_poe_usb_port4";
    private static final String KEY_USB_PORT5 = "key_poe_usb_port5";
    private static final String KEY_USB_PORT6 = "key_poe_usb_port6";
    private static final String KEY_BT = "key_poe_bt";
    private static final String KEY_WIFI = "key_poe_wifi";
    private static final String KEY_GPIO_OUTPUT = "key_poe_gpio_output";
    private static final String KEY_HDMI_OUTPUT = "key_poe_hdmi_output";
    private static final String KEY_BRIGHTNESS = "key_poe_brightness";
    private static final String KEY_VOLUME = "key_poe_volume"; 
    
    private static final String KEY_POE_POWER_STATUS = "key_poe_power_status";

    private static final String[] USB_PORT_KEY_LIST = {
            KEY_USB_PORT1, KEY_USB_PORT2,
            KEY_USB_PORT3, KEY_USB_PORT4,
            KEY_USB_PORT5, KEY_USB_PORT6
    };

    // format is used to display free power
    private static final DecimalFormat FORMAT = new DecimalFormat("##0.00");
    
    private static final String mModel = EloBuild.HWMODEL;
    private static final String MODEL_I4_STD_10 = EloBuild.MODEL_POS_I40_IN10_STD;
    private static final String MODEL_I4_STD_15 = EloBuild.MODEL_POS_I40_IN15_STD;
    private static final String MODEL_I4_STD_22 = EloBuild.MODEL_POS_I40_IN22_STD;
    
    // Message codes; see mHandler below.
    private static final int EVENT_RESET_BRIGHTNESS = 1;
    private static final int EVENT_RESET_VOLUME = 2;
    private static final int EVENT_RESTORE_POE_SETTINGS_START = 3;
    private static final int EVENT_RESTORE_POE_SETTINGS_COMPLETE = 4;

    private boolean mHasStoragePermission = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EloPeripheralManager elo = new EloPeripheralManager(this, null);
        if (!supportPoePowerManagerUi() || !elo.hasHardwareFeature(EloPeripheralManager.FEATRUE_POE)) {
            Log.d(TAG, "Device not support POE power manager, exit");
            finish();
            return;
        }

        // Hook any PoE settings if power source is PoE, but now PoE system is not ready
        PoE poeCtl = elo.getPoeCtl();
        if (poeCtl.isPoeOn() && !poeCtl.isPoeSystemReady()) {
            Toast.makeText(this, "PoE system is not ready, please retry later", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PoeSettingsFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestStoragePermission();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean supportPoePowerManagerUi() {
        // Only i-series STD supports PoE power manager settings UI
        if (mModel.equals(MODEL_I4_STD_10)
                || mModel.equals(MODEL_I4_STD_15)
                || mModel.equals(MODEL_I4_STD_22)) {
            return true;
        }

        return false;
    }

    public static class PoeSettingsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {
        
        private int mMinimumBacklight;
        private int mMaximumBacklight;  
        private int mMaximumMusicVolume;
        
        private int mLastBrightnessForPoe;
        private int mLastMusicVolumeForPoe;
        
        // mapping: preference Key  <-> USB ports id
        private Map<String, Integer> mUsbPortKeyMap = null;
        
        private PowerManager mPowerManager;
        private AudioManager mAudioManager;

        private PoE mPoeCtl;
        private EloPeripheralManager mEloManager;
          
        private SeekBarPreference mBrightnessPref;
        private SeekBarPreference mVolumePref; 
        
        private SwitchPreference mBtToggle;
        private SwitchPreference mWifiToggle;      
        private SwitchPreference mGpioToggle;
        private SwitchPreference mHdmiToggle;
        private SwitchPreference[] mUsbPortToggle; 
        
        private Preference mPoePowerPref;
        private ProgressDialog mPrgDialog;
        
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "handleMessage what: " + msg.what + ", arg1:" + msg.arg1);
                switch (msg.what) {
                    case EVENT_RESET_BRIGHTNESS:
                        // reset seek bar if set brightness fail
                        mBrightnessPref.setValue(mLastBrightnessForPoe - mMinimumBacklight);
                        break;

                    case EVENT_RESET_VOLUME:
                        // reset seek bar if set volume fail
                        mVolumePref.setValue(mLastMusicVolumeForPoe);
                        break;

                    case EVENT_RESTORE_POE_SETTINGS_COMPLETE:
                        // refresh UI after reset to default setting
                        dismissDialogs();
                        refreshUi();
                }
            }
        };
        
        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference == mBrightnessPref || preference == mVolumePref) {
                // handle new progress of seek bar in onPreferenceChange, so return true here
                return true;
            }

            // Get feature type while on/off in SwitchPreference
            int feature = -1;
            if (preference == mBtToggle) {
                feature = PoE.POE_FEATURE_BT;
            } else if (preference == mWifiToggle) {
                feature = PoE.POE_FEATURE_WIFI;
            } else if (preference == mGpioToggle) {
                feature = PoE.POE_FEATURE_GPIO_OUT;
            } else if (preference == mHdmiToggle) {
                feature = PoE.POE_FEATURE_HDMI_OUTPUT;
            } else {
                for (SwitchPreference toggle : mUsbPortToggle) {
                    if (toggle != null && toggle == preference) {
                        feature = mUsbPortKeyMap.get(toggle.getKey());
                    }
                }
            }

            // enable/disable POE feature
            if (feature != -1) {
                SwitchPreference toggle = (SwitchPreference) preference;
                boolean on = toggle.isChecked();
                boolean success = mPoeCtl.enableFeature(feature, on);
                if (!success) {
                    // reset SwitchPreference if operation fail
                    toggle.setChecked(!on);
                } else {
                    updateFeatureSummary(toggle, feature, on ? 1 : 0);
                    updatePowerStatusSummary();
                }
                if (on && !success) {
                    // toast POE power overflow if enable feature fail
                    Toast.makeText(getContext(), R.string.poe_power_overflow, Toast.LENGTH_SHORT)
                            .show();
                }
                return true;
            }

            super.onPreferenceTreeClick(preference);
            return false;
        }
        
        @Override
        public boolean onPreferenceChange(Preference preference, Object objValue) {
            boolean success= false;
            if (preference == mBrightnessPref) {
                int newBrightness = ((Integer) objValue).intValue() + mMinimumBacklight;
                if (mLastBrightnessForPoe != newBrightness) {
                    success = mPoeCtl.setBrightnessForPoe(newBrightness);
                    if (!success) {
                        // toast POE power overflow if change POE brightness fail
                        Toast.makeText(getContext(), R.string.poe_power_overflow, Toast.LENGTH_SHORT)
                                .show();
                        mHandler.sendEmptyMessageDelayed(EVENT_RESET_BRIGHTNESS, 500);
                        //mBrightnessPref.setProgress(mLastBrightnessForPoe - mMinimumBacklight);
                    } else {
                        mLastBrightnessForPoe = newBrightness;
                        updateFeatureSummary(mBrightnessPref, PoE.POE_FEATURE_BRIGHTNESS,
                                mLastBrightnessForPoe);
                        updatePowerStatusSummary();
                    }
                }
                return true;
            }

            if (preference == mVolumePref) {
                int newMusicVolume = ((Integer) objValue).intValue();
                if (newMusicVolume != mLastMusicVolumeForPoe) {
                    int newRatio = newMusicVolume * 100 / mMaximumMusicVolume;
                    success = mPoeCtl.setVolumeRatioForPoe(newRatio);

                    if (!success) {
                        // toast POE power overflow if change POE volume fail
                        Toast.makeText(getContext(), R.string.poe_power_overflow, Toast.LENGTH_SHORT)
                                .show();
                        mHandler.sendEmptyMessageDelayed(EVENT_RESET_VOLUME, 500);
                        // mVolumePref.setProgress(mLastMusicVolumeForPoe);
                    } else {
                        mLastMusicVolumeForPoe = newMusicVolume;
                        updateFeatureSummary(mVolumePref, PoE.POE_FEATURE_VOLUME, newRatio);
                        updatePowerStatusSummary();
                    }
                }

                return true;
            }

            // always let the preference setting proceed.
            return true;
        }
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (mModel.equals(MODEL_I4_STD_10)
                    || mModel.equals(MODEL_I4_STD_15)
                    || mModel.equals(MODEL_I4_STD_22)) {
                addPreferencesFromResource(R.xml.poe_power_manager_settings_i4);
            }

            mEloManager = new EloPeripheralManager(getContext(), null);
            mPoeCtl = mEloManager.getPoeCtl();
            mPowerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

            mUsbPortKeyMap = new HashMap<String, Integer>();
            mUsbPortKeyMap.put(KEY_USB_PORT1, PoE.POE_FEATURE_USB_PORT1);
            mUsbPortKeyMap.put(KEY_USB_PORT2, PoE.POE_FEATURE_USB_PORT2);
            mUsbPortKeyMap.put(KEY_USB_PORT3, PoE.POE_FEATURE_USB_PORT3);
            mUsbPortKeyMap.put(KEY_USB_PORT4, PoE.POE_FEATURE_USB_PORT4);
            mUsbPortKeyMap.put(KEY_USB_PORT5, PoE.POE_FEATURE_USB_PORT5);
            mUsbPortKeyMap.put(KEY_USB_PORT6, PoE.POE_FEATURE_USB_PORT6);

            initScreen();

            setHasOptionsMenu(true);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            //do nothing
        }
        
        @Override
        public void onResume() {
            super.onResume();

            refreshUi();
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.poe_settings, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            Message msg;
            switch (item.getItemId()) {
                case android.R.id.home:
                    getActivity().onBackPressed();
                    return true;

                case R.id.poe_settings_restore:
                    new RestorePoeThread(getContext(), mPoeCtl).execute();
                    return true;

                default:
                    break;
            }
            return super.onOptionsItemSelected(item);
        }
        
        private void initScreen() {
            final PreferenceScreen preferenceScreen = getPreferenceScreen();

            // Get power status screen preference
            mPoePowerPref = preferenceScreen.findPreference(KEY_POE_POWER_STATUS);
            
            // Get BT and wifi screen preference
            mBtToggle = (SwitchPreference) preferenceScreen.findPreference(KEY_BT);
            mWifiToggle = (SwitchPreference) preferenceScreen.findPreference(KEY_WIFI);
            
            // Get all screen preference about USB ports
            mUsbPortToggle = new SwitchPreference[UsbPort.MAX_USB_PORT];
            for (int i = 0; i < UsbPort.MAX_USB_PORT; i++) {
                mUsbPortToggle[i] = (SwitchPreference) preferenceScreen
                        .findPreference(USB_PORT_KEY_LIST[i]);
            }
            
            // Get brightness screen preference and set max for seek bar
            mMinimumBacklight = getMinimumScreenBrightnessSetting();
            mMaximumBacklight = getMaximumScreenBrightnessSetting();
            mBrightnessPref = (SeekBarPreference) preferenceScreen.findPreference(KEY_BRIGHTNESS);
            if (mBrightnessPref != null) {
                mBrightnessPref.setMax(mMaximumBacklight - mMinimumBacklight);
                mBrightnessPref.setOnPreferenceChangeListener(this);
            }
            
            // Get volume screen preference
            mVolumePref = (SeekBarPreference) preferenceScreen.findPreference(KEY_VOLUME);
            mMaximumMusicVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            if (mVolumePref != null) {
                mVolumePref.setMax(mMaximumMusicVolume);
                mVolumePref.setOnPreferenceChangeListener(this);
            }
            
            // Get GPIO screen preference
            mGpioToggle = (SwitchPreference) preferenceScreen.findPreference(KEY_GPIO_OUTPUT);
            
            // Get HDMI screen preference
            mHdmiToggle = (SwitchPreference) preferenceScreen.findPreference(KEY_HDMI_OUTPUT);        
        }
        
        private void refreshUi() {
            boolean on = false;
            
            // updateFeatureSummary for the power status
            if (mPoePowerPref != null) {
                updatePowerStatusSummary();
            }
            
            // update screen preference about USB ports
            for (int i = 0; i < UsbPort.MAX_USB_PORT; i++) {
                if (mUsbPortToggle[i] != null) {
                    on = mPoeCtl.isFeatureEnabled(i + 1);
                    mUsbPortToggle[i].setChecked(on);
                    updateFeatureSummary(mUsbPortToggle[i], i + 1, on ? 1 : 0);
                }
            }
            
            // update BT screen preference
            if (mBtToggle != null) {
                on = mPoeCtl.isFeatureEnabled(PoE.POE_FEATURE_BT);
                mBtToggle.setChecked(on);
                updateFeatureSummary(mBtToggle, PoE.POE_FEATURE_BT, on ? 1 : 0);
            }
            
            // update wifi screen preference
            if (mWifiToggle != null) {
                on = mPoeCtl.isFeatureEnabled(PoE.POE_FEATURE_WIFI);
                mWifiToggle.setChecked(on);
                updateFeatureSummary(mWifiToggle, PoE.POE_FEATURE_WIFI, on ? 1 : 0);
            }
            
            // update GPIO screen preference
            if (mGpioToggle != null) {
                on = mPoeCtl.isFeatureEnabled(PoE.POE_FEATURE_GPIO_OUT);
                mGpioToggle.setChecked(on);
                updateFeatureSummary(mGpioToggle, PoE.POE_FEATURE_GPIO_OUT, on ? 1 : 0);
            }
            
            // update HDMI screen preference
            if (mHdmiToggle != null) {
                on = mPoeCtl.isFeatureEnabled(PoE.POE_FEATURE_HDMI_OUTPUT);
                mHdmiToggle.setChecked(on);
                updateFeatureSummary(mHdmiToggle, PoE.POE_FEATURE_HDMI_OUTPUT, on ? 1 : 0);
            }
            
            // update progress of brightness screen preference
            if (mBrightnessPref != null) {
                mLastBrightnessForPoe = mPoeCtl.getBrightnessForPoe();
                mBrightnessPref.setValue(mLastBrightnessForPoe - mMinimumBacklight);
                updateFeatureSummary(mBrightnessPref, PoE.POE_FEATURE_BRIGHTNESS, mLastBrightnessForPoe);
            }
            
            // update progress of volume screen preference
            if (mVolumePref != null) {
                int volumeRatioForPoe = mPoeCtl.getVolumeRatioForPoe();
                mLastMusicVolumeForPoe = mPoeCtl.getStreamVolumeForPoe(AudioManager.STREAM_MUSIC);
                mVolumePref.setValue(mLastMusicVolumeForPoe);
                updateFeatureSummary(mVolumePref, PoE.POE_FEATURE_VOLUME, volumeRatioForPoe);
            }      
        }
        
        private void updateFeatureSummary(Preference pref, int feature, int value) {
            final String message;
            final String percentage;
            final float featurePower = mPoeCtl.getPowerForFeature(feature);
            final String powerStr = FORMAT.format(featurePower);

            if (pref == mBrightnessPref) {
                percentage = getBrightnessPercentage(value);
                message = getString(R.string.poe_brightness_summary, percentage, powerStr);
                //pref.setSummary(message);
                // display power in title for seekbar preference
                pref.setTitle(message);
            } else if (pref == mVolumePref) {
                percentage = getVolumePercentage(value);
                message = getString(R.string.poe_media_summary, percentage, powerStr);
                //pref.setSummary(message);
                // display power in title for seekbar preference
                pref.setTitle(message);
            } else {
                // For the on/off feature setting
                // if (value > 0) {
                // message = getString(R.string.poe_feature_on_summary, powerStr);
                // } else {
                // message = getString(R.string.poe_feature_off_summary, powerStr);
                // }
                message = getString(R.string.poe_feature_onoff_summary, powerStr);
                pref.setSummary(message);
            }
        }
        
        private void updatePowerStatusSummary() {
            final float maxPower = mPoeCtl.getMaxPowerCapacity();
            final float userPower = mPoeCtl.getUserMaxPower();
            final float freePower = mPoeCtl.getUserAvailablePower();
            String message = getString(R.string.poe_power_summary, FORMAT.format(maxPower),
                    FORMAT.format(userPower - freePower), FORMAT.format(userPower));
            if (mPoePowerPref != null) {
                mPoePowerPref.setSummary(message);
            }
        }
        
        private String getBrightnessPercentage(int value) {
            final double dValue = value;
            double ratio = 0.0;

            if (dValue > mMaximumBacklight) {
                ratio = 1.0;
            } else if (dValue < mMinimumBacklight) {
                ratio = 0.0;
            } else {
                ratio = (dValue - mMinimumBacklight) / (mMaximumBacklight - mMinimumBacklight);
            }

            return NumberFormat.getPercentInstance().format(ratio);
        }
        
        private String getVolumePercentage(int ratio) {
            final double dRatio = ratio;
            return NumberFormat.getPercentInstance().format(dRatio / 100);
        }
        
        private int getMinimumScreenBrightnessSetting() {
            /*
             * mPowerManager.getMinimumScreenBrightnessSetting is hided, so we use reflect method
             * here.
             */
            // mPowerManager.getMinimumScreenBrightnessSetting();

            int resId = Resources.getSystem().getIdentifier("config_screenBrightnessSettingMinimum",
                    "integer", "android");
            int min = Resources.getSystem().getInteger(resId);
            Log.d(TAG, "MinimumScreenBrightnessSetting: " + min);
            return min;
        }
        
        private int getMaximumScreenBrightnessSetting() {
            /*
             * mPowerManager.getMaximumScreenBrightnessSetting is hided, so we use reflect method
             * here.
             */
            // mPowerManager.getMaximumScreenBrightnessSetting();

            int resId = Resources.getSystem().getIdentifier("config_screenBrightnessSettingMaximum",
                    "integer", "android");
            int max = Resources.getSystem().getInteger(resId);
            Log.d(TAG, "MaximumScreenBrightnessSetting: " + max);
            return max;
        }

        private class RestorePoeThread extends AsyncTask<Void, Void, Boolean> {
            private Context mContext;
            private PoE mPoeCtl = null;

            public RestorePoeThread(Context context, PoE poeCtl) {
                mContext = context;
                mPoeCtl = poeCtl;
            }

            @Override
            protected void onPreExecute() {
                showRestoreDialog();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                // if /sdcard/Download/poe-cust.xml exist, restore POE settings with this xml
                Bundle customData = null;
                if (new File(SDCARD_POE_CUST_PATH).exists()) {
                    customData = PoeCustomBundle.createPoeBundle(SDCARD_POE_CUST_PATH);
                } else {
                    //restore with poe-cust.xml from assets
                    AssetManager manager = getContext().getAssets();
                    if (mModel.equals(MODEL_I4_STD_10)) {
                        customData = PoeCustomBundle.createPoeBundleFromAsset(manager, "poe-cust-i4-std_in10.xml");
                    } else if (mModel.equals(MODEL_I4_STD_15)) {
                        customData = PoeCustomBundle.createPoeBundleFromAsset(manager, "poe-cust-i4-std_in15.xml");
                    } else if (mModel.equals(MODEL_I4_STD_22)) {
                        customData = PoeCustomBundle.createPoeBundleFromAsset(manager, "poe-cust-i4-std_in22.xml");
                    } else {
                        //Go to reset to default if there is not any poe-cust.xml
                        customData = null;
                    }
                }
                mPoeCtl.restorePoeSettings(customData);
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                // Wait PoEController apply new settings and then goto dismiss dialog and refresh POE UI
                mHandler.sendEmptyMessageDelayed(EVENT_RESTORE_POE_SETTINGS_COMPLETE, 1000);
            }
        }


        private void showRestoreDialog() {
            mPrgDialog = ProgressDialog.show(getContext(),
                    getContext().getString(R.string.poe_restore_progress_title),
                    getContext().getString(R.string.poe_restore_progress_text));
        }

        private void dismissDialogs() {
            if (mPrgDialog != null) {
                mPrgDialog.dismiss();
                mPrgDialog = null;
            }
        }
    }

    private void requestStoragePermission() {
        String[] permissionsToRequest = new String[1];
        int permissionsRequestIndex = 0;

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.READ_EXTERNAL_STORAGE;
            Log.v(TAG, "requestPermissions count: " + permissionsToRequest.length);
            requestPermissions(permissionsToRequest, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult, requestCode:" + requestCode);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mHasStoragePermission = true;
            }else {
                mHasStoragePermission = false;
            }
        }
    }
}
