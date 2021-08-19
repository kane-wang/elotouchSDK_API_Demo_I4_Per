package com.elotouch.sdk_api.demo;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.elotouch.library.EloBuild;
import com.elotouch.library.EloPeripheralManager;
import com.elotouch.library.EloPeripheralManager.PoE;
import com.elotouch.library.EloPeripheralManager.UsbPort;
import com.elotouch.library.usb.UsbPortSwitcher;

import java.util.HashMap;
import java.util.Map;

public final class UsbPortSettings extends PreferenceActivity {
    private static final String TAG = "SDK_API_Demo_UsbPort";

    private static final String HWMODEL = EloBuild.HWMODEL;
    private static final String MODEL_I4_STD_10 = EloBuild.MODEL_POS_I40_IN10_STD;
    private static final String MODEL_I4_STD_15 = EloBuild.MODEL_POS_I40_IN15_STD;
    private static final String MODEL_I4_STD_22 = EloBuild.MODEL_POS_I40_IN22_STD;
    private static final String MODEL_I4_PUCK = EloBuild.MODEL_POS_I40_BPACK;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!(HWMODEL.equals(MODEL_I4_STD_10)
                || HWMODEL.equals(MODEL_I4_STD_15)
                || HWMODEL.equals(MODEL_I4_STD_22)
                || HWMODEL.equals(MODEL_I4_PUCK))) {
            finish();
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new UsbPortSettingsFragment()).commit();
    }

    public static class UsbPortSettingsFragment extends PreferenceFragment {
        // String keys for preference lookup
        private static final String KEY_USB_PORT1 = "key_usb_port1";
        private static final String KEY_USB_PORT2 = "key_usb_port2";
        private static final String KEY_USB_PORT3 = "key_usb_port3";
        private static final String KEY_USB_PORT4 = "key_usb_port4";
        private static final String KEY_USB_PORT5 = "key_usb_port5";
        private static final String KEY_USB_PORT6 = "key_usb_port6";

        private static final String[] KEY_LIST = {
                KEY_USB_PORT1, KEY_USB_PORT2,
                KEY_USB_PORT3, KEY_USB_PORT4,
                KEY_USB_PORT5, KEY_USB_PORT6
        };

        // Message codes; see mHandler below.
        private static final int EVENT_ENABLE_USB_PORT = 1;
        private static final int EVENT_DISABLE_USB_PORT = 2;
        private static final int EVENT_ENABLE_ALL_USB_PORT = 3;
        private static final int EVENT_DISABLE_ALL_USB_PORT = 4;
        private static final int EVENT_POE_STATE_CHANGE = 5;

        // mapping: preference Key  <-> USB ports
        private Map<String, Integer> mUsbPortKeyMap = null;
        private SwitchPreference[] mPortToggle;

        private EloPeripheralManager mEloManager;
        private UsbPort mMainUsbPortCtl;
        private PoE mPoeCtrl;
        // indicate to switch data role or power role
        private int mRole = UsbPortSwitcher.ROLE_FLAG_DATA;
        private boolean mSettingOngoing = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (HWMODEL.equals(MODEL_I4_STD_10)
                    || HWMODEL.equals(MODEL_I4_STD_15)
                    || HWMODEL.equals(MODEL_I4_STD_22)) {
                addPreferencesFromResource(R.xml.usb_port_settings_i4);
            } else if (HWMODEL.equals(MODEL_I4_PUCK)) {
                addPreferencesFromResource(R.xml.usb_port_settings_puck);
            } else {
                getActivity().onBackPressed();
                return;
            }

            mEloManager = new EloPeripheralManager(getActivity(), null);
            mMainUsbPortCtl = mEloManager.getUsbPortCtl();
            mPoeCtrl = mEloManager.getPoeCtl();

            // display USB power status as default if POE is on
            if (mPoeCtrl.isPoeOn()) {
                mRole = UsbPortSwitcher.ROLE_FLAG_POWER;
            }

            initScreen();
            mUsbPortKeyMap = new HashMap<String, Integer>();
            mUsbPortKeyMap.put(KEY_USB_PORT1, UsbPort.USER_USB_PORT1);
            mUsbPortKeyMap.put(KEY_USB_PORT2, UsbPort.USER_USB_PORT2);
            mUsbPortKeyMap.put(KEY_USB_PORT3, UsbPort.USER_USB_PORT3);
            mUsbPortKeyMap.put(KEY_USB_PORT4, UsbPort.USER_USB_PORT4);
            mUsbPortKeyMap.put(KEY_USB_PORT5, UsbPort.USER_USB_PORT5);
            mUsbPortKeyMap.put(KEY_USB_PORT6, UsbPort.USER_USB_PORT6);

            setHasOptionsMenu(true);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)  {
            if (ActivityManager.isUserAMonkey()) {
                return false;
            }
            
            // toggle is disabled if on/off action is not complete
            if (preference != null && preference.isEnabled() == false) {
                SwitchPreference pref = (SwitchPreference) preference;
                pref.setChecked(pref.isChecked() ? false : true);
                return true;
            }
            
            if (mSettingOngoing) {
                SwitchPreference pref = (SwitchPreference) preference;
                pref.setChecked(pref.isChecked() ? false : true);
                return true;
            }
                        
            for (SwitchPreference toggle : mPortToggle) {
                if (toggle != null && toggle == preference) {
                    int port = mUsbPortKeyMap.get(toggle.getKey());
                    if (toggle.isChecked() && !allowInPoe(port, mRole)) {
                        Log.w(TAG, "Not allow to enable USB port " + port + " when PoE on");
                        Toast.makeText(getActivity(), R.string.poe_not_allow, Toast.LENGTH_SHORT)
                                .show();
                        toggle.setChecked(false);
                        return true;
                    }

                    Message msg = mHandler.obtainMessage(toggle.isChecked() ? EVENT_ENABLE_USB_PORT
                            : EVENT_DISABLE_USB_PORT, port, 0, null);
                    // make sure msg.replyTo is not null for across processes
                    if (msg != null) {
                        msg.replyTo = new Messenger(msg.getTarget());
                    }

                    Log.d(TAG, "enableUsbPort, port: " + port + ", on/off:" + toggle.isChecked() + ", role:" + mRole);
                    mMainUsbPortCtl.enableUsbPort(port, toggle.isChecked(), mRole, msg);
                    mSettingOngoing = true;
                    // disable toggles until this enable/disable action done
                    if ((mRole & UsbPortSwitcher.ROLE_FLAG_DATA) != 0) {
                        toggle.setEnabled(false);
                    }
                    
                    return true;
                }
            }

            return false;
        }
        
        private void disablePeferences() {
            // disable all toggles until this enable/disable action done
            for (SwitchPreference toggle : mPortToggle) {
                if (toggle != null) {
                    toggle.setEnabled(false);
                }
            }
        }

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "handleMessage what: " + msg.what + ", port:" + msg.arg1 + ", ret:"
                        + (Integer) msg.obj);
                switch (msg.what) {
                    case EVENT_ENABLE_USB_PORT:
                    case EVENT_DISABLE_USB_PORT:
                        mSettingOngoing = false;
                        refreshScreen();
                        break;

                    case EVENT_ENABLE_ALL_USB_PORT:
                    case EVENT_DISABLE_ALL_USB_PORT:
                        mSettingOngoing = false;
                        refreshScreen();
                        break;

                    case EVENT_POE_STATE_CHANGE:
                        refreshScreen();
                        break;
                }
            }
        };

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            //if (UserManager.get(getContext()).isAdminUser()) {
                inflater.inflate(R.menu.usb_port, menu);
            //}
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            final Context context = getActivity();
            final MenuItem menuEnableAll = menu.findItem(R.id.usb_port_menu_enable_all_ports);
            final MenuItem menuDisableAll = menu.findItem(R.id.usb_port_menu_disable_all_ports);
            if (menuEnableAll == null || menuDisableAll == null) {
                return;
            }

            boolean hasPortOn = false;
            boolean hasPortOff = false;
            for (int i = 0; i < UsbPort.MAX_USB_PORT; i++) {
                if (mPortToggle[i] != null && mPortToggle[i].isChecked()) {
                    hasPortOn = true;
                } else if (mPortToggle[i] != null) {
                    hasPortOff = true;
                }
                if (hasPortOn && hasPortOff) {
                    break;
                }
            }

            menuEnableAll.setEnabled(hasPortOff);
            menuDisableAll.setEnabled(hasPortOn);

            final MenuItem dataItem = menu.findItem(R.id.usb_port_menu_data_role);
            final MenuItem powerItem = menu.findItem(R.id.usb_port_menu_power_role);
            dataItem.setEnabled(mRole != UsbPortSwitcher.ROLE_FLAG_DATA);
            powerItem.setEnabled(mRole != UsbPortSwitcher.ROLE_FLAG_POWER);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            Message msg;
            switch (item.getItemId()) {
                case android.R.id.home:
                    getActivity().onBackPressed();
                    return true;

                case R.id.usb_port_menu_enable_all_ports:
                    msg = mHandler.obtainMessage(EVENT_ENABLE_ALL_USB_PORT, -1, 0, null);
                    // make sure msg.replyTo is not null for across processes
                    if (msg != null) {
                        msg.replyTo = new Messenger(msg.getTarget());
                    }
                    mMainUsbPortCtl.enableAllUsbPorts(true, mRole, msg);
                    return true;

                case R.id.usb_port_menu_disable_all_ports:
                    msg = mHandler.obtainMessage(EVENT_ENABLE_ALL_USB_PORT, -1, 0, null);
                    // make sure msg.replyTo is not null for across processes
                    if (msg != null) {
                        msg.replyTo = new Messenger(msg.getTarget());
                    }
                    mMainUsbPortCtl.enableAllUsbPorts(false, mRole, msg);
                    return true;

                case R.id.usb_port_menu_data_role:
                    // Go to check data role status of usb ports
                    mRole = UsbPortSwitcher.ROLE_FLAG_DATA;
                    refreshScreen();
                    return true;

                case R.id.usb_port_menu_power_role:
                    // Go to check power role status of usb ports
                    mRole = UsbPortSwitcher.ROLE_FLAG_POWER;
                    refreshScreen();
                    return true;

                default:
                    break;
            }
            return super.onOptionsItemSelected(item);
        }

        private void initScreen() {
            final PreferenceScreen preferenceScreen = getPreferenceScreen();

            mPortToggle = new SwitchPreference[UsbPort.MAX_USB_PORT];
            boolean[] portStatus = mMainUsbPortCtl.getUsbPortStatus(mRole);
            for (int i = 0; i < UsbPort.MAX_USB_PORT; i++) {
                mPortToggle[i] = (SwitchPreference) preferenceScreen.findPreference(KEY_LIST[i]);
                if (mPortToggle[i] != null) {
                    mPortToggle[i].setChecked(portStatus[i]);
                    mPortToggle[i].setEnabled(true);
                }
            }
        }

        private void refreshScreen() {
            boolean[] portStatus = mMainUsbPortCtl.getUsbPortStatus(mRole);
            for (int i = 0; i < UsbPort.MAX_USB_PORT; i++) {
                if (mPortToggle[i] != null) {
                    mPortToggle[i].setChecked(portStatus[i]);
                    mPortToggle[i].setEnabled(true);
                }
            }
        }

        private boolean allowInPoe(int port, int role) {
            // POE restrict power role on/off only, don't restrict data role on/off
            if (role == UsbPortSwitcher.ROLE_FLAG_DATA) {
                return true;
            }

            // When PoE is off or current model not support POE, not limit any usb ports
            if (!mPoeCtrl.isPoeOn()) {
                return true;
            }
            // check allow or not according with PoE power manager settings
            return mPoeCtrl.isFeatureEnabled(port);
        }
    }
}