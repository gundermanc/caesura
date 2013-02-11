package com.gundersoft.caesura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

public class SmsFilterActivity extends Activity{
	private Context globalSharedContext;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private EditText blockedNumbersEditText;
    public static final int FORWARDED = 0;
    public static final int BLOCKED = 1;
   @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.smsfilter);     
        
        globalSharedContext = getApplicationContext();
        
        //Set up Preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(globalSharedContext);
        editor = preferences.edit();
        
        Button addForwardedButton = (Button)findViewById(R.id.addForwardedButton);
        addForwardedButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addNumber(FORWARDED);
			}
		});
        
        Button removeForwardedButton = (Button)findViewById(R.id.removeForwardedButton);
        removeForwardedButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Spinner blockedNumbersSpinner = (Spinner)findViewById(R.id.blockedNumbersSpinner);
				if(removeNumber(FORWARDED, preferences, editor, 
						blockedNumbersSpinner.getSelectedItem().toString())) {
					Toast.makeText(globalSharedContext, getString(R.string.smsfilter_blocked_success), Toast.LENGTH_LONG).show();
				}
				refresh();
			}
		});
        
        Button forwardedOptionsButton = (Button) findViewById(R.id.forwardedOptionsButton);
        forwardedOptionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				viewForwardedOptions();
			}
		});
        
        Button addBlockedButton = (Button)findViewById(R.id.addBlockedButton);
        addBlockedButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addNumber(BLOCKED);
			}
		});
        
        Button removeBlockedButton = (Button)findViewById(R.id.removeBlockedButton);
        removeBlockedButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Spinner blockedNumbersSpinner = (Spinner)findViewById(R.id.blockedNumbersSpinner);
				if(removeNumber(BLOCKED, preferences, editor, 
						blockedNumbersSpinner.getSelectedItem().toString())) {
					Toast.makeText(globalSharedContext, getString(R.string.smsfilter_blocked_success), Toast.LENGTH_LONG).show();
				}
				refresh();
			}
		});
        
        //Load Text
        refresh();
   }
   private void refresh() {
	   final Spinner blockedNumbersSpinner = (Spinner)findViewById(R.id.blockedNumbersSpinner);
	   String [] items = preferences.getString("MITS_Service_SmsFilter_Blocked", "").split("\n");
	   Tools.enumSpinnerFromArray(globalSharedContext, blockedNumbersSpinner, items);
	   
	   final Spinner forwardedNumbersSpinner = (Spinner)findViewById(R.id.forwardedNumbersSpinner);
	   items = preferences.getString("MITS_Service_SmsFilter_Forwarded", "").split("\n");
	   Tools.enumSpinnerFromArray(globalSharedContext, forwardedNumbersSpinner, items);
	   if(preferences.getString("MITS_Service_SmsFilter_Forwarded_Address", "").length()>=10){
		   forwardedNumbersSpinner.setEnabled(true);
	   } else {
		   forwardedNumbersSpinner.setEnabled(false);
	   }
   }
   private void viewForwardedOptions() {
	   	AlertDialog.Builder inputDialogBox = new AlertDialog.Builder(this);
		inputDialogBox.setTitle(R.string.app_name);
		inputDialogBox.setMessage(R.string.smsfilter_forwarded_options_msg);
		
		final EditText numberTextBox = new EditText(globalSharedContext);
		numberTextBox.setText(preferences.getString("MITS_Service_SmsFilter_Forwarded_Address", ""));
		inputDialogBox.setPositiveButton(R.string.smsfilter_forwarded_options_ok_btn, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String resultNumber = numberTextBox.getText().toString();
				if(resultNumber.length() >= 10) {
					editor.putString("MITS_Service_SmsFilter_Forwarded_Address", resultNumber);
					editor.commit();
					refresh();
					Toast.makeText(globalSharedContext, getString(R.string.smsfilter_blocked_success), Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(globalSharedContext, getString(R.string.smsfilter_fowarded_options_error), Toast.LENGTH_LONG).show();
				}
			}
		});
		
		LinearLayout testBoxLayout = new LinearLayout(this);	
		testBoxLayout.setOrientation(LinearLayout.VERTICAL);
		testBoxLayout.addView(numberTextBox);
		
		inputDialogBox.setView(testBoxLayout);
		inputDialogBox.show();
  }
   private void addNumber(final int type) {
	   	AlertDialog.Builder inputDialogBox = new AlertDialog.Builder(this);
		inputDialogBox.setTitle(R.string.app_name);
		inputDialogBox.setMessage(R.string.smsfilter_blocked_add_msg);
		
		final EditText numberTextBox = new EditText(globalSharedContext);
		inputDialogBox.setPositiveButton(R.string.smsfilter_blocked_add_btn, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String resultNumber = numberTextBox.getText().toString();
				if(resultNumber != null) {
					if(!addNumber(type, preferences, editor, resultNumber)){
						Toast.makeText(globalSharedContext, getString(R.string.smsfilter_blocked_add_error), Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(globalSharedContext, getString(R.string.smsfilter_blocked_success), Toast.LENGTH_LONG).show();
					}
					refresh();
				}
				
			}
		});
		
		LinearLayout testBoxLayout = new LinearLayout(this);	
		testBoxLayout.setOrientation(LinearLayout.VERTICAL);
		testBoxLayout.addView(numberTextBox);
		
		inputDialogBox.setView(testBoxLayout);
		inputDialogBox.show();
   }
   public static boolean addNumber(int type, SharedPreferences sPreferences, SharedPreferences.Editor sEditor, String numberToAdd) {
	   String oldBlockedNumbers;
	   if(!CaesuraMoleService.validatePhoneNumber(numberToAdd))
		   return false; // invalid phone number
	   if(type == BLOCKED){ 
		   oldBlockedNumbers = sPreferences.getString("MITS_Service_SmsFilter_Blocked", "");
	   } else  {
		   oldBlockedNumbers = sPreferences.getString("MITS_Service_SmsFilter_Forwarded", "");
	   }
	   if(oldBlockedNumbers.contains(numberToAdd) || numberToAdd.length() < 10) {
		   return false;
	   } else {
		   String toBeStored = oldBlockedNumbers;
		   if(oldBlockedNumbers.length()>0)
			   toBeStored += "\n";
		   toBeStored += numberToAdd;
		   
		   if(type == BLOCKED){
			   sEditor.putString("MITS_Service_SmsFilter_Blocked", toBeStored); 
		   } else {
			   sEditor.putString("MITS_Service_SmsFilter_Forwarded", toBeStored); 
		   }
		   sEditor.commit();
		   return true;
	   }
   }
   public static boolean removeNumber(int type, SharedPreferences sPreferences, SharedPreferences.Editor sEditor, String numberToRemove) {
	   String oldBlockedNumbers;
	   if(!CaesuraMoleService.validatePhoneNumber(numberToRemove))
		   return false; // invalid phone number
	   if(type == BLOCKED){ 
		   oldBlockedNumbers = sPreferences.getString("MITS_Service_SmsFilter_Blocked", "");
	   } else  {
		   oldBlockedNumbers = sPreferences.getString("MITS_Service_SmsFilter_Forwarded", "");
	   }
	   String newBlockedNumbers = "";
	   if(oldBlockedNumbers.contains(numberToRemove)) {
		   if(oldBlockedNumbers.contains("\n" + numberToRemove + "\n")){
			   newBlockedNumbers = oldBlockedNumbers.replace("\n" + numberToRemove + "\n", "");
		   }
		   if(oldBlockedNumbers.contains("\n" + numberToRemove)){
			   newBlockedNumbers = oldBlockedNumbers.replace("\n" + numberToRemove, "");
		   }
		   if(oldBlockedNumbers.contains(numberToRemove + "\n")){
			   newBlockedNumbers = oldBlockedNumbers.replace(numberToRemove + "\n", "");
		   }
		   if(type == BLOCKED) {
			    sEditor.putString("MITS_Service_SmsFilter_Blocked", newBlockedNumbers);
		   } else {
			   sEditor.putString("MITS_Service_SmsFilter_Forwarded", newBlockedNumbers);
		   }
		   sEditor.commit();
		   return true;
	   } else {
		   return false;
	   }
   }
   
   /**
    * Gets a complete list of numbers
    * @param type Which list to getNumbers from: either BLOCKED, or FORWARDED
    * @param sPrefs A SharedPreferences object that has access to this key.
    * @return A String containing the list of numbers
    */
   public static String getNumbers(int type, SharedPreferences sPrefs) {
	   String numbers = null;
	   if(type == FORWARDED)
		   numbers = sPrefs.getString("MITS_Service_SmsFilter_Forwarded", "");
	   else if(type == BLOCKED)
		   numbers = sPrefs.getString("MITS_Service_SmsFilter_Blocked", "");
	   return (numbers.length() >= 10) ? numbers:null;
   }
   
   public static boolean setForwardingAddress(String number, SharedPreferences.Editor sEditor){
	   if(!CaesuraMoleService.validatePhoneNumber(number))
		   return false;
	   sEditor.putString("MITS_Service_SmsFilter_Forwarded_Address", number);
	   sEditor.commit();
	   return true;
   }
   
   /**
    * Gets the address to forward the texts to.
    * @param sPrefs An initialized SharedPreferences that has access to the
    * key storing the settings values.
    * @return A String containing the phone number, or null if there is none.
    */
   public static String getForwardingAddress(SharedPreferences sPrefs){
	   String number = sPrefs.getString("MITS_Service_SmsFilter_Forwarded_Address", "");
	   return CaesuraMoleService.validatePhoneNumber(number) ? number:null;
   }
}
