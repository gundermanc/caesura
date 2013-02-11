package com.gundersoft.caesura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

public class SettingsActivity extends Activity{
	private Context globalSharedContext;
	private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.settings);
        
        globalSharedContext = getApplicationContext();
        
        //Set up Preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(globalSharedContext);
        editor = preferences.edit();	
        
        //Listeners:
        CheckBox blockMoleTextsCheckBox = (CheckBox)findViewById(R.id.blockMoleTextsCheckBox);
        blockMoleTextsCheckBox.setChecked(preferences.getBoolean("MITS_Service_Block_Mole_Texts", true));
        blockMoleTextsCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(((CheckBox)v).isChecked()) {
					editor.putBoolean("MITS_Service_Block_Mole_Texts", true);
				} else {
					editor.putBoolean("MITS_Service_Block_Mole_Texts", false);
				}
				editor.commit();
			}
		});
        
        CheckBox requirePasswordCheckBox = (CheckBox)findViewById(R.id.requirePasswordCheckBox);
        requirePasswordCheckBox.setChecked(preferences.getBoolean("MITS_Service_Require_Password", true));
        requirePasswordCheckBox.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(((CheckBox)v).isChecked()) {
					editor.putBoolean("MITS_Service_Require_Password", true);
				} else {
					editor.putBoolean("MITS_Service_Require_Password", false);
				}
				editor.commit();
			}
		});
        
        CheckBox notificationsEnabledCheckBox = (CheckBox)findViewById(R.id.notificationsEnabledCheckBox);
        notificationsEnabledCheckBox.setChecked(preferences.getBoolean("MITS_Console_Notifications_Enabled", true));
        notificationsEnabledCheckBox.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(((CheckBox)v).isChecked()) {
					editor.putBoolean("MITS_Console_Notifications_Enabled", true);
				} else {
					editor.putBoolean("MITS_Console_Notifications_Enabled", false);
				}
				editor.commit();
			}
		});
        
        Button setPasswordButton = (Button)findViewById(R.id.setPasswordButton);
        setPasswordButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setPasswordPrompt(SettingsActivity.this, editor);
			}
		});
        
        Button configurePanicModeButton = (Button)findViewById(R.id.configurePanicModeButton);
        configurePanicModeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				configurePanicMode();
			}
		});
        
        Button reviewAccessLogButton = (Button)findViewById(R.id.reviewAccessLogButton);
        reviewAccessLogButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				viewAccessLog();
			}
		});
        
    }
    private void configurePanicMode() {
		AlertDialog.Builder inputDialogBox = new AlertDialog.Builder(this);
		inputDialogBox.setTitle(R.string.app_name);
		inputDialogBox.setMessage(R.string.main_configure_panic_mode_message);
		
		final EditText titleBox = new EditText(globalSharedContext);
		final EditText textBox = new EditText(globalSharedContext);
		
		//Load Preferences:
		titleBox.setText(preferences.getString("MITS_Service_Panic_Title", "LOST PHONE!"));
		textBox.setText(preferences.getString("MITS_Service_Panic_Text", "Please return this phone. Thank you."));
		
		inputDialogBox.setPositiveButton(R.string.main_configure_panic_mode_btn, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				editor.putString("MITS_Service_Panic_Title", titleBox.getText().toString());
				editor.putString("MITS_Service_Panic_Text", textBox.getText().toString());
				editor.commit();
			}
		});

		LinearLayout testBoxLayout = new LinearLayout(this);	
		testBoxLayout.setOrientation(LinearLayout.VERTICAL);
		testBoxLayout.addView(titleBox);
		testBoxLayout.addView(textBox);
		
		inputDialogBox.setView(testBoxLayout);
		inputDialogBox.show();
	}
    public static void setPasswordPrompt(Context context, final Editor prefEditor) {
		AlertDialog.Builder inputDialogBox = new AlertDialog.Builder(context);
		inputDialogBox.setTitle(R.string.app_name);
		inputDialogBox.setMessage(R.string.main_set_pswd_dialog_message);
		
		final EditText ttsText = new EditText(context);
		
		inputDialogBox.setPositiveButton(R.string.main_set_pswd_dialog_btn, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				prefEditor.putString("MITS_Service_Password", ttsText.getText().toString());
				prefEditor.commit();
			}
		});

		LinearLayout testBoxLayout = new LinearLayout(context);	
		testBoxLayout.setOrientation(LinearLayout.VERTICAL);
		testBoxLayout.addView(ttsText);
		
		inputDialogBox.setView(testBoxLayout);
		inputDialogBox.show();
	}
    private void viewAccessLog() {
		AlertDialog.Builder inputDialogBox = new AlertDialog.Builder(this);
		inputDialogBox.setTitle(R.string.app_name);
		inputDialogBox.setMessage(R.string.main_access_log_message);
		
		final EditText logTextBox = new EditText(globalSharedContext);
		logTextBox.setMinLines(10);
		logTextBox.setGravity(Gravity.TOP);
		
		//Load Log:
		logTextBox.setText(preferences.getString("MITS_Service_Access_Log", ""));
		logTextBox.setTextSize(12);
		
		inputDialogBox.setPositiveButton(R.string.main_review_access_log_dialog_btn, null);
		inputDialogBox.setNegativeButton(R.string.main_review_access_log_dialog_clear_btn, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				editor.putString("MITS_Service_Access_Log", "");
				editor.commit();
			}
		});
		
		logTextBox.setFilters(new InputFilter[] {
			    new InputFilter() {
					@Override
					public CharSequence filter(CharSequence src, int start, 
							int end, Spanned dst, int dstart, int dend) {
            			return src.length() < 1 ? dst.subSequence(dstart, dend) : "";
					}
			    }
			});

		LinearLayout testBoxLayout = new LinearLayout(this);	
		testBoxLayout.setOrientation(LinearLayout.VERTICAL);
		testBoxLayout.addView(logTextBox);
		
		inputDialogBox.setView(testBoxLayout);
		inputDialogBox.show();
	}
}
