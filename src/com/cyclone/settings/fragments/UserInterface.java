/*
 * Copyright (C) 2023 Cyclone-AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cyclone.settings.fragments;

import static android.os.UserHandle.USER_CURRENT;
import static android.os.UserHandle.USER_SYSTEM;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.os.UserHandle;
import android.database.ContentObserver;
import android.content.om.IOverlayManager;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import androidx.preference.*;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.custom.preference.SystemSettingListPreference;

public class UserInterface extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_QS_PANEL_STYLE  = "qs_panel_style";

    private Handler mHandler;
    private IOverlayManager mOverlayManager;
    private IOverlayManager mOverlayService;
    private SystemSettingListPreference mQsStyle;

    public static final String TAG = "UserInterface";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.cyclone_settings_ui);

        mOverlayService = IOverlayManager.Stub
        .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));

        mQsStyle = (SystemSettingListPreference) findPreference(KEY_QS_PANEL_STYLE);
        mCustomSettingsObserver.observe();
        final Context mContext = getActivity().getApplicationContext();        
    }
    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            Context mContext = getContext();
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_PANEL_STYLE),
                    false, this, UserHandle.USER_ALL);
        }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (uri.equals(Settings.System.getUriFor(Settings.System.QS_PANEL_STYLE))) {
            updateQsStyle();
            }
        }
    }  
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQsStyle) {
            mCustomSettingsObserver.observe();
            return true;
        }            
        return false;
    }
    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE, 0, UserHandle.USER_CURRENT);
    }

    private void updateQsStyle() {
        ContentResolver resolver = getActivity().getContentResolver();

        int qsPanelStyle = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.QS_PANEL_STYLE , 0, UserHandle.USER_CURRENT);

        if (qsPanelStyle == 0) {
            setDefaultStyle(mOverlayService);
        } else if (qsPanelStyle == 1) {
            setQsStyle(mOverlayService, "com.android.system.qs.outline");
        } else if (qsPanelStyle == 2 || qsPanelStyle == 3) {
            setQsStyle(mOverlayService, "com.android.system.qs.twotoneaccent");
        }
    }

    public static void setDefaultStyle(IOverlayManager overlayManager) {
        for (int i = 0; i < QS_STYLES.length; i++) {
            String qsStyles = QS_STYLES[i];
            try {
                overlayManager.setEnabled(qsStyles, false, USER_SYSTEM);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setQsStyle(IOverlayManager overlayManager, String overlayName) {
        try {
            for (int i = 0; i < QS_STYLES.length; i++) {
                String qsStyles = QS_STYLES[i];
                try {
                    overlayManager.setEnabled(qsStyles, false, USER_SYSTEM);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            overlayManager.setEnabled(overlayName, true, USER_SYSTEM);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static final String[] QS_STYLES = {
        "com.android.system.qs.outline",
        "com.android.system.qs.twotoneaccent"
    };

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CYCLONE;
    }
}
