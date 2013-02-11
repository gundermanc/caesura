package com.gundersoft.caesura;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

public class PanicBannerActivity extends Activity{
	private static PanicMode pm = null;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panicbanner);    
        setTitle("MoleInTheSystem - Panic!");

        getWindow().addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED|LayoutParams.FLAG_KEEP_SCREEN_ON|LayoutParams.FLAG_TURN_SCREEN_ON);
        
        //Set Wallpaper
        getWindow().setBackgroundDrawable(peekWallpaper());
        
        //Set Text
		final TextView bannerTitleTextView = (TextView)findViewById(R.id.bannerTitleTextView);
		bannerTitleTextView.setBackgroundColor(0x99000000);
		bannerTitleTextView.setShadowLayer(12, 0, 0, 0xFFFF0000);
	    bannerTitleTextView.setText(getIntent().getStringExtra("PM_TITLE"));
	        
	    final TextView bannerCaptionTextView = (TextView)findViewById(R.id.bannerCaptionTextView);
	    bannerCaptionTextView.setBackgroundColor(0xFF000000);
	    bannerCaptionTextView.setText(getIntent().getStringExtra("PM_TEXT"));
	    
    }
	
	public static void registerPanicMode(PanicMode newPm){
		pm = newPm;
	}
	@Override
	public void onBackPressed(){
		//Do nothing! Muahahaha! :D
	}
	
	public void stopSound(View v) {
		pm.activatePanic(null, null, false, false);
		pm.closePanicMode();
		pm = null;
		finish();
	}
}
