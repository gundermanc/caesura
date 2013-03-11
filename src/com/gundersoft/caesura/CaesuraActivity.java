package com.gundersoft.caesura;

//import com.google.ads.AdRequest;
//import com.google.ads.AdView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

public class CaesuraActivity extends Activity {
    private static final int TTS_DATA_CHECK_INTENT = 155999;
	private Context globalSharedContext;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    /**Initializes Menu **/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// Handle item selection
        switch (item.getItemId()) {
        case R.id.aboutMenuItem:
            aboutDialogBox();
            return true;
        case R.id.settingsMenuItem:
        	Intent settingsMenuIntent = new Intent(this, SettingsActivity.class);
        	startActivity(settingsMenuIntent);
        	return true;
        case R.id.removeLauncherIconMenuItem:
        	killUIComponents();
        	return true;
        case R.id.rateMenuItem:
        	Toast.makeText(globalSharedContext, R.string.main_rate_msg, Toast.LENGTH_LONG).show();  
        	startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=com.gundersoft.caesura")));  
        	return true;
        case R.id.emailMenuItem:
        	Toast.makeText(globalSharedContext, R.string.main_feedback_msg, Toast.LENGTH_LONG).show(); 
        	Intent emailIntent = new Intent(Intent.ACTION_SEND);
        	emailIntent.setType("text/html");
        	emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"gundermanc@gmail.com"});

        	emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "**Caesura -- Feedback and Support" );
        	PackageInfo thisPackage;
    		try {
    			thisPackage = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
    			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Name:\n" + "Build: " + thisPackage.versionCode + 
    					"\nI need (check all that apply):\n - Help [ ]\n - Support [ ]\n - To give Feedback [ ]\n - To Aimlessly Chat [ ]\n - To buy you a cup of coffee (Donate) [ ]\n\n\n" );
    		} catch (NameNotFoundException e) {
    			e.printStackTrace();
    			return false;
    		}
        	
        	startActivity(emailIntent);
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);   
        
        globalSharedContext = getApplicationContext();
        
        PanicMode pm = new PanicMode(false, false);
        pm.activatePanic(getApplication(), this, true, false);
        pm.activatePanic(getApplication(), this, false, false);
        pm.activatePanic(getApplication(), this, true, false);
        pm.activatePanic(getApplication(), this, false, false);
        
        LiveLocation lm = new LiveLocation(globalSharedContext);
        if(lm.initProvider()) {
        	Toast.makeText(globalSharedContext, "PROVIDED!!", Toast.LENGTH_LONG).show();
        }   
		
        //Set up Preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(globalSharedContext);
        editor = preferences.edit();	
        
        //Setup Ads:
        //final AdView adView = (AdView)findViewById(R.id.adView);
        //adView.loadAd(new AdRequest());
        
        //Set up Click Handlers and restore preferences
        CheckBox enableServiceCheckBox = (CheckBox)findViewById(R.id.serviceEnabledCheckBox);
        enableServiceCheckBox.setChecked(preferences.getBoolean("MITS_Service_Enabled", false));
        enableServiceCheckBox.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(((CheckBox)v).isChecked()) {
					editor.putBoolean("MITS_Service_Enabled", true);
				} else {
					editor.putBoolean("MITS_Service_Enabled", false);
				}
				editor.commit();
			}
		});
        
        Button smsBlackListButton = (Button)findViewById(R.id.smsBlackListButton);
        smsBlackListButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent smsFilterActivityIntent = new Intent(globalSharedContext, SmsFilterActivity.class);
				startActivity(smsFilterActivityIntent);
			}
		});
        
        Button trollPanelButton = (Button) findViewById(R.id.trollPanelButton);
        trollPanelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent TrollPanelActivityIntent = new Intent(globalSharedContext, TrollPanelActivity.class);
				startActivity(TrollPanelActivityIntent);
			}
		});
        
        Button openConsoleButton = (Button) findViewById(R.id.openConsoleButton);
        openConsoleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent consoleActivityIntent = new Intent(globalSharedContext, ConsoleActivity.class);
				startActivity(consoleActivityIntent);
			}
		});
        
        Button tutorialsButton = (Button) findViewById(R.id.tutorialsButton);
        tutorialsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder tutorialsAlert = new AlertDialog.Builder(CaesuraActivity.this);
		    	tutorialsAlert.setTitle(R.string.app_name);
		    	tutorialsAlert.setMessage(R.string.main_tutorials_msg);

		    	tutorialsAlert.show();
			}
		});
        
        Button manualButton = (Button) findViewById(R.id.manualButton);
        manualButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				popInstructionManual(CaesuraActivity.this);
			}
		});
        
        Button locationButton = (Button) findViewById(R.id.locationButton);
        locationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent locationActivityIntent = new Intent(globalSharedContext, LocationActivity.class);
				startActivity(locationActivityIntent);
			}
		});
        
        Button exitButton = (Button)findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();			
			}
		});
        
        //Update Connected Icon
        updateConnectedIcon();
        
        //Install TTS Data
        checkTTSData();
        
        //Ask for Password
        passwordPrompt();
        
        //Display Notes:
        releaseNotes();
        
        //Register App Online if not already done:
        //registerApp(false);
    }
    public static void popInstructionManual(Context context){
    	popINETDialog(context, "file:///android_asset/manual.html", context.getString(R.string.app_name));
    }
	
    private void updateConnectedIcon() {
		TextView connectedTextView = (TextView)findViewById(R.id.connectedTextView);
    	if(CaesuraMoleService.isConnected()) {
    		connectedTextView.setText(R.string.main_connected_tv_connected);
    		connectedTextView.setShadowLayer(10, 0, 0, 0xFFFF0000);
    	} else {
    		connectedTextView.setText(R.string.main_connected_tv_disconnected);
    		connectedTextView.setShadowLayer(10, 0, 0, 0xFF00FF00);
    	}
    }
    private void releaseNotes(){
    	if(preferences.getBoolean("MITS_release_notes_firstrun_BETA_r2", true) == true) {
        	popINETDialog(this, "file:///android_asset/license.html", getString(R.string.app_name));
        	editor.putBoolean("MITS_release_notes_firstrun_BETA_r2", false);
        	editor.putBoolean("MITS_Registered", true); //Mandate Re-registration
        	editor.commit();
        	
           	//Register App Instance on online database:
        	registerApp(true);
    	}  
    }
    private boolean registerApp(boolean force) {
    	/*if(preferences.getBoolean("MITS_Registered", false) && !force) {
    		return false;
    	}
	    TelephonyManager mTelephonyMgr;
	    mTelephonyMgr = (TelephonyManager) globalSharedContext.getSystemService(Context.TELEPHONY_SERVICE);
    	String number = mTelephonyMgr.getLine1Number();
    	PackageInfo thisPackage;
		try {
			thisPackage = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			return false;
		}
    	String versionCode = String.valueOf(thisPackage.versionCode);
    	
    	if(number == null){
    		number = "ERROR_NO_NUMBER";
    	}
    	
    	String responseString = Tools.SimpleHttpRequest.get("http://www.gundersoft.com/pages/downloads/caesura/registration?number="
    			+ number + "&version=" + versionCode + "&country=" + mTelephonyMgr.getSimCountryIso());
    	if(responseString == null || !responseString.contains("ERROR_SUCCESS")){
    		Toast.makeText(globalSharedContext, R.string.main_register_fail, Toast.LENGTH_LONG).show();
    		return false;
    	} else {
    		editor.putBoolean("MITS_Registered", true);
    		editor.commit();
    	}*/
    	return true;
    }
    public void checkTTSData() {
    	Intent checkIntent = new Intent();
    	checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
    	startActivityForResult(checkIntent, TTS_DATA_CHECK_INTENT);
    }
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data) {
        if (requestCode == TTS_DATA_CHECK_INTENT) {
            if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            	installTTSData();
            }
        }
    }
    public void installTTSData() {
    	AlertDialog.Builder updateAlert = new AlertDialog.Builder(this);
    	updateAlert.setTitle(R.string.app_name);
    	updateAlert.setMessage(R.string.main_checktts_dialog);
    	updateAlert.setPositiveButton(R.string.main_checktts_download, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent installIntent = new Intent();
				installIntent.setAction(
	            TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	            startActivity(installIntent);
	            finish();
				
			}
		});
    	updateAlert.setNegativeButton(R.string.main_checktts_cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Toast.makeText(globalSharedContext, R.string.main_checktts_err, Toast.LENGTH_SHORT);
			}
		});
    	updateAlert.show();
    }
    public static void popINETDialog(Context context, String URL, String dialogTitle)  {
		AlertDialog.Builder webViewBox = new AlertDialog.Builder(context);
		webViewBox.setTitle(dialogTitle);
		final WebView helpWebView = new WebView(context);
		
		//webViewBox.setPositiveButton("Close", null);
		helpWebView.loadUrl(URL);
		LinearLayout ttsBoxLayout = new LinearLayout(context);	
		ttsBoxLayout.setOrientation(LinearLayout.VERTICAL);
		ttsBoxLayout.addView(helpWebView);
		
		webViewBox.setView(ttsBoxLayout);

		webViewBox.show();
    }
    @Override
    public boolean onSearchRequested() {
    	Toast.makeText(globalSharedContext, "sfsf", Toast.LENGTH_LONG).show();
    	return false;
    }
	private void passwordPrompt() {
		final String mitsSavedPassword = preferences.getString("MITS_Service_Password", "");
		if(mitsSavedPassword.compareTo("") != 0) {
			AlertDialog.Builder inputDialogBox = new AlertDialog.Builder(this);
			//The section below prevents cancelling of the dialog via search key
			inputDialogBox.setOnKeyListener(new DialogInterface.OnKeyListener() {

			    @Override
			    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			        if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
			            return true; // Pretend we processed it
			        }
			        return false; // Any other keys are still processed as normal
			    }
			});
			inputDialogBox.setTitle(R.string.app_name);
			inputDialogBox.setMessage(R.string.main_login_dialog_message);
			inputDialogBox.setIcon(R.drawable.icon);
			
			final EditText ttsText = new EditText(globalSharedContext);
			inputDialogBox.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					CaesuraActivity.this.finish();
				}
			});
			inputDialogBox.setPositiveButton(R.string.main_login_dialog_btn, new DialogInterface.OnClickListener() {	
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(ttsText.getText().toString().compareTo(mitsSavedPassword) != 0) {
						CaesuraActivity.this.finish();
					}
					
				}
			});

			LinearLayout testBoxLayout = new LinearLayout(this);	
			testBoxLayout.setOrientation(LinearLayout.VERTICAL);
			testBoxLayout.addView(ttsText);
			
			inputDialogBox.setView(testBoxLayout);
			inputDialogBox.show();
		} else {
	    	SettingsActivity.setPasswordPrompt(this, editor);
		}
	}
	private void aboutDialogBox() {
		AlertDialog.Builder inputDialogBox = new AlertDialog.Builder(this);
		String displayText = getString (R.string.main_about_dlg_message);
		PackageInfo thisPackage = null;
		
		inputDialogBox.setTitle(R.string.app_name);
		try {
			thisPackage = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
			displayText = displayText.replace("%V", thisPackage.versionName);
			displayText = displayText.replace("%B", Integer.toString(thisPackage.versionCode));
		}catch (NameNotFoundException e) {
			displayText = displayText.replace("%V", "[UNKNOWN]");
			displayText = displayText.replace("%B", "[UNKNOWN]");
			e.printStackTrace();
		}
		inputDialogBox.setIcon(getResources().getDrawable(R.drawable.abouticon));
		inputDialogBox.setMessage(displayText);
		inputDialogBox.setPositiveButton("Close", null);
		inputDialogBox.show();
	}
	private void killUIComponents(){
		AlertDialog.Builder confirmationBox = new AlertDialog.Builder(this);
		confirmationBox.setTitle(R.string.app_name);
		confirmationBox.setMessage(R.string.main_kill_ui_box_msg);
		confirmationBox.setPositiveButton(R.string.main_button_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO Auto-generated method stub
				PackageManager p = getPackageManager();
				p.setComponentEnabledSetting(CaesuraActivity.this.getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
				Toast.makeText(globalSharedContext, R.string.main_kill_ui_restart_msg, Toast.LENGTH_LONG).show();
			}
		});
		confirmationBox.setNegativeButton(R.string.main_button_no, null);
		confirmationBox.show();
		}
}