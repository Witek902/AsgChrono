package com.example.michal.asgchrono;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SetupFragment extends PreferenceFragment {

    /**
     * Create an instance of SetupFragment.
     */
    public static SetupFragment newInstance() {
        SetupFragment fragment = new SetupFragment();
        fragment.setArguments(null);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.setup_preference);
    }
}