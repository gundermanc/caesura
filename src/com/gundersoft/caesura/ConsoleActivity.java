package com.gundersoft.caesura;

import com.gundersoft.caesura.Tools.UI;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.SmsManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ConsoleActivity extends Activity{
	private static final int CONTACT_PICKER_RESULT = 1001;
	
	private static Context globalSharedContext = null;
	private static TextView historyEditText = null;
	private static EditText commandEditText = null;
	private static SharedPreferences preferences;
	private SmsManager sm = null;
	private static String[] g_cmdsList = null;
	private String phoneNumber = "";
   @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.console, menu);
        return true;
    }
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
   	// Handle item selection
       switch (item.getItemId()) {
       case R.id.insertCommandMenuItem:
    	   popInsertCmdDialog();
    	   return true;
       case R.id.manualMenuItem:
    	   CaesuraActivity.popInstructionManual(this);
    	   return true;
       case R.id.insertPhoneNumberMenuItem:
    	   Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,  
		            Contacts.CONTENT_URI);  
		    startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT+1);
    	   return true;
       case R.id.consoleTipsMenuItem:
    	   CaesuraActivity.popINETDialog(this, "file:///android_asset/console_tips.html", "");
    	   return true;
       default:
           return super.onOptionsItemSelected(item);
       }
   }
	@Override
	   public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.console);   
        
        globalSharedContext = getApplicationContext();
        sm = SmsManager.getDefault();
        
        //Set up Preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(globalSharedContext);
        
        commandEditText = (EditText)findViewById(R.id.commandEditText);
        
        historyEditText = (TextView)findViewById(R.id.historyEditText);
     
        Button sendButton = (Button)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String commandText = commandEditText.getText().toString();
				if(commandText.length() > 0){
					if(commandText.compareToIgnoreCase(getString(R.string.service_cmd_exit)) == 0) {
						Toast.makeText(globalSharedContext, R.string.console_exit_msg, Toast.LENGTH_LONG).show();
						finish();
					}
					sendsms(commandText);
					commandEditText.setText("");
					recvsms(globalSharedContext, "Me", commandText, false);
					//Handler scrollHandler = new Handler();
					/*scrollHandler.post(new Runnable() {
						public void run() {
							historyEditText.scrollTo(0, (historyEditText.getLineCount() * historyEditText.getLineHeight()) / historyEditText.set);
						}
					});*/
					
				} else {
					Toast.makeText(globalSharedContext, R.string.console_msg_err_blank, Toast.LENGTH_SHORT).show();
				}
			}
		});
        //Prompt for phone number
        telephoneNumberPrompt();
	}
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		switch (requestCode) {  
        case CONTACT_PICKER_RESULT:  
        	if (resultCode == RESULT_OK) {  
        		// handle contact results  
            	Uri result = data.getData();
            	String id = result.getLastPathSegment();
            	Cursor cursor = getContentResolver().query(  
            	        Phone.CONTENT_URI, null,  
            	        Phone.CONTACT_ID + "=?",  
            	        new String[]{id}, null);  
            	
            	int emailIdx = cursor.getColumnIndex(Email.DATA);  
            	  
                // let's just get the first phone
                if (cursor.moveToFirst()) {  
                    String phone = cursor.getString(emailIdx); 
                    initConsole(phone);
                } else {
                	Toast.makeText(this, R.string.console_msg_phone_err, Toast.LENGTH_LONG).show();
                	finish();
                }
    	    } else {  
    	        // gracefully handle failure  
    	        Log.w("Caesura_Console_Contacts_Picker", "Warning: activity result not ok");  
    	        finish();
    	    }  
            break; 
        case CONTACT_PICKER_RESULT+1:  
        	if (resultCode == RESULT_OK) {  
        		// handle contact results  
            	Uri result = data.getData();
            	String id = result.getLastPathSegment();
            	Cursor cursor = getContentResolver().query(  
            	        Phone.CONTENT_URI, null,  
            	        Phone.CONTACT_ID + "=?",  
            	        new String[]{id}, null);  
            	
            	int emailIdx = cursor.getColumnIndex(Email.DATA);  
            	  
                // let's just get the first phone
                if (cursor.moveToFirst()) {  
                    String phone = cursor.getString(emailIdx); 
                    String tmpText = commandEditText.getText().toString();
                    if(!tmpText.endsWith(" ")){
                    	tmpText += " ";
                    }
                    commandEditText.setText(tmpText + phone);
                } else {
                	Toast.makeText(this, R.string.console_msg_phone_err, Toast.LENGTH_LONG).show();
                }
    	    } else {  
    	        // gracefully handle failure  
    	        Log.w("Caesura_Console_Contacts_Picker", "Warning: activity result not ok");  
    	    }  
            break;
        }  
	}  
	@Override
	public void onDestroy() {
		historyEditText = null;
		super.onDestroy();
	}
	@Override
	public void onBackPressed(){
		sendsms(getString(R.string.service_cmd_exit));
		Toast.makeText(globalSharedContext, R.string.console_exit_msg, Toast.LENGTH_LONG).show();
		finish();
	}
	private void telephoneNumberPrompt() {
		final AlertDialog.Builder inputDialogBox = new AlertDialog.Builder(this);
		inputDialogBox.setTitle(R.string.app_name);
		inputDialogBox.setMessage(R.string.console_connect_dialog_message);
		inputDialogBox.setIcon(R.drawable.connect_icon);
	
		final EditText ttsText = new EditText(globalSharedContext);
		inputDialogBox.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		inputDialogBox.setNeutralButton(R.string.console_contacts_btn, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,  
				            Contacts.CONTENT_URI);  
				    startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
			}
		});
		inputDialogBox.setPositiveButton(R.string.console_connect_dialog_btn, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				initConsole(ttsText.getText().toString()); //Check Phone Number and start console
			}
		});
		
		LinearLayout testBoxLayout = new LinearLayout(this);	
		testBoxLayout.setOrientation(LinearLayout.VERTICAL);
		testBoxLayout.addView(ttsText);
		
		inputDialogBox.setView(testBoxLayout);
		inputDialogBox.show();
	}
	private void popInsertCmdDialog() {
		final AlertDialog.Builder inputDialogBox = new AlertDialog.Builder(this);
		inputDialogBox.setTitle(R.string.app_name);
		inputDialogBox.setIcon(R.drawable.exclamation);
	
		final Spinner cmdSpinner = new Spinner(this);
		Tools.enumSpinnerFromArray(this, cmdSpinner, g_cmdsList);
		cmdSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				commandEditText.setText(cmdSpinner.getSelectedItem().toString());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				commandEditText.setText(cmdSpinner.getSelectedItem().toString());
			}
		});
		
		inputDialogBox.setPositiveButton(R.string.console_insert_cmd_ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		LinearLayout testBoxLayout = new LinearLayout(this);	
		testBoxLayout.setOrientation(LinearLayout.VERTICAL);
		testBoxLayout.addView(cmdSpinner);
		
		inputDialogBox.setView(testBoxLayout);
		inputDialogBox.setMessage(R.string.console_insert_cmd);
		inputDialogBox.show();
	}
	private void initConsole(String newPhoneNumber) {
		if(newPhoneNumber.length() >= 10) {
			phoneNumber = newPhoneNumber;
			Intent moleServiceIntent = new Intent(globalSharedContext, CaesuraMoleService.class);
			
			//Get Command List from Service
			moleServiceIntent.putExtra("MITS_Mode", "CONSOLE_POACH");
			startService(moleServiceIntent);
			
			//send connect command
			sendsms(getString(R.string.service_cmd_login)); //Send Login command
			Toast.makeText(globalSharedContext, R.string.console_connect_success, Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(globalSharedContext, R.string.console_connect_dialog_err, Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	public static void updateCmdsList(String[] cmdsList) {
		g_cmdsList = cmdsList;
	}
	public static boolean recvsms(Context context, String smsSender, String smsMessageBody, boolean notify){
		if(historyEditText != null){
			historyEditText.setText(historyEditText.getText().toString() + smsSender
					+ ": " + smsMessageBody + "\n");
			if(preferences.getBoolean("MITS_Console_Notifications_Enabled", true) && notify){
				String notificationTitle = context.getString(R.string.console_notification_ticker);
				UI.popNotification(ConsoleActivity.class, context, 0, notificationTitle, 
						notificationTitle, smsMessageBody, null, null, 
						Notification.FLAG_AUTO_CANCEL, Notification.DEFAULT_SOUND|Notification.DEFAULT_LIGHTS|Notification.DEFAULT_VIBRATE);
				
			}
			return true;
		}
		return false;
	}
	private void sendsms(String smsMessageBody){
		sm.sendTextMessage(phoneNumber, smsMessageBody, smsMessageBody, null, null);
	}
}
