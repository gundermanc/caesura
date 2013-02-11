package com.gundersoft.caesura;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;


public class LocationActivity extends Activity{
	Context globalSharedContext = null;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.location);   
        
        globalSharedContext = getApplicationContext();
    }
}
