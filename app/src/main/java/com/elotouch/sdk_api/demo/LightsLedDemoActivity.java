package com.elotouch.sdk_api.demo;

import android.app.ActionBar;
import android.app.Activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.elotouch.library.EloPeripheralManager;
import com.elotouch.library.EloPeripheralManager.OemLights;

public class LightsLedDemoActivity extends Activity {
    private static final String TAG = "SDK_API_Demo_OemLights";

    private static final boolean ENABLE_HW_FLASH_UI = true;
    
    // Light UI mode
    private static final int LIGHT_UI_MODE_OFF = 0;
    private static final int LIGHT_UI_MODE_WHITE_ON = 1;
    private static final int LIGHT_UI_MODE_GREEN_ON = 2;
    private static final int LIGHT_UI_MODE_ORANGE_ON = 3;
    private static final int LIGHT_UI_MODE_WHITE_FLASH = 4;
    private static final int LIGHT_UI_MODE_GREEN_FLASH = 5;
    private static final int LIGHT_UI_MODE_ORANGE_FLASH = 6;

    // Light default flash timer
    private static final int LIGHT_FLASH_ONMS = 500;
    private static final int LIGHT_FLASH_OFFMS = 500;

    private Context mContext;
    private EloPeripheralManager mEloManager;
    private EloPeripheralManager.OemLights mOemLightsMgr = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lights_led_demo);

        mContext = this;
        mEloManager = new EloPeripheralManager(this, null);
        mOemLightsMgr = mEloManager.getOemLightsManager();
        updateUiVisableState();
        setListeners();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // turn on test flag to skip power led controller process in system_server
        mEloManager.setSysProperty("persist.sys.led.custom", "true");
    }

    @Override
    public void onPause() {
        super.onPause();
        //  off test flag for power led controller process in system_server
        mEloManager.setSysProperty("persist.sys.led.custom", "false");
    }

    @Override
    public void onStop() {
        super.onStop();
        //  off test flag for power led controller process in system_server
        mEloManager.setSysProperty("persist.sys.led.custom", "false");
    }

    private void updateUiVisableState() {
        // If customer LED0 not exist, remove the UI of customer LED0
        final LinearLayout led0Container = (LinearLayout) findViewById(
                R.id.customer_led_0_container);
        if (led0Container != null
                && mOemLightsMgr.lightExist(OemLights.LIGHT_ID_POWER) == false) {
            led0Container.setVisibility(View.GONE);
        }

        // If customer LED1 not exist, remove the UI of customer LED1
        final LinearLayout led1Container = (LinearLayout) findViewById(
                R.id.customer_led_1_container);
        if (led1Container != null
                && mOemLightsMgr.lightExist(OemLights.LIGHT_ID_FUNCTION) == false) {
            led1Container.setVisibility(View.GONE);
        }

        //
        final LinearLayout indicationLedContainer = (LinearLayout) findViewById(
                R.id.indication_led_container);
        if (indicationLedContainer != null
                && mOemLightsMgr.lightExist(OemLights.LIGHT_ID_INDCATION) == false) {
            indicationLedContainer.setVisibility(View.GONE);
        }
    }
    
    private void setListeners() {
        //Customer LED 0:
        final Button cusLed0Off = (Button) findViewById(R.id.btn_customer_led_0_off);
        if (cusLed0Off != null) {
            cusLed0Off.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "customer LED 0: OFF");
                    mOemLightsMgr.turnOff(OemLights.LIGHT_ID_POWER);
                }
            });
        }
        final Button cusLed0GreenOn = (Button) findViewById(R.id.btn_customer_led_0_green_on);
        if (cusLed0GreenOn != null) {
            cusLed0GreenOn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "customer LED 0: GREEN ON");
                    mOemLightsMgr.setColor(OemLights.LIGHT_ID_POWER, OemLights.ARGB_GREEN);
                }
            });
        }
        final Button cusLed0OrangeOn = (Button) findViewById(R.id.btn_customer_led_0_orange_on);
        if (cusLed0OrangeOn != null) {
            cusLed0OrangeOn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "customer LED 0: ORANGE ON");
                    mOemLightsMgr.setColor(OemLights.LIGHT_ID_POWER, OemLights.ARGB_ORANGE);
                }
            });
        }
        final Button custLed0GreenFlash = (Button) findViewById(R.id.btn_customer_led_0_green_flash);
        if (custLed0GreenFlash != null) {
            custLed0GreenFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "customer LED 0: GREEN FLASH");
                    mOemLightsMgr.setFlashing(OemLights.LIGHT_ID_POWER, OemLights.ARGB_GREEN,
                            OemLights.LIGHT_FLASH_TIMED, LIGHT_FLASH_ONMS, LIGHT_FLASH_OFFMS);
                }
            });
        }
        final Button cusLed0OrangeFlash = (Button) findViewById(R.id.btn_customer_led_0_orange_flash);
        if (cusLed0OrangeFlash != null) {
            cusLed0OrangeFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "customer LED 0: ORANGE FLASH");
                    mOemLightsMgr.setFlashing(OemLights.LIGHT_ID_POWER, OemLights.ARGB_ORANGE,
                            OemLights.LIGHT_FLASH_TIMED, LIGHT_FLASH_ONMS, LIGHT_FLASH_OFFMS);
                }
            });
        }
            
        final Button custLed0GreenHwFlash = (Button) findViewById(
                R.id.btn_customer_led_0_green_hardware_flash);
        if (custLed0GreenHwFlash != null) {
            if (ENABLE_HW_FLASH_UI) {
                custLed0GreenHwFlash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Log.i(TAG, "customer LED 0: GREEN HARDWARE FLASH");
                        mOemLightsMgr.pulse(OemLights.LIGHT_ID_POWER, OemLights.ARGB_GREEN, 10 * 1000);
                    }
                });
            } else {
                custLed0GreenHwFlash.setVisibility(View.GONE);
            }
        }
        
        final Button cusLed0OrangeHwFlash = (Button) findViewById(
                R.id.btn_customer_led_0_orange_hardware_flash);
        if (cusLed0OrangeHwFlash != null) {
            if (ENABLE_HW_FLASH_UI) {
                cusLed0OrangeHwFlash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Log.i(TAG, "customer LED 0: ORANGE HARDWARE FLASH");
                        mOemLightsMgr.pulse(OemLights.LIGHT_ID_POWER, OemLights.ARGB_ORANGE, 10 * 1000);
                    }
                });
            } else {
                cusLed0OrangeHwFlash.setVisibility(View.GONE);
            }
        }

        //Customer LED 1:
        final Button cusLed1Off = (Button) findViewById(R.id.btn_customer_led_1_off);
        if (cusLed1Off != null) {
            cusLed1Off.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "customer LED 1: OFF");
                    mOemLightsMgr.turnOff(OemLights.LIGHT_ID_FUNCTION);
                }
            });
        }
        final Button cusLed1RedOn = (Button) findViewById(R.id.btn_customer_led_1_red_on);
        if (cusLed1RedOn != null) {
            cusLed1RedOn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "customer LED 1: RED ON");
                    mOemLightsMgr.setColor(OemLights.LIGHT_ID_FUNCTION, OemLights.ARGB_RED);
                }
            });
        }
        final Button cusLed1BlueOn = (Button) findViewById(R.id.btn_customer_led_1_blue_on);
        if (cusLed1BlueOn != null) {
            cusLed1BlueOn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "customer LED 1: blue ON");
                    mOemLightsMgr.setColor(OemLights.LIGHT_ID_FUNCTION, OemLights.ARGB_BLUE);
                }
            });
        }
        final Button cusLed1RedFlash = (Button) findViewById(R.id.btn_customer_led_1_red_flash);
        if (cusLed1RedFlash != null) {
            cusLed1RedFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "customer LED 1: RED FLASH");
                    mOemLightsMgr.setFlashing(OemLights.LIGHT_ID_FUNCTION, OemLights.ARGB_RED,
                            OemLights.LIGHT_FLASH_TIMED, LIGHT_FLASH_ONMS, LIGHT_FLASH_OFFMS);
                }
            });
        }
        final Button cusLed1BlueFlash = (Button) findViewById(R.id.btn_customer_led_1_blue_flash);
        if (cusLed1BlueFlash != null) {
            cusLed1BlueFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "customer LED 1: BLUE FLASH");
                    mOemLightsMgr.setFlashing(OemLights.LIGHT_ID_FUNCTION, OemLights.ARGB_BLUE,
                            OemLights.LIGHT_FLASH_TIMED, LIGHT_FLASH_ONMS, LIGHT_FLASH_OFFMS);
                }
            });
        }

        //Indication LED:
        final Button indLedOff = (Button) findViewById(R.id.btn_indication_led_off);
        if (indLedOff != null) {
            indLedOff.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "Indication LED: OFF");
                    mOemLightsMgr.turnOff(OemLights.LIGHT_ID_INDCATION);
                }
            });
        }
        final Button indLedWhiteOn = (Button) findViewById(R.id.btn_indication_led_white_on);
        if (indLedWhiteOn != null) {
            indLedWhiteOn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "Indication LED: WHITE ON");
                    mOemLightsMgr.setColor(OemLights.LIGHT_ID_INDCATION, OemLights.ARGB_WHITE);
                }
            });
        }
        final Button indLedOrangeOn = (Button) findViewById(R.id.btn_indication_led_orange_on);
        if (indLedOrangeOn != null) {
            indLedOrangeOn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "Indication LED: ORANGE ON");
                    mOemLightsMgr.setColor(OemLights.LIGHT_ID_INDCATION, OemLights.ARGB_ORANGE);
                }
            });
        }
        final Button indLedWhiteFlash = (Button) findViewById(R.id.btn_indication_led_white_flash);
        if (indLedWhiteFlash != null) {
            indLedWhiteFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "Indication LED: WHITE FLASH");
                    mOemLightsMgr.setFlashing(OemLights.LIGHT_ID_INDCATION, OemLights.ARGB_WHITE,
                            OemLights.LIGHT_FLASH_TIMED, LIGHT_FLASH_ONMS, LIGHT_FLASH_OFFMS);
                }
            });
        }
        final Button indLedOrangeFlash = (Button) findViewById(R.id.btn_indication_led_orange_flash);
        if (indLedOrangeFlash != null) {
            indLedOrangeFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Log.i(TAG, "Indication LED: ORANGE FLASH");
                    mOemLightsMgr.setFlashing(OemLights.LIGHT_ID_INDCATION, OemLights.ARGB_ORANGE,
                            OemLights.LIGHT_FLASH_TIMED, LIGHT_FLASH_ONMS, LIGHT_FLASH_OFFMS);
                }
            });
        }
                
        final Button indLedWhiteHwFlash = (Button) findViewById(
                R.id.btn_indication_led_white_hardware_flash);
        if (indLedWhiteHwFlash != null) {
            if (ENABLE_HW_FLASH_UI) {
                indLedWhiteHwFlash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Log.i(TAG, "Indication LED: WHITE HARDWARE FLASH");
                        mOemLightsMgr.pulse(OemLights.LIGHT_ID_INDCATION, OemLights.ARGB_WHITE,
                                10 * 1000);
                    }
                });
            } else {
                indLedWhiteHwFlash.setVisibility(View.GONE);
            }
        }
    }
}
