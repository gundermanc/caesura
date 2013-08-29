package com.gundersoft.caesura;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

public class DummyActivity extends Activity{
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        if(getIntent().getStringExtra("MITS_Action").compareTo("MITS_ACTION_UNLOCK_DEVICE") == 0){
        	getWindow().setFlags(LayoutParams.FLAG_TURN_SCREEN_ON|LayoutParams.FLAG_KEEP_SCREEN_ON|
        			LayoutParams.FLAG_DISMISS_KEYGUARD|LayoutParams.FLAG_SHOW_WHEN_LOCKED, LayoutParams.FLAG_TURN_SCREEN_ON|LayoutParams.FLAG_KEEP_SCREEN_ON|
        			LayoutParams.FLAG_DISMISS_KEYGUARD|LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        	KeyguardManager mgr = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE); 
	        KeyguardLock lock = mgr.newKeyguardLock(Context.KEYGUARD_SERVICE);
	        lock.disableKeyguard();
        }
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
        //setContentView(R.layout.main);
        finish();
    }
}
