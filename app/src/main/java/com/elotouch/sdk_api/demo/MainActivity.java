
package com.elotouch.sdk_api.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Handler.Callback;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.elotouch.library.EloBuild;
import com.elotouch.library.EloPeripheralEventListener;
import com.elotouch.library.EloPeripheralManager;
import com.elotouch.library.EloPeripheralManager.CDVoltage;
import com.elotouch.library.EloPeripheralManager.DisplayPort;
import com.elotouch.library.EloPeripheralManager.ExtHubType;
import com.elotouch.library.EloPeripheralManager.EthernetLinkMode;
import com.elotouch.library.EloPeripheralManager.ExtLed;
import com.elotouch.library.EloPeripheralManager.DpConcurrencyMode;
import com.elotouch.library.EloPeripheralManager.UsbPort;

import com.elotouch.library.EloPeripheralManager.ExtUsbPort;
import com.elotouch.payment.register2.cd.CashDrawer;

import android.os.Process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Date;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "SDK_API_Demo";

    private static final String HWMODEL = EloBuild.HWMODEL;
    private static final String MODEL_I4_STD_10 = EloBuild.MODEL_POS_I40_IN10_STD;
    private static final String MODEL_I4_STD_15 = EloBuild.MODEL_POS_I40_IN15_STD;
    private static final String MODEL_I4_STD_22 = EloBuild.MODEL_POS_I40_IN22_STD;
    private static final String MODEL_I4_PUCK =   EloBuild.MODEL_POS_I40_BPACK;
    private static final String MODEL_I4_TYPEC =  EloBuild.MODEL_POS_I40_IN15_TYPEC;
    /**
     * Wifi band values
     */
    private static final int WIFI_BAND_AUTO = 0;
    private static final int WIFI_BAND_5GHZ = 1;
    private static final int WIFI_BAND_2GHZ = 2;

    private static final String[] HEAP_SIZES = {"256m", "512m", "1024m"};
    private static final String[] HEAP_GROWTH_LIMITS = {"96m", "192m", "256m", "384m"};

    private static final int GPIO_VALUE_HIGH = EloPeripheralManager.GPIO_HIGH;
    private static final int GPIO_VALUE_LOW =  EloPeripheralManager.GPIO_LOW;

    private static final int MSG_IDLEMODE_INACTIVE_TRIGGER = 100;
    private static final int MSG_DISABLE_IDLE_MODE_TEMPORARY = 200;
    private static final int MSG_SET_ETH_LINK_MODE = 300;
    private static final int MSG_REFRESH_ETH_SPEED = 400;
    private static final int MSG_SET_SLK_POWER_MODE = 500;
    private static final int MSG_SET_USB_DP_MODE_DONE = 600;

    private static final int INDEX_SET_SLK_OFF = 0;
    private static final int INDEX_SET_SLK_GREEN_ON = 1;
    private static final int INDEX_SET_SLK_RED_ON = 2;
    private static final String PATH_BRIGHTNESS_SET = "/data/vendor/misc/backlight/brightness_set";
    private static final String PATH_BRIGHTNESS_TIMER_ENABLE = "/data/vendor/misc/backlight/brightness_timer_enable";
    private static final String PATH_BRIGHTNESS_TIMER_SET = "/data/vendor/misc/backlight/brightness_timer_set";

    private EthernetProxyDialog mEthProxyDialog = null;
    private AlertDialog mAlertDialog = null;
    private Button mBtnSetBrightness, mBtnEnableBrightnessTimer, mBtnSetBrightnessTimer, mBtnResetToDefault;
    private Button mBtnDisableScreenTimeout;
    private TextView mTxtBrightness, mTxtBrightnessTimerSet;
    private Button mBtnExtGPIO;
    private Button mBtnUart;
    private Button mUsbDataRoleBtn;
    private TextView mTxtViewExtGPIO_in_1, mTxtViewExtGPIO_in_2;
    private Dialog mWifiDialog;
    private Switch mWifiDfsSwitch;
    private Spinner mWifiBandSpinner;
    private TextView mTxtHeapSize, mTxtHeapGrowthLimit;
    private Button mEthernetProxySettings;
    private Spinner mEthLinkModeSpinner;
    private Spinner mExtLedSpinner;
    private TextView mEthSpeedDuplex;
    private Spinner mUsbDpModeSpinner;
    private Button mBtnPoeSettings;
    private TextView mTxtViewPoe;
    private Switch mTouchThroughModeSwitch;

    private TextView mDisplayPortStateView;
    private Spinner mDisplayPortSpinner;
    private Spinner mScreenDensitySpinner;
    private Button mCdGpioButton;
    private TextView mCdVoltageView;
    private Button mCfdBrightnessButton;
    private Button mCfdSwitcherButton;
    private Resources mRes;
    private int mClickTimes_heap_1 = 0, mClickTimes_heap_2 = 0;

    private Context mContext;
    private EloPeripheralManager mEloManager;
    private WifiManager mWifiManager;
    private EthernetLinkMode mEthLinkModeCtl;
    private ExtLed mExtLedCtrl;
    private ExtUsbPort mExtUsbPortCtl;
    private CashDrawer mCD;

    private boolean mGMSRestricted = false;
    private boolean mTouchThroughModeEnabled = false;
    private Message mMsgUSBHubError = null;
    private int[] mExtGPIOValueList = {0, 0, 0};
    private Switch mGMSRestrictSwitch;
    private EloPeripheralManager.DisplayPort mPrimaryPort;
    private DisplayPort mActivePort;
    private List<DisplayPort> mAvailablePorts = null;

    private boolean isPaused = false;
    private CDVoltage mCdVoltage = CDVoltage.V12;
    private boolean bExtTouchOn = false;
    private boolean mIsUsbDeviceRole = false;
    private boolean mWifiDfsOn = false;
    private int mWifiBandMode = WIFI_BAND_AUTO;
    private int mUsbDpMode = DpConcurrencyMode.HS_USB_AND_DP_4LANES.getMode();
    private int mEthLinkMode = EthernetLinkMode.ETH_LINK_MODE_UNKNOWN;

    private int mDimLevel = 0;
    private Date oldTime, newTime;
    private boolean mBrightnessTimerEnabled = true;
    private boolean mOldBrightnessTimerEnabled;
    private String mOldBrightnessTimerSet;
    private boolean mScreenTimeoutDisabled = false;
    
    private int mScreenDensity = EloPeripheralManager.DEFAULT_LCD_DENSITY;
    private Switch mUsbDebugModeSwitch;
    private boolean mUsbDebugModeEnabled = false;

    private Handler customHandler = new Handler();
    private Runnable mCdCheckThread = new Runnable() {
        public void run() {
            if (mCD.isDrawerOpen()) {
                mCdGpioButton.setBackgroundColor(Color.RED);
            } else {
                mCdGpioButton.setBackgroundColor(Color.GREEN);
            }

            if (!isPaused)
                customHandler.postDelayed(mCdCheckThread, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRes = getResources();

        mContext = this;
        mEloManager = new EloPeripheralManager(this, new PeripheralEventListener());
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mTxtHeapSize = (TextView) findViewById(R.id.txt_heap_size);
        mTxtHeapGrowthLimit = (TextView) findViewById(R.id.txt_heap_growth_limit);
        mEthLinkModeCtl = mEloManager.geEthernetLinkModeCtl();
        mExtUsbPortCtl = mEloManager.getExtUsbPortCtl();
        mBtnUart = (Button) findViewById(R.id.button_uart);
        // If current model support GPIO port,enable the external GPIO UI
        if (mEloManager.hasHardwareFeature(EloPeripheralManager.FEATRUE_GPIO)) {
            mBtnExtGPIO = (Button) findViewById(R.id.btnGPIO);
            mTxtViewExtGPIO_in_1 = (TextView) findViewById(R.id.txtGPIO_1);
            mTxtViewExtGPIO_in_2 = (TextView) findViewById(R.id.txtGPIO_2);
        } else {
            findViewById(R.id.contGPIO).setVisibility(View.GONE);
            mBtnExtGPIO = null;
            mTxtViewExtGPIO_in_1 = null;
            mTxtViewExtGPIO_in_2 = null;
        }

        mCD = new CashDrawer(this);
        mCdGpioButton = (Button) findViewById(R.id.button_cd_status_di1);
        mCdVoltageView = (TextView) findViewById(R.id.txt_cd_voltage);

        // If current model supports micro-USB SLK or GPIO SLK, enable SLK UI
        mExtLedCtrl = mEloManager.getExtLedCtl();
        Log.d(MainActivity.class.getSimpleName(), String.valueOf(mExtLedCtrl) + "got");
        if (supportExternalLed() && mExtLedCtrl.isEnabled()) {
            mExtLedSpinner = ((Spinner) findViewById(R.id.external_led_mode_list));
        } else {
            findViewById(R.id.external_led_setting).setVisibility(View.GONE);
            mExtLedSpinner = null;
        }

        if (supportManualConfiureUsbpd()) {
            mUsbDpModeSpinner = ((Spinner) findViewById(R.id.usb_dp_mode_list));
        } else {
            mUsbDpModeSpinner = null;
            findViewById(R.id.usb_dp_mode_settings).setVisibility(View.GONE);
        }

        mUsbDataRoleBtn = (Button) findViewById(R.id.button_usb_role);
        if (mUsbDataRoleBtn != null) {
            if (!mEloManager.hasHardwareFeature(EloPeripheralManager.FEATRUE_ADB_SWITCH)) {
                mUsbDataRoleBtn.setVisibility(View.GONE);
                mUsbDataRoleBtn = null;
            } else {
                mIsUsbDeviceRole = mEloManager.isUsbDeviceRole();
                mUsbDataRoleBtn.setText(mIsUsbDeviceRole ? R.string.usb_role_device : R.string.usb_role_host);
            }
        }

        mTxtViewPoe = (TextView) findViewById(R.id.power_resouce);
        if (!mEloManager.hasHardwareFeature(EloPeripheralManager.FEATRUE_POE)) {
            findViewById(R.id.poe_settings).setVisibility(View.GONE);
            mBtnPoeSettings = null;
        } else {
            mBtnPoeSettings = (Button) findViewById(R.id.poe_settings);
        }

        if (supportMultiDisplayPorts()) {
            mDisplayPortStateView = (TextView) findViewById(R.id.display_port_setting_summary);
            mDisplayPortSpinner = ((Spinner) findViewById(R.id.display_port_setting_list));
        } else {
            findViewById(R.id.disply_port_setting).setVisibility(View.GONE);
            mDisplayPortStateView = null;
            mDisplayPortSpinner = null;
        }

        mCfdSwitcherButton = (Button) findViewById(R.id.button_cfd_switcher);
        mCfdBrightnessButton = (Button) findViewById(R.id.cfd_brightness_button);

        setListeners();

        // Register for misc other intent broadcasts.
        IntentFilter intentFilter =
                new IntentFilter(EloPeripheralManager.WIFI_DFS_STATE_CHANGED_ACTION);
        intentFilter.addAction(EloPeripheralManager.WIFI_BAND_SETTING_CHANGED_ACTION);
        intentFilter.addAction(ExtUsbPort.ACTION_EXT_USB_HUB_STATE);
        intentFilter.addAction(EloPeripheralManager.ACTION_POE_CHANGED);
        intentFilter.addAction(EloPeripheralManager.ACTION_DISPLAY_PORT_CHANGED);
        registerReceiver(mIntentReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEloManager.OnResume();
        isPaused = false;
        updateUI();

        View cdGroupView = findViewById(R.id.container_cash_drawer);
        if (!hasCdPort()) {
            cdGroupView.setVisibility(View.GONE);
        } else {
            cdGroupView.setVisibility(View.VISIBLE);
            customHandler.post(mCdCheckThread);
            mCdVoltage = mCD.getCashDrawerVoltage();
            if (mCdVoltage == CDVoltage.V12) {
                mCdVoltageView.setText("12V");
            } else {
                mCdVoltageView.setText("24V");
            }
        }

        if (mWifiDfsSwitch != null) {
            mWifiDfsOn = mEloManager.isWifiDfsEnabled();
            mWifiDfsSwitch.setChecked(mWifiDfsOn);
        }

        if (mWifiBandSpinner != null) {
            mWifiBandMode = mEloManager.getWifiFrequencyBand();
            mWifiBandSpinner.setSelection(mWifiBandMode);
        }

        mEthLinkModeSpinner = ((Spinner) findViewById(R.id.ethernet_link_mode_list));
        if (isEthernetOn() && mEthLinkModeSpinner != null) {
            findViewById(R.id.ethernet_link_mode_setting).setVisibility(View.VISIBLE);
            mEthLinkModeSpinner.setOnItemSelectedListener(mEthLinkModeItemSelectedListener);
            mEthLinkMode = mEthLinkModeCtl.getLinkMode();
            mEthLinkModeSpinner.setSelection(mEthLinkMode, true);
            mEthSpeedDuplex = (TextView) findViewById(R.id.ethernet_current_speed_duplex);
            if (mEthSpeedDuplex != null) {
                mEthSpeedDuplex.setText(getEthernetSpeedDuplexSummary());
            }
        } else {
            findViewById(R.id.ethernet_link_mode_setting).setVisibility(View.GONE);
            mEthLinkModeSpinner = null;
            mEthSpeedDuplex = null;
        }

        mEthernetProxySettings = (Button) findViewById(R.id.ethernet_proxy_settings_button);
        List<String> ethList = EthernetProxyDialog.getEthernetDeviceList();
        if (ethList == null || ethList.size() == 0) {
            mEthernetProxySettings.setVisibility(View.GONE);
            mEthernetProxySettings = null;
        } else if (mEthernetProxySettings != null){
            mEthernetProxySettings.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    mEthProxyDialog = new EthernetProxyDialog(mContext);
                    mEthProxyDialog.show();
                }
            });
        }
                
        if (mCfdBrightnessButton != null) {
            if (hasCFD()) {
                mCfdBrightnessButton.setVisibility(View.VISIBLE);
            } else {
                mCfdBrightnessButton.setVisibility(View.GONE);
            }
        }
        
        if (mExtLedCtrl != null && mExtLedSpinner != null) {
            mExtLedSpinner.setOnItemSelectedListener(mExtLedModeItemSelectedListener);
            mExtLedSpinner.setSelection(0, true);
        }

        if (mUsbDpModeSpinner != null) {
            mUsbDpMode = mEloManager.getUsbDpConcurrencyMode().getMode();
            mUsbDpModeSpinner.setSelection(mUsbDpMode);
        }

        Button extUsbPortDemo = (Button) findViewById(R.id.btn_ext_usb_port_demo);
        if (extUsbPortDemo != null) {
            int[] extHubList = mExtUsbPortCtl.getAvailableExtHubList();
            if (extHubList != null && extHubList.length > 0) {
                extUsbPortDemo.setVisibility(View.VISIBLE);
            } else {
                // If there is not any external HUB attached, hide UI
                extUsbPortDemo.setVisibility(View.GONE);
            }
        }
        
        // Add debug code about DHCP server host name
        Log.i(TAG, " DHCP server hostname:" + mEloManager.getDhcpServerName());
    }

    private void updateUI() {
        updateDisplayPortUI();

        if (mEloManager.isCfdEnabled()) {
            mCfdSwitcherButton.setText(R.string.cfd_backlight_off);
        } else {
            mCfdSwitcherButton.setText(R.string.cfd_backlight_on);
        }

        if (mEloManager.isUartOn()) {
            mBtnUart.setText(R.string.uart_off);
        } else {
            mBtnUart.setText(R.string.uart_on);
        }

        if (mBtnExtGPIO != null) {
            for (int i = 0; i < 3; i++) {
                mExtGPIOValueList[i] = mEloManager.getGpio(mEloManager.getGpioInterafces()[i]);
                Log.i(TAG, " The GPIO [" + i + "]  value of GPIO port" + mExtGPIOValueList[i]);
            }

            if (mExtGPIOValueList[0] == GPIO_VALUE_HIGH)
                mBtnExtGPIO.setText(R.string.pull_gpio_low);
            else
                mBtnExtGPIO.setText(R.string.pull_gpio_high);

            if (mExtGPIOValueList[1] == GPIO_VALUE_HIGH) {
                mTxtViewExtGPIO_in_1.setText(mRes.getText(R.string.gpio_in_1)
                        + " " + mRes.getText(R.string.gpio_value_high));
            } else {
                mTxtViewExtGPIO_in_1.setText(mRes.getText(R.string.gpio_in_1)
                        + " " + mRes.getText(R.string.gpio_value_low));
            }

            if (mExtGPIOValueList[2] == GPIO_VALUE_HIGH) {
                mTxtViewExtGPIO_in_2.setText(mRes.getText(R.string.gpio_in_2)
                        + " " + mRes.getText(R.string.gpio_value_high));
            } else {
                mTxtViewExtGPIO_in_2.setText(mRes.getText(R.string.gpio_in_2)
                        + " " + mRes.getText(R.string.gpio_value_low));
            }
        }

        try {
            mTxtHeapSize.setText(mEloManager.getHeapSize());
            mTxtHeapGrowthLimit.setText(mEloManager.getHeapGrowthLimit());
        } catch (NoSuchMethodError err) {
            Log.e(TAG, "NoSuchMethodError for heap API !!!");
        }

        if (mTxtViewPoe != null) {
            mTxtViewPoe.setText(mEloManager.isPoeOn() ? R.string.power_source_poe
                    : R.string.power_source_ac);
        }
    }

    private void setListeners() {
        final Button btnTouch, btnTTYUSB;

        btnTouch = (Button) findViewById(R.id.btnTouch);
        btnTouch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(TAG, "enableExtTouch: " + bExtTouchOn);
                mEloManager.enableExtTouch(bExtTouchOn);
                if (bExtTouchOn) {
                    bExtTouchOn = false;
                    btnTouch.setText(R.string.disable_ext_touch);
                } else {
                    bExtTouchOn = true;
                    btnTouch.setText(R.string.enable_ext_touch);
                }
            }
        });

        btnTTYUSB = (Button) findViewById(R.id.btnTTYUSB);
        btnTTYUSB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                boolean testDevExist = false;
                String[] devList = mEloManager.ttyUSB_getDevicesList();
                if (devList != null && devList.length > 0) {
                    for (String dev : devList) {
                        Log.i(TAG, "ttyUSB dev:" + dev);
                        if (dev.equals("/dev/ttyUSB10")) {
                            testDevExist = true;
                        }
                    }
                } else {
                    Log.i(TAG, "empty ttyUSB dev list:");
                }
                Log.i(TAG, "Test ttyUSB device - ttyUSB10, testDevExist:" + testDevExist);
                ttyUSB_test("/dev/ttyUSB10", 9600,
                        EloPeripheralManager.SERIAL_FLAG_CS7
                                | EloPeripheralManager.SERIAL_FLAG_STOP_1
                                | EloPeripheralManager.SERIAL_FLAG_P_EVEN);
            }
        });

        Button openCdButton = (Button) findViewById(R.id.button_open_cd);
        Button cdVolButton = (Button) findViewById(R.id.button_cd_voltage);
        if (openCdButton != null) {
            if (mCD != null) {
                openCdButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Log.i(TAG, "Test CD - Open");
                        mCD.openCashDrawer();
                    }
                });
            } else {
                openCdButton.setVisibility(View.GONE);
            }
        }
        if (cdVolButton != null) {
            if (mCD != null) {
                cdVolButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Log.i(TAG, "change CD Voltage");
                        if (mCdVoltage == CDVoltage.V12) {
                            mCD.setCashDraweVoltage(CDVoltage.V24);
                        } else {
                            mCD.setCashDraweVoltage(CDVoltage.V12);
                        }
                        mCdVoltage = mCD.getCashDrawerVoltage();
                        if (mCdVoltage == CDVoltage.V12) {
                            mCdVoltageView.setText("12V");
                        } else {
                            mCdVoltageView.setText("24V");
                        }
                    }
                });
            } else {
                cdVolButton.setVisibility(View.GONE);
            }
        }

        if (mBtnExtGPIO != null) {
            mBtnExtGPIO.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (mExtGPIOValueList[0] == GPIO_VALUE_HIGH) {
                        Log.i(TAG, "Pull GPIO ext low");
                        mExtGPIOValueList[0] = GPIO_VALUE_LOW;
                        mEloManager.pullGpioLow();
                        mBtnExtGPIO.setText(R.string.pull_gpio_high);
                    } else {
                        Log.i(TAG, "Pull GPIO ext high");
                        mExtGPIOValueList[0] = GPIO_VALUE_HIGH;
                        mEloManager.pullGpioHigh();
                        mBtnExtGPIO.setText(R.string.pull_gpio_low);
                    }
                }
            });
        }

        mBtnUart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mEloManager.isUartOn()) {
                    Log.i(TAG, "Turn UART Off");
                    mEloManager.switchUart(false);
                    mBtnUart.setText(R.string.uart_on);
                } else {
                    Log.i(TAG, "Turn UART On");
                    mEloManager.switchUart(true);
                    mBtnUart.setText(R.string.uart_off);
                }
            }
        });

        if (mUsbDataRoleBtn != null) {
            mUsbDataRoleBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    mEloManager.switchUsbDataRole(!mIsUsbDeviceRole);
                    mIsUsbDeviceRole = !mIsUsbDeviceRole;
                    mUsbDataRoleBtn.setText(mIsUsbDeviceRole ? R.string.usb_role_device : R.string.usb_role_host);
                }
            });
        }

        mTxtBrightness = (TextView) findViewById(R.id.txt_brightness);
        int currentBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        mTxtBrightness.setText(Integer.toString(currentBrightness * 100 / 255));
        if (currentBrightness == 204) { //already 80, then dim to 60 when first click
            mDimLevel = 1;
        }
        mBtnSetBrightness = (Button) findViewById(R.id.btn_set_brightness);
        mBtnSetBrightness.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int brightness = 80 - mDimLevel * 20;
                Log.i(TAG, "Click to set brightness: " + brightness);
                mEloManager.setBrightness(brightness);
                mTxtBrightness.setText(Integer.toString(brightness));
                mDimLevel++;
                if (mDimLevel > 4) {
                    mDimLevel = 0;
                }
            }
        });

        mBtnEnableBrightnessTimer = (Button) findViewById(R.id.btn_brightness_timer);
        mBrightnessTimerEnabled = 1 == mEloManager.getBrightnessTimerEnableStatus();
        mOldBrightnessTimerEnabled = mBrightnessTimerEnabled;
        if (mBrightnessTimerEnabled) {
            mBtnEnableBrightnessTimer.setText(R.string.disable_brightness_timer);
        } else {
            mBtnEnableBrightnessTimer.setText(R.string.enable_brightness_timer);
        }
        mBtnEnableBrightnessTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mBrightnessTimerEnabled) {
                    Log.i(TAG, "Disable brightness timer feature");
                    mEloManager.enableBrightnessTimer(false);
                    mBtnEnableBrightnessTimer.setText(R.string.enable_brightness_timer);
                    mBrightnessTimerEnabled = false;
                } else {
                    Log.i(TAG, "Enable brightness timer feature");
                    mEloManager.enableBrightnessTimer(true);
                    mBtnEnableBrightnessTimer.setText(R.string.disable_brightness_timer);
                    mBrightnessTimerEnabled = true;
                }
            }
        });

        mTxtBrightnessTimerSet = (TextView) findViewById(R.id.txt_brightness_timer);
        mOldBrightnessTimerSet = getBrightnessTimerSetContent();
        mTxtBrightnessTimerSet.setText(mOldBrightnessTimerSet);
        mBtnSetBrightnessTimer = (Button) findViewById(R.id.btn_set_brightness_timer);
        mBtnSetBrightnessTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Calendar cal = Calendar.getInstance();
                int start_hr = cal.get(Calendar.HOUR_OF_DAY);
                int start_min = cal.get(Calendar.MINUTE);
                int end_hr = start_hr;
                int end_min = start_min + 2;
                if (start_min > 57) {
                    end_min -= 60;
                    end_hr += 1;
                }
                mEloManager.setBrightnessTimer(start_hr, start_min, end_hr, end_min, 40);
                mTxtBrightnessTimerSet.setText(start_hr + " " + start_min + " " + end_hr + " " + end_min + " 40");
                Log.i(TAG, "Click to set brightness timer: start_hr " + start_hr + "; start_min: " +
                        start_min + "; end_hr: " + end_hr + "; end_min: " + end_min + "; brightness: 40");
            }
        });

        mBtnResetToDefault = (Button) findViewById(R.id.btn_reset_default);
        mBtnResetToDefault.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(TAG, "Reset to default");
                mEloManager.enableBrightnessTimer(true);
                mBrightnessTimerEnabled = true;
                mBtnEnableBrightnessTimer.setText(R.string.disable_brightness_timer);
                mEloManager.setBrightnessTimer(0, 0, 6, 0, 0);
                mTxtBrightnessTimerSet.setText("0 0 6 0 0");
                mEloManager.disableScreenTimeout(false);
                mScreenTimeoutDisabled = false;
                mBtnDisableScreenTimeout.setText(R.string.disable_screen_timeout);
            }
        });

        mBtnDisableScreenTimeout = (Button) findViewById(R.id.btn_disable_screen_timeout);
        mBtnDisableScreenTimeout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mScreenTimeoutDisabled) {
                    Log.i(TAG, "Enable screen timeout");
                    mEloManager.disableScreenTimeout(false);
                    mBtnDisableScreenTimeout.setText(R.string.disable_screen_timeout);
                    mScreenTimeoutDisabled = false;
                } else {
                    Log.i(TAG, "Disable screen timeout");
                    mEloManager.disableScreenTimeout(true);
                    mBtnDisableScreenTimeout.setText(R.string.enable_screen_timeout);
                    mScreenTimeoutDisabled = true;
                }
            }
        });

        Button btnIdleMode = (Button) findViewById(R.id.idle_mode_button);
        btnIdleMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(TAG, "Test active & deactive idle mode ");
                mEloManager.activeIdleMode();
                mHandler.sendEmptyMessageDelayed(MSG_IDLEMODE_INACTIVE_TRIGGER, 5 * 1000);
            }
        });

        if(mCfdSwitcherButton != null) {
            mCfdSwitcherButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (mEloManager.isCfdEnabled()) {
                        Log.i(TAG, "Turn CFD Off");
                        mEloManager.enableCfd(false);
                        mCfdSwitcherButton.setText(R.string.cfd_backlight_on);
                    } else {
                        Log.i(TAG, "Turn CFD On");
                        mEloManager.enableCfd(true);
                        mCfdSwitcherButton.setText(R.string.cfd_backlight_off);
                    }
                }
            });
        }

        if (mCfdBrightnessButton != null) {
            mCfdBrightnessButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(new ComponentName("com.elotouch.sdk_api.demo",
                            "com.elotouch.sdk_api.demo.CfdBrightnessSettingsActivity"));
                    startActivity(intent);
                }
            });
        }
        
        Button ledLightsDemo = (Button) findViewById(R.id.btn_led_lights_demo);
        if (ledLightsDemo != null) {
            ledLightsDemo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(new ComponentName("com.elotouch.sdk_api.demo",
                            "com.elotouch.sdk_api.demo.LightsLedDemoActivity"));
                    startActivity(intent);
                }
            });
        }

        Button usbPortDemo = (Button) findViewById(R.id.btn_usb_port_demo);
        if (usbPortDemo != null) {
            if (supportControlUsbPortOnMainBoard()) {
                usbPortDemo.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setComponent(new ComponentName("com.elotouch.sdk_api.demo",
                                "com.elotouch.sdk_api.demo.UsbPortSettings"));
                        startActivity(intent);
                    }
                });
            } else {
                // if current model doesn't support enable/disable the USB ports on the main board, hide UI
                usbPortDemo.setVisibility(View.GONE);
            }
        }
        Button extUsbPortDemo = (Button) findViewById(R.id.btn_ext_usb_port_demo);
        if (extUsbPortDemo != null) {
            extUsbPortDemo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(new ComponentName("com.elotouch.sdk_api.demo",
                            "com.elotouch.sdk_api.demo.ExtUsbPortSettings"));
                    startActivity(intent);
                }
            });
        }

        if (mBtnPoeSettings != null) {
            mBtnPoeSettings.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(new ComponentName("com.elotouch.sdk_api.demo",
                            "com.elotouch.sdk_api.demo.PoeSettingsActivity"));
                    startActivity(intent);
                }
            });
        }

        Button bcrDemo = (Button) findViewById(R.id.btn_bcr_demo);
        if (bcrDemo != null) {
            if (!mEloManager.hasHardwareFeature(EloPeripheralManager.FEATRUE_ZEBRA_BCR)) {
                bcrDemo.setVisibility(View.GONE);
            } else {
                bcrDemo.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setComponent(new ComponentName("com.elotouch.sdk_api.demo",
                                "com.elotouch.sdk_api.demo.BCRDemoActivity"));
                        startActivity(intent);
                    }
                });
            }
        }

        mWifiDfsSwitch = (Switch) findViewById(R.id.wifi_dfs_switch);
        if (mWifiDfsSwitch != null) {
            if (supportDfsUi()) {
                mWifiDfsSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean on = mWifiDfsSwitch.isChecked();
                        if (on != mWifiDfsOn) {
                            mWifiDfsOn = mWifiDfsOn;
                            mEloManager.setWifiDfsEnabled(on);
                            // re-start wifi to make new band to take effect
                            if (mWifiManager.isWifiEnabled()) {
                                Log.d(TAG, "restart wifi for setting to take affect");
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    showWifiRestartConfirmDialog();
                                } else {
                                    mWifiManager.setWifiEnabled(false);
                                    SystemClock.sleep(50);
                                    mWifiManager.setWifiEnabled(true);
                                }
                            }
                        }
                    }
                });
            } else {
                mWifiDfsSwitch.setVisibility(View.GONE);
                mWifiDfsSwitch = null;
            }
        }

        mWifiBandSpinner = (Spinner) findViewById(R.id.wifi_band_list);
        if (mWifiBandSpinner != null) {
            mWifiBandSpinner.setOnItemSelectedListener(mWifiBandItemSelectedListener);
        }

        if (mUsbDpModeSpinner != null) {
            mUsbDpModeSpinner.setOnItemSelectedListener(mUsbDpModeItemSelectedListener);
        }

        if (mDisplayPortSpinner != null) {
            mDisplayPortSpinner.setOnItemSelectedListener(mDisplayPortItemSelectedListener);
        }

        Button btnSetProfile1 = (Button) findViewById(R.id.btn_profile1);
        btnSetProfile1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEloManager.setBatteryProfile(EloPeripheralManager.PROFILE_NORMAL);
            }
        });
        Button btnSetProfile2 = (Button) findViewById(R.id.btn_profile2);
        btnSetProfile2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEloManager.setBatteryProfile(EloPeripheralManager.PROFILE_SAVING_MODE);
            }
        });
        Button btnSetProfile3 = (Button) findViewById(R.id.btn_profile3);
        btnSetProfile3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEloManager.setBatteryProfile(EloPeripheralManager.PROFILE_ULTRA_SAVING_MODE);
            }
        });

        Button btnKeyRemap1 = (Button) findViewById(R.id.btn_key_remap1);
        btnKeyRemap1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEloManager.setKeyRemap(EloPeripheralManager.LEFT_TRIGGER_BUTTON, KeyEvent.KEYCODE_HOME)) {
                    Toast.makeText(mContext, R.string.key_remap_toast1, Toast.LENGTH_LONG).show();
                }
            }
        });

        Button btnKeyRemap2 = (Button) findViewById(R.id.btn_key_remap2);
        btnKeyRemap2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEloManager.setKeyRemap(EloPeripheralManager.RIGHT_TRIGGER_BUTTON, "com.android.settings")) {
                    Toast.makeText(mContext, R.string.key_remap_toast2, Toast.LENGTH_LONG).show();
                }
            }
        });

        Button btnKeyWakeup1 = (Button) findViewById(R.id.btn_key_wakeup1);
        btnKeyWakeup1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEloManager.setKeyWakeup(EloPeripheralManager.LEFT_TRIGGER_BUTTON, true)) {
                    Toast.makeText(mContext, R.string.key_wakeup_toast1, Toast.LENGTH_LONG).show();
                }
            }
        });

        Button btnKeyWakeup2 = (Button) findViewById(R.id.btn_key_wakeup2);
        btnKeyWakeup2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEloManager.setKeyWakeup(EloPeripheralManager.RIGHT_TRIGGER_BUTTON, true)) {
                    Toast.makeText(mContext, R.string.key_wakeup_toast2, Toast.LENGTH_LONG).show();
                }
            }
        });

        mGMSRestrictSwitch = (Switch) findViewById(R.id.gms_restricted_mode);
        if ("GMS".equals(mEloManager.getSysProperty("ro.vendor.build.oem.prjname"))) {
            mGMSRestricted = mEloManager.getGMSRestrictedMode();
            mGMSRestrictSwitch.setChecked(mGMSRestricted);
            mGMSRestrictSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showRebootConfirmDialog();
                }
            });
        } else {
            mGMSRestrictSwitch.setVisibility(View.GONE);
        }

        mTouchThroughModeSwitch = (Switch) findViewById(R.id.touch_through_mode);
        if (supportTouchThroughMode()) {
            mTouchThroughModeEnabled = Settings.System.getInt(getContentResolver(),
                    /*Settings.System.TOUCH_THROUGH_MODE*/"touch_through_mode", 0) == 1;
            mTouchThroughModeSwitch.setChecked(mTouchThroughModeEnabled);
            mTouchThroughModeSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mTouchThroughModeEnabled) {
                        mEloManager.setTouchThroughMode(false);
                        mTouchThroughModeEnabled = false;
                    } else {
                        mEloManager.setTouchThroughMode(true);
                        mTouchThroughModeEnabled = true;
                    }
                }
            });
        } else {
            mTouchThroughModeSwitch.setVisibility(View.GONE);
        }

        mScreenDensitySpinner = (Spinner) findViewById(R.id.screen_density_list);
        mScreenDensity = mEloManager.getLcdDensity();
        mScreenDensitySpinner.setSelection(mScreenDensity == 160 ? 0 : 1);
        mScreenDensitySpinner.setOnItemSelectedListener(mScreenDensityItemSelectedListener);

        mUsbDebugModeSwitch = (Switch) findViewById(R.id.usb_debug_mode);
        if (supportUsbDebugMode()) {
            mUsbDebugModeEnabled = Settings.Global.getInt(getContentResolver(),
                Settings.Global.ADB_ENABLED, 0) != 0;
            mUsbDebugModeSwitch.setChecked(mUsbDebugModeEnabled);
            mUsbDebugModeSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mUsbDebugModeEnabled) {
                        mEloManager.enableAdbDebugging(false);
                        mUsbDebugModeEnabled = false;
                    } else {
                        mEloManager.enableAdbDebugging(true);
                        mUsbDebugModeEnabled = true;
                    }
                }
            });
        } else {
            mUsbDebugModeSwitch.setVisibility(View.GONE);
        }
    }

    final Handler mHandler = new Handler(new Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_IDLEMODE_INACTIVE_TRIGGER) {
                mEloManager.disactiveIdleMode();
                return true;
            } else if (msg.what == MSG_SET_ETH_LINK_MODE) {
                mEthLinkMode = mEthLinkModeCtl.getLinkMode();
                mEthLinkModeSpinner.setSelection(mEthLinkMode, true);
                mHandler.sendEmptyMessageDelayed(MSG_REFRESH_ETH_SPEED, 2500);
                return true;
            } else if (msg.what == MSG_REFRESH_ETH_SPEED) {
                if (mEthSpeedDuplex != null) {
                    mEthSpeedDuplex.setText(getEthernetSpeedDuplexSummary());
                }
                return true;
            } else if (msg.what == MSG_SET_SLK_POWER_MODE) {
                int mode = msg.arg1;
                // mExtLedSpinner.setSelection(mode, true);
                return true;
            } else if (msg.what == MSG_SET_USB_DP_MODE_DONE) {
                Log.i(TAG, "Set USBPD USB mode DONE");
                mUsbDpMode = mEloManager.getUsbDpConcurrencyMode().getMode();
                mUsbDpModeSpinner.setSelection(mUsbDpMode);
            }
            return false;
        }
    });
    
    private void ttyUSB_test(final String devPath, final int baudrate, final int flags) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mEloManager.ttyUSB_getDevicesList();
                int fd = -1;
                byte[] data = {(byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45, (byte) 0x46, (byte) 0x47, (byte) 0x48, (byte) 0x49, (byte) 0x60, (byte) 0x61};
                byte[] dataOut = new byte[data.length];
                try {
                    fd = mEloManager.ttyUSB_open(devPath, baudrate, flags);
                    if (fd != -1) {
                        mEloManager.ttyUSB_write(fd, data, data.length);
                        int lenOut = mEloManager.ttyUSB_read(fd, dataOut);
                        Log.i(TAG, "ttyUSB_read, lenOut:" + lenOut + ", data.length: " + data.length);
                        if (lenOut != data.length)
                            Log.e(TAG, "ttyUSB device Exception - length unequal !!!");
                        for (int i = 0; i < lenOut; i++) {
                            Log.i(TAG, "ttyUSB_read, dataOut[" + i + "]: 0x" + Integer.toHexString(dataOut[i] & 0xFF));
                        }
                        mEloManager.ttyUSB_close(fd);
                    } else {
                        Log.i(TAG, "ttyUSB_open return -1!!!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ttyUSB device Exception - ttyUSB10 !!!");
                }
            }
        }).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        customHandler.removeCallbacks(mCdCheckThread);
        mEloManager.OnPause();
        Log.v("Status", "[KC] Pause");
        isPaused = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mIntentReceiver);
        mEloManager.enableBrightnessTimer(mOldBrightnessTimerEnabled);
        if (mOldBrightnessTimerSet != null) {
            String[] parameter = mOldBrightnessTimerSet.split(" ");
            if (parameter.length != 5) {
                Log.w(TAG, "old parameter length is not 5!");
                return;
            }
            try {
                int start_hr = Integer.valueOf(parameter[0]);
                int start_min = Integer.valueOf(parameter[1]);

                int end_hr = Integer.valueOf(parameter[2]);
                int end_min = Integer.valueOf(parameter[3]);
                int timerSetBrightness = Integer.valueOf(parameter[4]);
                mEloManager.setBrightnessTimer(start_hr, start_min, end_hr, end_min, timerSetBrightness);
            } catch (NumberFormatException e) {
                mEloManager.setBrightnessTimer(0, 0, 6, 0, 0);
            }
        }

        dismissDialogs();
    }

    public void onClick_heap(View arg0) {
        try {
            if (arg0.getId() == R.id.btn_heap_size) {
                if (mClickTimes_heap_1 >= HEAP_SIZES.length)
                    mClickTimes_heap_1 = 0;
                String size = HEAP_SIZES[mClickTimes_heap_1++];
                mEloManager.setHeapSize(size);
                mTxtHeapSize.setText(mEloManager.getHeapSize());
            } else if (arg0.getId() == R.id.btn_heap_growth_limit) {
                if (mClickTimes_heap_2 >= HEAP_GROWTH_LIMITS.length)
                    mClickTimes_heap_2 = 0;
                String limit = HEAP_GROWTH_LIMITS[mClickTimes_heap_2++];
                mEloManager.setHeapGrowthLimit(limit);
                mTxtHeapGrowthLimit.setText(mEloManager.getHeapGrowthLimit());
            }
        } catch (NoSuchMethodError err) {
            Log.e(TAG, "NoSuchMethodError for heap API !!!");
        }
    }

    private class PeripheralEventListener implements EloPeripheralEventListener {
        @Override
        public void onEvent(int state, String data) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onEvent, state = " + state + " data = " + data);
        }

        @Override
        public void onEvent(int state) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onEvent, state = " + state);
        }

        @Override
        public void onEvent(int pinNumber, int state) {
            // TODO Auto-generated method stub
            //Log.d(TAG, "onEvent, pinNumber = " + pinNumber + " state = " + state);
        }
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive:" + action);
            if (action.equals(EloPeripheralManager.WIFI_DFS_STATE_CHANGED_ACTION)) {
                if (mWifiDfsSwitch != null) {
                    boolean state = intent.getBooleanExtra(EloPeripheralManager.EXTRA_IS_WIFI_DFS_ENABLED, false);
                    Log.d(TAG, "WIFI_DFS_STATE_CHANGED_ACTION, extr-dfsOn:" + state + ", mWifiDfsOn:" + mWifiDfsOn);
                    if (mWifiDfsOn != state) {
                        mWifiDfsOn = state;
                        mWifiDfsSwitch.setChecked(mWifiDfsOn);
                    }
                }
            } else if (action.equals(EloPeripheralManager.WIFI_BAND_SETTING_CHANGED_ACTION)) {
                if (mWifiBandSpinner != null) {
                    int band = intent.getIntExtra(EloPeripheralManager.EXTRA_WIFI_BAND_SETTING, WIFI_BAND_AUTO);
                    Log.d(TAG, "WIFI_BAND_SETTING_CHANGED_ACTION, extr-band:" + band + ", mWifiBandMode:" + mWifiBandMode);
                    if (band != mWifiBandMode) {
                        mWifiBandMode = band;
                        mWifiBandSpinner.setSelection(mWifiBandMode);
                    }
                }
            } else if (action.equals((EloPeripheralManager.ACTION_EXT_USB_HUB_STATE))) {
                Log.d(TAG, "Receive external HUB connect/disconnect event");
                
                //show or hide CFD brightness button when attached/detach external USB hub.
                if (mCfdBrightnessButton != null) {
                    if (hasCFD()) {
                        mCfdBrightnessButton.setVisibility(View.VISIBLE);
                    } else {
                        mCfdBrightnessButton.setVisibility(View.GONE);
                    }
                }
                
                // show or hide external USB port UI when attached/detach external USB hub.
                Button extUsbPortDemo = (Button) findViewById(R.id.btn_ext_usb_port_demo);
                if (extUsbPortDemo != null) {
                    int[] extHubList = mExtUsbPortCtl.getAvailableExtHubList();
                    if (extHubList != null && extHubList.length > 0) {
                        extUsbPortDemo.setVisibility(View.VISIBLE);
                    } else {
                        // If there is not any external HUB attached, hide UI
                        extUsbPortDemo.setVisibility(View.GONE);
                    }
                }
                
                // Hide or show Cash drawer when external hub attach/detach
                if (!isPaused) {
                    View cdGroupView = findViewById(R.id.container_cash_drawer);
                    if (!hasCdPort()) {
                        cdGroupView.setVisibility(View.GONE);
                        customHandler.removeCallbacks(mCdCheckThread);
                    } else {
                        cdGroupView.setVisibility(View.VISIBLE);
                        customHandler.removeCallbacks(mCdCheckThread);
                        customHandler.post(mCdCheckThread);
                        mCdVoltage = mCD.getCashDrawerVoltage();
                        if (mCdVoltage == CDVoltage.V12) {
                            mCdVoltageView.setText("12V");
                        } else {
                            mCdVoltageView.setText("24V");
                        }
                    }
                }
            } else if (intent.getAction().equals((EloPeripheralManager.ACTION_POE_CHANGED))) {
                mTxtViewPoe.setText(mEloManager.isPoeOn() ? R.string.power_source_poe
                        : R.string.power_source_ac);
            } else if (intent.getAction().equals((EloPeripheralManager.ACTION_DISPLAY_PORT_CHANGED))) {
                updateDisplayPortUI();
            }
        }
    };

    AdapterView.OnItemSelectedListener mEthLinkModeItemSelectedListener =
            new AdapterView.OnItemSelectedListener() {

        public void onItemSelected(AdapterView parent, View v, int pos, long id) {
            if (mEthLinkMode != pos && pos >= 0
                    && pos <= EthernetLinkMode.ETH_LINK_MODE_MAX) {
                mEthLinkMode = pos;
                Message msg = mHandler.obtainMessage(MSG_SET_ETH_LINK_MODE, mEthLinkMode, 0, null);
                // make sure msg.replyTo is not null for across processes
                if (msg != null) {
                    msg.replyTo = new Messenger(msg.getTarget());
                }                
                mEthLinkModeCtl.setLinkMode(mEthLinkMode, msg);
            }
        }

        public void onNothingSelected(AdapterView parent) {
        }
    };

    AdapterView.OnItemSelectedListener mExtLedModeItemSelectedListener =
            new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView parent, View v, int pos, long id) {
                    int portMask = 0;
                    int statusBits = 0;
                    Log.i(TAG, "SLK spinner onItemSelected - " + pos);
                    if (pos >= 0 && pos <= INDEX_SET_SLK_RED_ON) {

                        portMask = getSlkPortMask();
                        if (pos != 0) {// turn on external LED
                            statusBits = portMask;
                        } else {// turn off external LED
                            statusBits = 0;
                        }

                        Message msg = mHandler.obtainMessage(MSG_SET_SLK_POWER_MODE, pos, pos, null);
                        // make sure msg.replyTo is not null for across processes
                        if (msg != null) {
                            msg.replyTo = new Messenger(msg.getTarget());
                        }
                        mExtLedCtrl.setPower(portMask, statusBits, msg);

                        if (pos == INDEX_SET_SLK_GREEN_ON) {
                            mExtLedCtrl.setColor(portMask, ExtLed.EXT_LED_COLOR_GREEN);
                        } else if (pos == INDEX_SET_SLK_RED_ON) {
                            mExtLedCtrl.setColor(portMask, ExtLed.EXT_LED_COLOR_RED);
                        }
                    }

                }

                public void onNothingSelected(AdapterView parent) {
                }
            };

    AdapterView.OnItemSelectedListener mWifiBandItemSelectedListener =
            new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView parent, View v, int pos, long id) {
                    String selectedItem = parent.getItemAtPosition(pos).toString();
                    String[] valueList = getResources().getStringArray(R.array.wifi_frequency_band_values);
                    int selectedMode = Integer.valueOf(valueList[pos]);
                    Log.d(TAG, "wifiBand-onItemSelected"
                            + ", selectedItem: " + selectedItem
                            + ", position:" + pos
                            + ", selectedMode:" + selectedItem);
                    if (mWifiBandMode != selectedMode) {
                        mWifiBandMode = selectedMode;
                        mEloManager.setWifiFrequencyBand(selectedMode);
                        // re-start wifi to make new band to take effect
                        if (mWifiManager.isWifiEnabled()) {
                            Log.d(TAG, "restart wifi for setting to take affect");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                showWifiRestartConfirmDialog();
                            } else {
                                mWifiManager.setWifiEnabled(false);
                                SystemClock.sleep(50);
                                mWifiManager.setWifiEnabled(true);
                            }
                        }
                    }
                }

                public void onNothingSelected(AdapterView parent) {
                }
            };

    AdapterView.OnItemSelectedListener mUsbDpModeItemSelectedListener =
            new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView parent, View v, int pos, long id) {
                    String selectedItem = parent.getItemAtPosition(pos).toString();
                    String[] valueList = getResources().getStringArray(R.array.usb_dp_concrurrency_values);
                    int selectedMode = Integer.valueOf(valueList[pos]);
                    Log.d(TAG, "UsbDpMode-onItemSelected"
                            + ", selectedItem: " + selectedItem
                            + ", position:" + pos
                            + ", selectedMode:" + selectedItem);
                    if (mUsbDpMode != selectedMode) {
                        mUsbDpMode = selectedMode;
                        DpConcurrencyMode enumMode = DpConcurrencyMode.fromInteger(selectedMode);
                        Message msg = mHandler.obtainMessage(MSG_SET_USB_DP_MODE_DONE, selectedMode, 0, null);
                        mEloManager.setUsbDpConcurrencyMode(enumMode, msg);
                    }
                }

                public void onNothingSelected(AdapterView parent) {
                }
            };

    AdapterView.OnItemSelectedListener mDisplayPortItemSelectedListener =
            new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView parent, View v, int pos, long id) {
                    String selectedItem = parent.getItemAtPosition(pos).toString();
                    String[] valueList = getResources().getStringArray(R.array.multi_display_port_config_values);
                    int selectedport = Integer.valueOf(valueList[pos]);
                    Log.d(TAG, "DisplayPort-onItemSelected"
                            + ", selectedItem: " + selectedItem
                            + ", position:" + pos
                            + ", selectedMode:" + selectedItem);
                    if (mPrimaryPort.getPortId() != selectedport) {
                        DisplayPort enumPort = DisplayPort.fromInteger(selectedport);
                        if (!mEloManager.setPrimaryDisplayPortId(enumPort)) {
                            Log.d(TAG, "fail setPrimaryDisplayPortId: " + selectedport);
                            mDisplayPortSpinner.setSelection(mPrimaryPort.getPortId() - 1);
                        } else {
                            updateDisplayPortUI();
                        }
                    }
                }

                public void onNothingSelected(AdapterView parent) {
                }
            };

    AdapterView.OnItemSelectedListener mScreenDensityItemSelectedListener =
            new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView parent, View v, int pos, long id) {
                    String selectedItem = parent.getItemAtPosition(pos).toString();
                    String[] valueList = getResources().getStringArray(R.array.screen_density_values);
                    int selectedDensity = Integer.valueOf(valueList[pos]);
                    Log.d(TAG, "ScreenDensity-onItemSelected"
                            + ", selectedItem: " + selectedItem
                            + ", position:" + pos
                            + ", selectedDensity:" + selectedDensity);
                    if (mScreenDensity != selectedDensity) {
                        mScreenDensity = selectedDensity;
                        showDensityRebootDialog();
                    }
                }

                public void onNothingSelected(AdapterView parent) {
                }
            };

    private void sendMessageToHandler(int msgID, Object information) {
        // TODO Auto-generated method stub
        Message message;
        message = mHandler.obtainMessage(msgID, information);
        mHandler.sendMessage(message);
    }

    private String getBrightnessTimerSetContent() {
        String content = mEloManager.getBrightnessTimerSetContent();
        Log.d(TAG, "getBrightnessTimerSetContent: " + content);
        //If content is null or not valid, return default
        if (content == null || content.split(" ").length != 5) {
            return "0 0 6 0 0";
        }
        return content;
    }

    private void updateDisplayPortUI() {
        if (mDisplayPortStateView == null || mDisplayPortSpinner == null) {
            return;
        }

        mPrimaryPort = mEloManager.getPrimaryDisplayPortId();
        mActivePort = mEloManager.getActiveDisplayPortId();
        mAvailablePorts = mEloManager.getAvailableDisplayPortList();

        mDisplayPortStateView.setText(buildDisplayPrefSummary());

        mDisplayPortSpinner.setSelection(mPrimaryPort.getPortId() - 1);
        if (mAvailablePorts.size() >= 2) {
            mDisplayPortSpinner.setEnabled(true);
        } else {
            mDisplayPortSpinner.setEnabled(false);
        }
    }

    private String buildDisplayPrefSummary() {
        String message = "";
        String primaryPortStr = "";
        String activePortStr = "";

        String[] summaries = getResources()
                .getStringArray(R.array.multi_display_port_config_choices);
        String[] portEntries = getResources()
                .getStringArray(R.array.multi_display_port_config_values);
        for (int i = 0; i < portEntries.length; i++) {
            if (mPrimaryPort.getPortId() == Integer.parseInt(portEntries[i])) {
                primaryPortStr = summaries[i];
                break;
            }
        }

        if (mActivePort != DisplayPort.UNKNOW) {
            // connect a monitor via HDMI or DP port
            for (int i = 0; i < portEntries.length; i++) {
                if (mActivePort.getPortId() == Integer.parseInt(portEntries[i])) {
                    activePortStr = summaries[i];
                    break;
                }
            }
        } else {
            // Backpack didn't connect to any external display device
            activePortStr = getString(R.string.display_port_none);
        }

        return getString(R.string.multi_display_port_config_summary, primaryPortStr,
                activePortStr);
    }

    private String readFromFile(String path) {
        Log.i(TAG, "Try to read file: " + path);
        try {
            InputStream instream = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
            String line = reader.readLine();
            if (line != null) {
                return line;
            }
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (PATH_BRIGHTNESS_SET.equals(path)) {
            return "80";
        } else if (PATH_BRIGHTNESS_TIMER_ENABLE.equals(path)) {
            return "on";
        } else {
            return "0 0 6 0 0";
        }
    }

    void showWifiRestartConfirmDialog() {
        dismissDialogs();

        mWifiDialog = new AlertDialog.Builder(this)
                .setTitle("Switch wifi for setting to take affect ?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                        startActivity(panelIntent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();

        mWifiDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface var1) {
                mWifiDialog = null;
            }
        });
    }

    private void dismissDialogs() {
        if (mWifiDialog != null) {
            mWifiDialog.dismiss();
            mWifiDialog = null;
        }
    }

    private boolean isSystemUid() {
        Log.i(TAG, "SDK Demo uid: " + Process.myUid());
        return Process.myUid() == Process.SYSTEM_UID;
    }

    private boolean hasDevicePowerPermission() {
        return getPackageManager().checkPermission("android.permission.DEVICE_POWER",
                getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isEthernetOn() {
        boolean available = mEthLinkModeCtl.isEthAvailable();
        Log.i(TAG, "isEthernetOn: " + available);
        return available;
    }

    private String getEthernetSpeedDuplexSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("Ethernet current speed: ");

        int speed = mEthLinkModeCtl.getLinkSpeed();
        builder.append(speed);
        String duplex = mEthLinkModeCtl.getLinkDuplex();
        builder.append(", duplex:" + duplex);
        builder.append(" (please plug-in ethernet cable to check these link parameters)");

        return builder.toString();
    }

    private int getSlkPortMask() {
        if (!supportExternalLed()) {
            return 0;
        }

        int slkPortMask = 1;
        if (HWMODEL.equals(MODEL_I4_STD_10)
                || HWMODEL.equals(MODEL_I4_STD_15)
                || HWMODEL.equals(MODEL_I4_STD_22)) {
            //TODO, sync with BSP to get the micro-USB port ID
            slkPortMask = ExtLed.MASK_USB_PORT2
                    | ExtLed.MASK_USB_PORT3
                    | ExtLed.MASK_USB_PORT4
                    | ExtLed.MASK_USB_PORT5;
        }

        if (mEloManager.hasHardwareFeature(EloPeripheralManager.FEATRUE_GPIO)) {
            slkPortMask = slkPortMask | ExtLed.MASK_PORT_GPIO;
        }

        return slkPortMask;
    }
    
    private boolean hasCFD() {
        ExtHubType hub = mExtUsbPortCtl.getConnectedExtHub();
        return hub == ExtHubType.POS_STAND_CFD;
    }
        
    private boolean hasCdPort() {
        ExtHubType hub = mExtUsbPortCtl.getConnectedExtHub();
        if (hub == ExtHubType.POS_STAND_CFD
                || hub == ExtHubType.POS_STAND_NO_CFD
                || hub == ExtHubType.FLIP_STAND) {
            return true;
        }
        return false;
    }

    private boolean supportExternalLed() {
        // Only i-series STD supports micro-USB SLK
        if (HWMODEL.contains(MODEL_I4_STD_10) ||
                HWMODEL.contains(MODEL_I4_STD_15) ||
                HWMODEL.contains(MODEL_I4_STD_22)) {
            return true;
        } else if (mEloManager.hasHardwareFeature(EloPeripheralManager.FEATRUE_GPIO)) {
            //If current model support GPIO SLK, return true
            return true;
        }

        return false;
    }

    private boolean supportManualConfiureUsbpd() {
        /**
         * Note: 
         * 1. SDA660's PD controller of type C USB port cannot switch DP and USB3.0
         * automatically, user should switch it manually 
         * 2. The external DP comes from DSI2DP Bridge:
         * 1) the default mode is 2-lane-DP + USB-SS
         * 2) If connect POS stand via external DP, user MAY use 4-land-DP or USB-SS via some user case,
         *    so will provide API to switch configure DP and USB
         */
        if (mEloManager.hasHardwareFeature(EloPeripheralManager.FEATRUE_EXTERNAL_DP)) {            
            if (hasPosStandNoCfdHub()) {
                return true;
            }

            if (hasFlipStandHub()) {
                return true;
            }

            return false;
        }

        return true;
    }

    private boolean hasPosStandNoCfdHub() {
        ExtHubType hub = mExtUsbPortCtl.getConnectedExtHub();
        if (hub == ExtHubType.POS_STAND_NO_CFD) {
            return true;
        }
        
        return false;
    }

    private boolean hasFlipStandHub() {
        ExtHubType hub = mExtUsbPortCtl.getConnectedExtHub();
        if (hub == ExtHubType.FLIP_STAND) {
            return true;
        }
        
        return false;
    }

    private boolean supportControlUsbPortOnMainBoard() {
        // I4 standard supports user enable/disable USB ports
        if (HWMODEL.equals(MODEL_I4_STD_10)
                || HWMODEL.equals(MODEL_I4_STD_15)
                || HWMODEL.equals(MODEL_I4_STD_22)) {
            return true;
        }

        // PUCK supports user enable/disable USB ports
        if (HWMODEL.equals(MODEL_I4_PUCK)) {
            //TODO, enable it when PUCK USB feature ready
            return true;
        }

        return false;
    }

    private boolean supportTouchThroughMode() {
        if (HWMODEL.contains(MODEL_I4_STD_10) ||
                HWMODEL.contains(MODEL_I4_STD_15) ||
                HWMODEL.contains(MODEL_I4_STD_22) ||
                HWMODEL.contains(MODEL_I4_TYPEC)) {
            return true;
        }

        return false;
    }

    private boolean supportDfsUi() {
        // Hide DFS settings UI for backpack?
        if (HWMODEL.equals(MODEL_I4_PUCK)) {
            return false;
        }

        return true;
    }

    private boolean supportMultiDisplayPorts() {
        // Only bckpack has multi external display ports.
        if (HWMODEL.equals(MODEL_I4_PUCK)) {
            return true;
        }

        return false;
    }

    private void showRebootConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        mAlertDialog = builder.setTitle(getString(R.string.gms_restrict_dialog_title))
                           .setMessage(getString(R.string.gms_restrict_dialog_message))
                           .setCancelable(false)
                           .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                 public void onClick(DialogInterface dialog, int which) {
                                     dialog.dismiss();
                                     mEloManager.enableGMSRestrictedMode(!mGMSRestricted);
                                 }
                           })
                           .create();
         mAlertDialog.show();
    }

    private void showDensityRebootDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        mAlertDialog = builder.setTitle(mContext.getString(R.string.screen_density_title))
                   .setMessage(mContext.getString(R.string.screen_density_dialog_message))
                   .setCancelable(false)
                   .setPositiveButton(mContext.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                         public void onClick(DialogInterface dialog, int which) {
                             dialog.dismiss();
                             mEloManager.setLcdDensity(mScreenDensity);
                         }
                   })
                   .create();
        mAlertDialog.show();
    }

    private boolean supportUsbDebugMode() {
        if (mEloManager.hasHardwareFeature(EloPeripheralManager.FEATRUE_PAYMENT)) {
            return false;
        }

        return true;
    }

}
