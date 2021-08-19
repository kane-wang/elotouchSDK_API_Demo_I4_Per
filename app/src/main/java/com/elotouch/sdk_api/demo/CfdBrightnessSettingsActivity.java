package com.elotouch.sdk_api.demo;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
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
import com.elotouch.library.EloPeripheralManager.ExtHubType;
import com.elotouch.library.EloPeripheralManager.ExtUsbPort;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public final class CfdBrightnessSettingsActivity extends PreferenceActivity {
    private static final String TAG = "SDK_API_CFD_demo";

    // String keys for preference lookup
    private static final String KEY_CFD_TURN_ON_BACKLIGHT = "key_cfd_turn_on_backlight";
    private static final String KEY_CFD_SYNC_BACKLIGHT = "key_cfd_sync_backlight";
    private static final String KEY_CFD_SET_BACKLIGHT = "key_cfd_set_backlight";

    // format is used to display free power
    private static final DecimalFormat FORMAT = new DecimalFormat("##0.00");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EloPeripheralManager elo = new EloPeripheralManager(this, null);
        ExtUsbPort extUsbPortCtl = elo.getExtUsbPortCtl();
        ExtHubType hub = extUsbPortCtl.getConnectedExtHub();
        
        if (hub != ExtHubType.POS_STAND_CFD) {
            Log.d(TAG, "POS STAND with CFD not exist, exit");
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
                new CfdSettingsFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    public static class CfdSettingsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {
        private boolean mCfdOn = false;
        private int mMinimumBacklight;
        private int mMaximumBacklight;  
        private int mLastBrightnessForCfd;
        //private boolean mSyncWithSettingsApp = true;
        
        private PowerManager mPowerManager;
        private EloPeripheralManager mEloManager;

        private SwitchPreference mCfdSwitcherPref;
        private SeekBarPreference mBrightnessPref;        
        private SwitchPreference mSyncToggle;

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference == mBrightnessPref) {
                // handle new progress of seek bar in onPreferenceChange, so return true here
                return true;
            } else if(preference == mCfdSwitcherPref) {
                mCfdOn = mCfdSwitcherPref.isChecked();
                mEloManager.enableCfd(mCfdOn);
                // Enable/Disable CFD brightness after turning on/off CFD
                mBrightnessPref.setEnabled(mCfdOn);
                mSyncToggle.setEnabled(mCfdOn);
                return true;
            } else if (preference == mSyncToggle) {
                boolean on = mSyncToggle.isChecked();
            }

            super.onPreferenceTreeClick(preference);
            return false;
        }
        
        @Override
        public boolean onPreferenceChange(Preference preference, Object objValue) {
            if (preference == mBrightnessPref) {
                int newBrightness = ((Integer) objValue).intValue() + mMinimumBacklight;
                if (mLastBrightnessForCfd != newBrightness) {
                    mEloManager.setBrightnessForCFD(newBrightness, mSyncToggle.isChecked());
                    mLastBrightnessForCfd = newBrightness;
                    updateBrightnessSummary(mBrightnessPref, mLastBrightnessForCfd);
                }
                return true;
            }

            // always let the preference setting proceed.
            return true;
        }
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.cfd_brightness);

            mEloManager = new EloPeripheralManager(getContext(), null);
            mPowerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
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
            mCfdOn = mEloManager.isCfdEnabled();
            refreshUi();
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            Message msg;
            switch (item.getItemId()) {
                case android.R.id.home:
                    getActivity().onBackPressed();
                    return true;

                default:
                    break;
            }
            return super.onOptionsItemSelected(item);
        }
        
        private void initScreen() {
            final PreferenceScreen preferenceScreen = getPreferenceScreen();

            // Get the CFD switcher screen preference
            mCfdSwitcherPref = (SwitchPreference) preferenceScreen
                    .findPreference(KEY_CFD_TURN_ON_BACKLIGHT);

            // Get Brightness sync screen preference
            mSyncToggle = (SwitchPreference) preferenceScreen
                    .findPreference(KEY_CFD_SYNC_BACKLIGHT);

            // Get CFD brightness screen preference and set max for seek bar
            mMinimumBacklight = getMinimumScreenBrightnessSetting();
            mMaximumBacklight = getMaximumScreenBrightnessSetting();
            mBrightnessPref = (SeekBarPreference) preferenceScreen
                    .findPreference(KEY_CFD_SET_BACKLIGHT);
            if (mBrightnessPref != null) {
                mBrightnessPref.setMax(mMaximumBacklight - mMinimumBacklight);
                mBrightnessPref.setOnPreferenceChangeListener(this);
            }
        }
        
        private void refreshUi() {
            boolean on = false;

            mCfdSwitcherPref.setChecked(mCfdOn);

            // update progress of brightness screen preference
            if (mBrightnessPref != null) {
                mLastBrightnessForCfd = mEloManager.getBrightnessForCFD();
                mBrightnessPref.setValue(mLastBrightnessForCfd - mMinimumBacklight);
                updateBrightnessSummary(mBrightnessPref, mLastBrightnessForCfd);
                mBrightnessPref.setEnabled(mCfdOn);
            }

            if(mSyncToggle != null) {
                mSyncToggle.setEnabled(mCfdOn);
            }
        }
        
        private void updateBrightnessSummary(Preference pref, int value) {
            final String message;
            final String percentage;

            if (pref == mBrightnessPref) {
                percentage = getBrightnessPercentage(value);
                message = getString(R.string.cfd_brightness_level_summary, percentage);
                // display percentage in title for seekbar preference
                pref.setTitle(message);
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
    }
}
