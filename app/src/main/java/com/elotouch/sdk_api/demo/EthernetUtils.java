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

import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.elotouch.sdk_api.demo.R;

public final class EthernetUtils {
    private static final String TAG = "EthernetUtils";
    
    // [+]Kelly, allow input hostname to proxy host name
    private static final String HC = "a-zA-Z0-9\\_";

    private static final String HOSTNAME_REGEXP =
            "^$|^[" + HC + "]+(\\-[" + HC + "]+)*(\\.[" + HC + "]+(\\-[" + HC + "]+)*)*$";
    private static final Pattern HOSTNAME_PATTERN;
    private static final String EXCLUSION_REGEXP =
            "$|^(\\*)?\\.?[" + HC + "]+(\\-[" + HC + "]+)*(\\.[" + HC + "]+(\\-[" + HC + "]+)*)*$";
    private static final Pattern EXCLUSION_PATTERN;
    static {
        HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEXP);
        EXCLUSION_PATTERN = Pattern.compile(EXCLUSION_REGEXP);
    }
    // [-]Kelly, allow input hostname to proxy host name
    
    public static int validateProxy(String hostname, String port, String exclList) {
        Log.d(TAG, "validateProxy");

        Matcher match = HOSTNAME_PATTERN.matcher(hostname);
        String exclListArray[] = exclList.split(",");

        if (!match.matches())
            return R.string.proxy_error_invalid_host;

        for (String excl : exclListArray) {
            Matcher m = EXCLUSION_PATTERN.matcher(excl);
            if (!m.matches())
                return R.string.proxy_error_invalid_exclusion_list;
        }

        if (hostname.length() > 0 && port.length() == 0) {
            return R.string.proxy_error_empty_port;
        }

        if (port.length() > 0) {
            if (hostname.length() == 0) {
                return R.string.proxy_error_empty_host_set_port;
            }
            int portVal = -1;
            try {
                portVal = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                return R.string.proxy_error_invalid_port;
            }
            if (portVal <= 0 || portVal > 0xFFFF) {
                return R.string.proxy_error_invalid_port;
            }
        }
        return 0;
    }
}
