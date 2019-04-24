package com.example.chatapp;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

public class SettingFrame extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.setting, s);
    }
}