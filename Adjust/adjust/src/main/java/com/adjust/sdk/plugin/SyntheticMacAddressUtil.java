package com.adjust.sdk.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.adjust.sdk.AdjustFactory;
import com.adjust.sdk.ILogger;

import java.security.SecureRandom;
import java.util.Locale;

public class SyntheticMacAddressUtil {

    private static final String PREFS_NAME = "com.adeven.adjustio";
    private static final String PREFS_KEY = "mac";
    private static final byte MAC_LOCALLY_ADMINISTERED_UNICAST = 2;

    private static ILogger logger = AdjustFactory.getLogger();

    public static String getMacAddress(final Context context) {
        final String rawAddress = getRawMacAddress(context);
        if (rawAddress == null) {
            return null;
        }
        final String upperAddress = rawAddress.toUpperCase(Locale.US);
        return removeSpaceString(upperAddress);
    }

    private static String getRawMacAddress(final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final String storedMac = prefs.getString(PREFS_KEY, null);
        if (storedMac != null) {
            return storedMac;
        }
        final byte[] newMac = generateMac();
        final String mac = formatWithColons(newMac);
        prefs.edit().putString(PREFS_KEY, mac).commit();
        logger.info(String.format("Generated (and stored) MAC address: %s", mac));
        return mac;
    }

    private static byte[] generateMac() {
        final SecureRandom random = new SecureRandom();
        final byte[] newMac = new byte[6];
        random.nextBytes(newMac);
        newMac[0] = MAC_LOCALLY_ADMINISTERED_UNICAST;
        return newMac;
    }

    private static String formatWithColons(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
            if (i < (bytes.length - 1)) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    private static String removeSpaceString(final String inputString) {
        if (inputString == null) {
            return null;
        }

        final String outputString = inputString.replaceAll("\\s", "");
        if (TextUtils.isEmpty(outputString)) {
            return null;
        }

        return outputString;
    }
}
