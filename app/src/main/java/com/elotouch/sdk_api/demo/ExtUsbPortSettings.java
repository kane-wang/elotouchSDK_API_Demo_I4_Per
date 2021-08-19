package com.elotouch.sdk_api.demo;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.elotouch.library.EloBuild;
import com.elotouch.library.EloPeripheralManager;
import com.elotouch.library.EloPeripheralManager.ExtHubType;
import com.elotouch.library.EloPeripheralManager.ExtUsbPort;
import com.elotouch.library.usb.UsbPortSwitcher;

import java.util.HashMap;
import java.util.Map;

public final class ExtUsbPortSettings extends PreferenceActivity {
    private static final String TAG = "SDK_API_Demo_ExtUsbPort";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        private ExtUsbPort mExtUsbPortCtl;
        // indicate to switch data role or power role
        private int mRole = UsbPortSwitcher.ROLE_FLAG_DATA;
        private ExtHubType mExtHubType = ExtHubType.UNKNOW;
        private boolean mSettingOngoing = false;
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mEloManager = new EloPeripheralManager(getActivity(), null);
            mExtUsbPortCtl = mEloManager.getExtUsbPortCtl();            
            mExtHubType = mExtUsbPortCtl.getConnectedExtHub();

            if (mExtHubType == ExtHubType.POS_HUB) {
                addPreferencesFromResource(R.xml.usb_port_settings_pos_hub);
                getActivity().setTitle(R.string.pos_hub_usb_port_setting_title);
            } else if (mExtHubType == ExtHubType.IO_HUB) {
                addPreferencesFromResource(R.xml.usb_port_settings_io_hub);
                getActivity().setTitle(R.string.io_hub_usb_port_setting_title);
            } else if (mExtHubType == ExtHubType.POS_STAND_CFD) {
                addPreferencesFromResource(R.xml.usb_port_settings_pos_stand);
                getActivity().setTitle(R.string.pos_stand_hub_setting_title);
            } else if (mExtHubType == ExtHubType.POS_STAND_NO_CFD) {
                addPreferencesFromResource(R.xml.usb_port_settings_pos_stand_no_cfd);
                getActivity().setTitle(R.string.pos_stand_no_cfd_hub_setting_title);
            }else if (mExtHubType == ExtHubType.FLIP_STAND) {
                addPreferencesFromResource(R.xml.usb_port_settings_flip_stand);
                getActivity().setTitle(R.string.flip_stand_hub_setting_title);
            } else {
                getActivity().onBackPressed();
            }

            initScreen();
            mUsbPortKeyMap = new HashMap<String, Integer>();
            mUsbPortKeyMap.put(KEY_USB_PORT1, ExtUsbPort.EXT_USER_USB_PORT1);
            mUsbPortKeyMap.put(KEY_USB_PORT2, ExtUsbPort.EXT_USER_USB_PORT2);
            mUsbPortKeyMap.put(KEY_USB_PORT3, ExtUsbPort.EXT_USER_USB_PORT3);
            mUsbPortKeyMap.put(KEY_USB_PORT4, ExtUsbPort.EXT_USER_USB_PORT4);
            mUsbPortKeyMap.put(KEY_USB_PORT5, ExtUsbPort.EXT_USER_USB_PORT5);
            mUsbPortKeyMap.put(KEY_USB_PORT6, ExtUsbPort.EXT_USER_USB_PORT6);

            setHasOptionsMenu(true);
        }

        @Override
        public void onResume() {
            super.onResume();

            // Register for misc other intent broadcasts.
            IntentFilter intentFilter =
                    new IntentFilter(EloPeripheralManager.ACTION_EXT_USB_HUB_STATE);
            getActivity().registerReceiver(mIntentReceiver, intentFilter);
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivity().unregisterReceiver(mIntentReceiver);
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

                    Message msg = mHandler.obtainMessage(toggle.isChecked() ? EVENT_ENABLE_USB_PORT
                            : EVENT_DISABLE_USB_PORT, port, 0, null);
                    mExtUsbPortCtl.enableUsbPort(port, toggle.isChecked(), mRole, msg);
                    mSettingOngoing = true;
                    // disable toggle until this enable/disable action done
                    if ((mRole & UsbPortSwitcher.ROLE_FLAG_DATA) != 0) {
                        toggle.setEnabled(false);
                    }
                    return true;
                }
            }

            return false;
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
            for (int i = 0; i < ExtUsbPort.MAX_EXT_USB_PORT; i++) {
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
            dataItem.setEnabled(mRole != UsbPortSwitcher.ROLE_FLAG_DATA &&
                    (mExtHubType == ExtHubType.POS_STAND_CFD
                            || mExtHubType == ExtHubType.POS_STAND_NO_CFD
                            || mExtHubType == ExtHubType.FLIP_STAND));
            powerItem.setEnabled(mRole != UsbPortSwitcher.ROLE_FLAG_POWER &&
                    (mExtHubType == ExtHubType.POS_STAND_CFD
                            || mExtHubType == ExtHubType.POS_STAND_NO_CFD
                            || mExtHubType == ExtHubType.FLIP_STAND));
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
                    mExtUsbPortCtl.enableAllUsbPorts(true, mRole, msg);
                    return true;

                case R.id.usb_port_menu_disable_all_ports:
                    msg = mHandler.obtainMessage(EVENT_ENABLE_ALL_USB_PORT, -1, 0, null);
                    // make sure msg.replyTo is not null for across processes
                    if (msg != null) {
                        msg.replyTo = new Messenger(msg.getTarget());
                    }
                    mExtUsbPortCtl.enableAllUsbPorts(false, mRole, msg);
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

            mPortToggle = new SwitchPreference[ExtUsbPort.MAX_EXT_USB_PORT];
            boolean[] portStatus = mExtUsbPortCtl.getUsbPortStatus(mRole);
            for (int i = 0; i < ExtUsbPort.MAX_EXT_USB_PORT; i++) {
                mPortToggle[i] = (SwitchPreference) preferenceScreen.findPreference(KEY_LIST[i]);
                if (mPortToggle[i] != null) {
                    mPortToggle[i].setChecked(portStatus[i]);
                    mPortToggle[i].setEnabled(true);
                }
            }
        }

        private void refreshScreen() {
            boolean[] portStatus = mExtUsbPortCtl.getUsbPortStatus(mRole);
            for (int i = 0; i < ExtUsbPort.MAX_EXT_USB_PORT; i++) {
                if (mPortToggle[i] != null) {
                    mPortToggle[i].setChecked(portStatus[i]);
                    mPortToggle[i].setEnabled(true);
                }
            }
        }

        private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "onReceive:" + action);
                if (action.equals((EloPeripheralManager.ACTION_EXT_USB_HUB_STATE))) {
                    boolean hubRemoved = false;
                    Log.d(TAG, "Receive external HUB connect/disconnect event");
                    ExtHubType connectedHub = mExtUsbPortCtl.getConnectedExtHub();
                    if (mExtHubType != connectedHub) {
                        hubRemoved = true;
                    }
                    // If external HUB removed, finish activity
                    if (hubRemoved) {
                        getActivity().onBackPressed();
                    }
                }
            }
        };
    }
}