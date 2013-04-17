//
//  Util.java
//  AdjustIo
//
//  Created by Christian Wellenbrock on 11.10.12.
//  Copyright (c) 2012 adeven. All rights reserved.
//

package com.adeven.adjustio;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings.Secure;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

public class Util {

    private static final String BASEURL = "https://app.adjust.io";
    private static final String CLIENTSDK = "android1.5";
    private static final String LOGTAG = "AdjustIo";

    private static final String PREFS_NAME = "com.adeven.adjustio";
    private static final String PREFS_KEY = "mac";
    private static final byte MAC_LOCALLY_ADMINISTERED_UNICAST = 2;

    public static boolean checkPermissions(Application app) {
        boolean result = true;

        if (app.checkCallingOrSelfPermission(android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED) {
            Log.e(LOGTAG, "This SDK requires the INTERNET permission. See the README for details.");
            result = false;
        }
        if (app.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_DENIED) {
            Log.w(LOGTAG, "You can improve your tracking results by adding the ACCESS_WIFI_STATE permission. See the README for details.");
        }

        return result;
    }

    public static String getBase64EncodedParameters(Map<String, String> parameters) {
        if (parameters == null) {
            return null;
        }

        JSONObject jsonObject = new JSONObject(parameters);
        byte[] bytes = jsonObject.toString().getBytes();
        String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
        return encoded;
    }

    public static StringEntity getEntityEncodedParameters(String... parameters)
            throws UnsupportedEncodingException {
        List<NameValuePair> pairs = new ArrayList<NameValuePair>(2);
        for (int i = 0; i + 1 < parameters.length; i += 2) {
            String key = parameters[i];
            String value = parameters[i + 1];
            if (value != null) {
                pairs.add(new BasicNameValuePair(key, value));
            }
        }

        StringEntity entity = new UrlEncodedFormEntity(pairs);
        return entity;
    }

    public static HttpClient getHttpClient(String userAgent) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpParams params = httpClient.getParams();
        params.setParameter(CoreProtocolPNames.USER_AGENT, userAgent);
        return httpClient;
    }

    public static HttpPost getPostRequest(String path) {
        String url = BASEURL + path;
        HttpPost request = new HttpPost(url);

        String language = Locale.getDefault().getLanguage();
        request.addHeader("Accept-Language", language);
        request.addHeader("Client-SDK", CLIENTSDK);

        return request;
    }

    protected static String getUserAgent(Application app) {
        Resources resources = app.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        Locale locale = configuration.locale;
        int screenLayout = configuration.screenLayout;

        StringBuilder builder = new StringBuilder();
        builder.append(getPackageName(app));
        builder.append(" " + getAppVersion(app));
        builder.append(" " + getDeviceType(screenLayout));
        builder.append(" " + getDeviceName());
        builder.append(" " + getOsName());
        builder.append(" " + getOsVersion());
        builder.append(" " + getLanguage(locale));
        builder.append(" " + getCountry(locale));
        builder.append(" " + getScreenSize(screenLayout));
        builder.append(" " + getScreenFormat(screenLayout));
        builder.append(" " + getScreenDensity(displayMetrics));
        builder.append(" " + getDisplayWidth(displayMetrics));
        builder.append(" " + getDisplayHeight(displayMetrics));

        String userAgent = builder.toString();
        return userAgent;
    }

    private static String getPackageName(Application app) {
        String packageName = app.getPackageName();
        String sanitized = sanitizeString(packageName);
        return sanitized;
    }

    private static String getAppVersion(Application app) {
        try {
            PackageManager packageManager = app.getPackageManager();
            String name = app.getPackageName();
            PackageInfo info = packageManager.getPackageInfo(name, 0);
            String versionName = info.versionName;
            String result = sanitizeString(versionName);
            return result;
        } catch (NameNotFoundException e) {
            return "unknown";
        }
    }

    private static String getDeviceType(int screenLayout) {
        int screenSize = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        switch (screenSize) {
        case Configuration.SCREENLAYOUT_SIZE_SMALL:
        case Configuration.SCREENLAYOUT_SIZE_NORMAL:
            return "phone";
        case Configuration.SCREENLAYOUT_SIZE_LARGE:
        case 4:
            return "tablet";
        default:
            return "unknown";
        }
    }

    private static String getDeviceName() {
        String deviceName = Build.MODEL;
        String sanitized = sanitizeString(deviceName);
        return sanitized;
    }

    private static String getOsName() {
        return "android";
    }

    private static String getOsVersion() {
        String osVersion = "" + Build.VERSION.SDK_INT;
        String sanitized = sanitizeString(osVersion);
        return sanitized;
    }

    private static String getLanguage(Locale locale) {
        String language = locale.getLanguage();
        String sanitized = sanitizeString(language, "zz");
        return sanitized;
    }

    private static String getCountry(Locale locale) {
        String country = locale.getCountry();
        String sanitized = sanitizeString(country, "zz");
        return sanitized;
    }

    private static String getScreenSize(int screenLayout) {
        int screenSize = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        switch (screenSize) {
        case Configuration.SCREENLAYOUT_SIZE_SMALL:
            return "small";
        case Configuration.SCREENLAYOUT_SIZE_NORMAL:
            return "normal";
        case Configuration.SCREENLAYOUT_SIZE_LARGE:
            return "large";
        case 4:
            return "xlarge";
        default:
            return "unknown";
        }
    }

    private static String getScreenFormat(int screenLayout) {
        int screenFormat = screenLayout & Configuration.SCREENLAYOUT_LONG_MASK;

        switch (screenFormat) {
        case Configuration.SCREENLAYOUT_LONG_YES:
            return "long";
        case Configuration.SCREENLAYOUT_LONG_NO:
            return "normal";
        default:
            return "unknown";
        }
    }

    private static String getScreenDensity(DisplayMetrics displayMetrics) {
        int density = displayMetrics.densityDpi;
        int low = (DisplayMetrics.DENSITY_MEDIUM + DisplayMetrics.DENSITY_LOW) / 2;
        int high = (DisplayMetrics.DENSITY_MEDIUM + DisplayMetrics.DENSITY_HIGH) / 2;

        if (density == 0) {
            return "unknown";
        } else if (density < low) {
            return "low";
        } else if (density > high) {
            return "high";
        } else {
            return "medium";
        }
    }

    private static String getDisplayWidth(DisplayMetrics displayMetrics) {
        String displayWidth = String.valueOf(displayMetrics.widthPixels);
        String sanitized = sanitizeString(displayWidth);
        return sanitized;
    }

    private static String getDisplayHeight(DisplayMetrics displayMetrics) {
        String displayHeight = String.valueOf(displayMetrics.heightPixels);
        String sanitized = sanitizeString(displayHeight);
        return sanitized;
    }

    protected static String getMacAddress(Application app) {
        String rawAddress = getRawMacAddress(app);
        String upperAddress = rawAddress.toUpperCase();
        String sanitized = sanitizeString(upperAddress);
        return sanitized;
    }

    private static String getRawMacAddress(final Application app) {
        final SharedPreferences prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final String storedMac = prefs.getString(PREFS_KEY, null);
        if (storedMac != null) {
            return storedMac;
        }
        final byte[] newMac = generateMac();
        final String mac = formatWithColons(newMac);
        prefs.edit().putString(PREFS_KEY, mac).commit();
        Log.i(LOGTAG, String.format("generated (and stored) mac address: %s", mac));
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

    // removes spaces and replaces empty string with "unknown"
    private static String sanitizeString(String string) {
        return sanitizeString(string, "unknown");
    }

    private static String sanitizeString(String string, String defaultString) {
        if (string == null) {
            string = defaultString;
        }

        String result = string.replaceAll("\\s", "");
        if (result.length() == 0) {
            result = defaultString;
        }

        return result;
    }

    public static String loadAddress(String interfaceName) {
        try {
            String filePath = "/sys/class/net/" + interfaceName + "/address";
            StringBuffer fileData = new StringBuffer(1000);
            BufferedReader reader;
            reader = new BufferedReader(new FileReader(filePath));
            char[] buf = new char[1024];
            int numRead = 0;

            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
            }

            reader.close();
            String address = fileData.toString();
            return address;
        } catch (IOException e) {
            return null;
        }
    }

    public static String getAndroidId(Application app) {
        return Secure.getString(app.getContentResolver(), Secure.ANDROID_ID);
    }

    public static String getAttributionId(Application application) {
        try {
            ContentResolver contentResolver = application.getContentResolver();
            Uri uri = Uri.parse("content://com.facebook.katana.provider.AttributionIdProvider");
            String columnName = "aid";
            String[] projection = {columnName};
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            String attributionId = cursor.getString(cursor.getColumnIndex(columnName));
            cursor.close();
            return attributionId;
        } catch (Exception e) {
            return null;
        }
    }

    public static String sha1(String text) {
        try {
            MessageDigest mesd = MessageDigest.getInstance("SHA-1");
            byte[] bytes = text.getBytes("iso-8859-1");
            mesd.update(bytes, 0, bytes.length);
            byte[] sha2hash = mesd.digest();
            return convertToHex(sha2hash);
        } catch (Exception e) {
            return "";
        }
    }

    private static String convertToHex(byte[] bytes) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int halfbyte = (bytes[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buffer.append((char) ('0' + halfbyte));
                else
                    buffer.append((char) ('a' + (halfbyte - 10)));
                halfbyte = bytes[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        String hex = buffer.toString();
        return hex;
    }
}
