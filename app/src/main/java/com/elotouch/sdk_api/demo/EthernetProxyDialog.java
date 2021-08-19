/*
 * Copyright (C) 2021 OEM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.elotouch.sdk_api.demo;

import android.view.View;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import android.app.AlertDialog;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.widget.LinearLayout;
import android.widget.Toast;
import android.text.TextUtils;
import android.util.Log;

import com.elotouch.library.EloPeripheralManager;
import com.elotouch.library.net.ProxySetting;

public class EthernetProxyDialog extends AlertDialog implements
        DialogInterface.OnClickListener,
        AdapterView.OnItemSelectedListener {
    private final static String TAG = "EthernetConfigDialog";
    private static final boolean DBG = true;
    
    private static final String mIfacePrefix = "eth";
    
    private Context mContext;
    private EloPeripheralManager mEloManager;
    private EthernetProxyInfo mProxyInfo;
    private List<String> mDevList;
    
    /* main view and iface list view */
    private View mMainView;
    private Spinner mDevSpinnerView;
        
    /* views for proxy setting */
    private View mPacGroup;
    private View mStaticProxyGroup;
    private Spinner mProxyTypeSpinner;
    private EditText mProxyPacUrlEdit;
    private EditText mProxyHostEdit;
    private EditText mProxyPortEdit;
    private EditText mProxyExclusionListEdit;
       
    private static Comparator<String> sIfaceComparator = new Comparator<String>() {
        @Override
        public int compare(String object1, String object2) {
            return object1.compareToIgnoreCase(object2);
        }
    };    
    
    public EthernetProxyDialog(Context context) {
        super(context);
        
        mContext = context;
        mEloManager = new EloPeripheralManager(context, null);
        
        setTitle(R.string.eth_proxy_settings_title);
        setButton(BUTTON_POSITIVE, getContext().getText(R.string.eth_menu_save), this);
        setButton(BUTTON_NEGATIVE, getContext().getText(R.string.eth_menu_cancel), this);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mMainView = getLayoutInflater().inflate(R.layout.eth_proxy_configure_v2, /* root */ null);
        setView(mMainView);
        buildDialog();
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void onStart() {
        super.onStart();

        mDevList = getEthernetDeviceList();
        if (mDevList.size() > 0) {
            mProxyInfo = new EthernetProxyInfo(mDevList.get(0));
            mProxyInfo.readConfig(getContext());
        } else {
            dismiss();
            return;
        }

        // Fill UI fields and add listener
        initDevListUI();
        initProxyFields();

        // Add listener
        mDevSpinnerView.setOnItemSelectedListener(this);
        mProxyTypeSpinner.setOnItemSelectedListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                saveConfig();
                break;
            case BUTTON_NEGATIVE:
                dialog.cancel();
                break;
            default:
                Log.e(TAG,"Unknow button");
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mDevSpinnerView) {
            if (mDevList.size() > position) {
                if (mProxyInfo.getIfName().equalsIgnoreCase(mDevList.get(position))) {
                    // do nothing if selected item doesn't change
                } else {
                    mProxyInfo = new EthernetProxyInfo(mDevList.get(position));
                    mProxyInfo.readConfig(getContext());
                    // refresh proxy settings fields with last config
                    initProxyFields();
                }
            }
        } else if (parent == mProxyTypeSpinner) {
            // refresh UI when proxy type changed
            updateProxyFields();
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        if (parent == mDevSpinnerView) {
            Log.e(TAG,"onNothingSelected, dev list");
        }
    }
    
    private void buildDialog() {
        mDevSpinnerView = (Spinner) mMainView.findViewById(R.id.eth_dev_spinner);

        mProxyTypeSpinner = (Spinner) mMainView.findViewById(R.id.proxy_type_settings);
        mStaticProxyGroup = mMainView.findViewById(R.id.static_proxy_fields);
        mProxyHostEdit = (EditText) mMainView.findViewById(R.id.proxy_address_edit);
        mProxyPortEdit = (EditText) mMainView.findViewById(R.id.proxy_port_edit);
        mProxyExclusionListEdit = (EditText) mMainView.findViewById(R.id.proxy_exclusionlist);
        mPacGroup = mMainView.findViewById(R.id.pac_proxy_fields);
        mProxyPacUrlEdit = (EditText) mMainView.findViewById(R.id.proxy_pac_edit);
    }

    public void saveConfig() {
        EthernetProxyInfo info = new EthernetProxyInfo(mProxyInfo.getIfName());
        if (DBG) {
            Log.d(TAG, "Config device for " + mDevSpinnerView.getSelectedItem().toString());
        }

        // build proxy settings to EthernetProxyInfo
        int proxyType = mProxyTypeSpinner.getSelectedItemPosition();
        if (proxyType == EthernetProxyInfo.PROXY_TYPE_NONE) {
            info.setProxySettingType(EthernetProxyInfo.PROXY_TYPE_NONE);
        } else if (proxyType == EthernetProxyInfo.PROXY_TYPE_STATIC) {
            String proxyAddr = mProxyHostEdit.getText().toString();
            String proxyPort = mProxyPortEdit.getText().toString();
            String bypass = mProxyExclusionListEdit.getText().toString();

            int proxyErrorRes = EthernetUtils.validateProxy(proxyAddr, proxyPort, bypass);
            if (proxyErrorRes != 0) {
                Toast.makeText(mContext, proxyErrorRes, Toast.LENGTH_LONG).show();
                return;
            }
            info.setProxySettingType(EthernetProxyInfo.PROXY_TYPE_STATIC);
            info.setProxyAddr(proxyAddr);
            info.setProxyPort(proxyPort);
            info.setProxyExclusionList(bypass);
        } else if (proxyType == EthernetProxyInfo.PROXY_TYPE_PAC) {
            info.setProxySettingType(EthernetProxyInfo.PROXY_TYPE_PAC);
            info.setProxyPacUrl(mProxyPacUrlEdit.getText().toString());
        }
       
        // config new proxy settings to Ethernet device
        ProxySetting settings = info.buildProxySetting();
        if (settings != null) {
            info.writeConfig(mContext);
            mEloManager.updateEthernetProxy(settings);
        }
    }
    
    private void initDevListUI() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(), android.R.layout.simple_spinner_item, mDevList);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mDevSpinnerView.setAdapter(adapter);

        for (int index = 0; index < mDevList.size(); index++) {
            if (mProxyInfo.getIfName().equalsIgnoreCase(mDevList.get(index))) {
                mDevSpinnerView.setSelection(index);
                break;
            }
        }
    }    
    
    private void initProxyFields() {
        int proxyType = mProxyInfo.getProxySettingType();
        if (proxyType == EthernetProxyInfo.PROXY_TYPE_PAC) {
            mProxyTypeSpinner.setSelection(EthernetProxyInfo.PROXY_TYPE_PAC);
        } else if (proxyType == EthernetProxyInfo.PROXY_TYPE_STATIC) {
            mProxyTypeSpinner.setSelection(EthernetProxyInfo.PROXY_TYPE_STATIC);
        } else {
            mProxyTypeSpinner.setSelection(EthernetProxyInfo.PROXY_TYPE_NONE);
        }
        updateProxyFields();
    }
    
    private void updateProxyFields() {
        if (mProxyTypeSpinner.getSelectedItemPosition() == EthernetProxyInfo.PROXY_TYPE_PAC) {
            mPacGroup.setVisibility(View.VISIBLE);
            mStaticProxyGroup.setVisibility(View.GONE);
            String pacUrl = mProxyInfo.getProxyPacUrl();
            mProxyPacUrlEdit.setText(pacUrl, TextView.BufferType.EDITABLE);
        } else if (mProxyTypeSpinner
                .getSelectedItemPosition() == EthernetProxyInfo.PROXY_TYPE_STATIC) {
            mStaticProxyGroup.setVisibility(View.VISIBLE);
            mPacGroup.setVisibility(View.GONE);
            String proxyHost = mProxyInfo.getProxyAddr();
            mProxyHostEdit.setText(proxyHost, TextView.BufferType.EDITABLE);

            String proxyPort = mProxyInfo.getProxyPort();
            mProxyPortEdit.setText(proxyPort, TextView.BufferType.EDITABLE);

            String proxyExclude = mProxyInfo.getProxyExclusionList();
            mProxyExclusionListEdit.setText(proxyExclude, TextView.BufferType.EDITABLE);
        } else {
            mPacGroup.setVisibility(View.GONE);
            mStaticProxyGroup.setVisibility(View.GONE);
        }
    }
    
    /**
     * get all the ethernet device names
     * @return interface name list on success, {@code null} on failure
     */
    public static List<String> getEthernetDeviceList() {
        ArrayList<String> devList = new ArrayList<String>();
        try {
            List<NetworkInterface> interfaces = Collections
                    .list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.getName().startsWith(mIfacePrefix)) {
                    devList.add(intf.getName());
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, "SocketException when list ethernet iface", ex);
        }

        if (devList.size() > 0) {
            devList.sort(sIfaceComparator);
        }

        return devList;
    }
}

