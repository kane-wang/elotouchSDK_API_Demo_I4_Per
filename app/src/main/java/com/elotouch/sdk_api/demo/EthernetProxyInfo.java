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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

import com.elotouch.library.net.ProxySetting;

public class EthernetProxyInfo {
    private static final String DEFAULT_ETH_IFACE = "eth0";
    
    /** These values come from "proxy_type_settings" resource array */
    public static final int PROXY_TYPE_NONE = 0;
    public static final int PROXY_TYPE_STATIC = 1;
    public static final int PROXY_TYPE_PAC = 2; 
    
    // IP configuration for Ethernet iface
    private String mIfaceName;
    private int mProxyType;
    private String mProxyPacUrl;
    private String mProxyHost;
    private String mProxyPort;
    private String mProxyExclusionList;

    public EthernetProxyInfo(String iface) {
        mIfaceName = iface;
        mProxyType = PROXY_TYPE_NONE;
        mProxyPacUrl = null;
        mProxyHost = null;
        mProxyPort = null;
        mProxyExclusionList = null;
    }
    
    public synchronized void readConfig(Context context) {
        if (TextUtils.isEmpty(mIfaceName)) {
            mIfaceName = DEFAULT_ETH_IFACE;
        }
        // if SharedPreferences for current iface is not exist, init with default value
        SharedPreferences sp = context.getSharedPreferences(mIfaceName, Context.MODE_PRIVATE);
        
        // get ProxySettings from SharedPreferences
        String proxyType = sp.getString("proxy_type", "");
        if (proxyType.equalsIgnoreCase("none")) {
            mProxyType = PROXY_TYPE_NONE;
        } else if (proxyType.equalsIgnoreCase("static")) {
            mProxyType = PROXY_TYPE_STATIC;
        } else if (proxyType.equalsIgnoreCase("pac")) {
            mProxyType = PROXY_TYPE_PAC;
        } else {
            mProxyType = PROXY_TYPE_NONE;
        }

        if (mProxyType == PROXY_TYPE_PAC) {
            mProxyPacUrl = sp.getString("proxyPacUrl", null);
        } else if (mProxyType == PROXY_TYPE_STATIC) {
            mProxyHost = sp.getString("proxy_host", null);
            mProxyPort = sp.getString("proxy_port", null);
            mProxyExclusionList = sp.getString("proxy_exclusion_list", null);
        }
    }
    
    public synchronized void writeConfig(Context context) {
        if (TextUtils.isEmpty(mIfaceName)) {
            mIfaceName = DEFAULT_ETH_IFACE;
        }

        SharedPreferences sp = context.getSharedPreferences(mIfaceName, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        if (mProxyType == PROXY_TYPE_NONE) {
            editor.putString("proxy_type", "none");
            editor.remove("proxyPacUrl");
            editor.remove("proxy_host");
            editor.remove("proxy_port");
            editor.remove("proxy_exclusion_list");
        } else if (mProxyType == PROXY_TYPE_PAC) {
            editor.putString("proxy_type", "pac");
            editor.putString("proxyPacUrl", mProxyPacUrl);
            editor.remove("proxy_host");
            editor.remove("proxy_port");
            editor.remove("proxy_exclusion_list");
        } else if (mProxyType == PROXY_TYPE_STATIC) {
            editor.putString("proxy_type", "static");
            editor.putString("proxy_host", mProxyHost);
            editor.putString("proxy_port", mProxyPort);
            editor.putString("proxy_exclusion_list", mProxyExclusionList);
            editor.remove("proxyPacUrl");
        }
        editor.commit();
    }
    
    public synchronized ProxySetting buildProxySetting() {
        try {
            if (mProxyType == PROXY_TYPE_NONE) {
                return new ProxySetting(mIfaceName);
            } else if (mProxyType == PROXY_TYPE_PAC) {
                return ProxySetting.buildPacProxyWithIface(mIfaceName, mProxyPacUrl);
            } else if (mProxyType == PROXY_TYPE_STATIC) {
                return ProxySetting.buildDirectProxyWithIface(mIfaceName, mProxyHost, mProxyPort,
                        mProxyExclusionList);
            }
        } catch (IllegalArgumentException ex) {
            Log.e("EthernetProxyInfo", "build ProxySetting with invalid args:" + ex.toString());
            return null;
        }
        return null;
    }
    
    /**
     * save interface name into the configuration
     */
    public void setIfName(String ifname) {
        this.mIfaceName = ifname;
    }

    /**
     * Returns the interface name from the saved configuration
     * @return interface name
     */
    public String getIfName() {
        return mIfaceName;
    }

    public int getProxySettingType() {
        return mProxyType;
    }

    public int setProxySettingType(int type) {
        return this.mProxyType = type;
    }
    
    public String getProxyPacUrl() {
        return mProxyPacUrl;
    }

    public void setProxyPacUrl(String urlString) {
        this.mProxyPacUrl = urlString;
    }
        
    public void setProxyAddr(String ip) {
        this.mProxyHost = ip;
    }

    public String getProxyAddr() {
        return mProxyHost;
    }

    public void setProxyPort(String port) {
        this.mProxyPort = port;
    }

    public String getProxyPort() {
        return mProxyPort;
    }

    public String getProxyExclusionList() {
        return mProxyExclusionList;
    }

    public void setProxyExclusionList(String mProxyExclusionList) {
        this.mProxyExclusionList = mProxyExclusionList;
    }
    
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("iface:" + mIfaceName);

        if (mProxyType == PROXY_TYPE_NONE) {
            builder.append("proxy_type:" + "none");
        } else if (mProxyType == PROXY_TYPE_PAC) {
            builder.append("proxy_type:" + "pac");
            builder.append("proxyPacUrl:" + mProxyPacUrl);
        } else if (mProxyType == PROXY_TYPE_STATIC) {
            builder.append("proxy_type:" + "static");
            builder.append("proxy_host:" + mProxyHost);
            builder.append("proxy_port:" + mProxyPort);
            builder.append("proxy_exclusion_list:" + mProxyExclusionList);
        }

        return builder.toString();
    }
}
