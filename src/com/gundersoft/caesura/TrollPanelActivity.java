package com.gundersoft.caesura;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;

public class TrollPanelActivity extends Activity{
	private Context globalSharedContext;
	private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.trollpanel);
        
        globalSharedContext = getApplicationContext();
        
        //Set up Preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(globalSharedContext);
        editor = preferences.edit();	
    }
}
