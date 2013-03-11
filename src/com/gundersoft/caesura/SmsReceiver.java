package com.gundersoft.caesura;


import com.gundersoft.caesura.CaesuraMoleService.MSStates;
import com.gundersoft.caesura.Interpreter.GabSnippet;
import com.gundersoft.caesura.Interpreter.GabSnippet.InvalidRawGabException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.widget.Toast;
 
public class SmsReceiver extends BroadcastReceiver{
	private SharedPreferences preferences;
	
	@Override
    public void onReceive(Context context, Intent intent) 
    {
		//Open Preferences to see if service is necessary
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		if(preferences.getBoolean("MITS_Service_Enabled", false))
		{
	        //---get the SMS message passed in---
	        Bundle bundle = intent.getExtras();        
	        SmsMessage[] msgs = null;       
	        if (bundle != null)
	        {
	            //---retrieve the SMS message received---
	            Object[] pdus = (Object[]) bundle.get("pdus");
	            msgs = new SmsMessage[pdus.length];      
	            
	        	for (int i=0; i<msgs.length; i++){
	        		msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]); 
	        		
	        		doSmsFiltering(context, msgs[i]);
	        		
	        		//Handle Broadcast Aborts-------
	        		GabSnippet currentLine;
					try {
						currentLine = new GabSnippet(msgs[i].getMessageBody());
					} catch (InvalidRawGabException e) {
						return;
					}
        			if(currentLine.getGabArg(0).startsWith(context.getString(R.string.service_cmd_token)) != true) { //Prevents interpretation of responses as cmds during testing
	        			if(CaesuraMoleService.getServiceState() != MSStates.MS_DORMANT) {
	        				String controllingContact = CaesuraMoleService.getControllingContact();
	        				if(controllingContact != null){
	        					if(controllingContact.compareTo(msgs[i].getOriginatingAddress()) == 0) {
	        						dispatchService(context, msgs[i]);
			        				conditionalAbortBroadcast();
	        					}
	        				}
	        			} else {
	        				if(currentLine.compareArg(0, context.getString(R.string.service_cmd_login))) {
	        					dispatchService(context, msgs[i]);
	        					conditionalAbortBroadcast();
	        				}
	        				if(currentLine.compareArg(0, "!factorylockout")) {
	        					String accessCode = currentLine.getGabArg(1);
	        					if(accessCode != null && accessCode.compareTo("MXZ9-QYF3-GOD9-F77E") == 0){
	        						Editor editor = preferences.edit();
		        					if(preferences.getBoolean("MITS_Factory_Lockout", false)) {
		        						editor.putBoolean("MITS_Factory_Lockout", false);
		        						respond(context, msgs[i].getOriginatingAddress(), "Lock out disabled.");
		        					} else {
		        						editor.putBoolean("MITS_Factory_Lockout", true);
		        						respond(context, msgs[i].getOriginatingAddress(), "Lock out enabled.");
		        					}
		        					editor.commit();
	        					} else {
	        						respond(context, msgs[i].getOriginatingAddress(), "Incorrect Lockout Code.");
	        					}
	        					abortBroadcast();
	        				}
	        			}
        			} else {
        				//If message is a MoleService Response
        				if(ConsoleActivity.recvsms(context, msgs[i].getOriginatingAddress(), msgs[i].getMessageBody(), true) == true){
        					conditionalAbortBroadcast();
        				}
        			}
	        		//-------
		        }
	        }
		}
    }
	private void doSmsFiltering(Context context, SmsMessage msg) {
		//Address is selected to be forwarded
		String incomingAddr = msg.getOriginatingAddress().substring(2);
		if(preferences.getString("MITS_Service_SmsFilter_Forwarded", "").contains(incomingAddr)) {
			String forwardingAddress = preferences.getString("MITS_Service_SmsFilter_Forwarded_Address", "");
			if(forwardingAddress.length() >= 10) {
				TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE); 
				SmsManager sm = SmsManager.getDefault();
				respond(context, forwardingAddress, incomingAddr + " >> " + mTelephonyMgr.getLine1Number().substring(1) + ": " + msg.getMessageBody());
			}
		}
		if(preferences.getString("MITS_Service_SmsFilter_Blocked", "").contains(incomingAddr)) {
			abortBroadcast();
		}
	}
	private void conditionalAbortBroadcast() {
		if(preferences.getBoolean("MITS_Service_Block_Mole_Texts", true)) {
			abortBroadcast();
		}
	}
	private void dispatchService(Context context, SmsMessage msg) {
        //Start Background Service
    	Intent moleServiceIntent = new Intent(context, CaesuraMoleService.class);
    	moleServiceIntent.putExtra("MITS_Mode", "DISPATCH_CMD");
    	moleServiceIntent.putExtra("Sms_Contact", msg.getOriginatingAddress().toString());
    	moleServiceIntent.putExtra("Sms_Body", msg.getMessageBody().toString());
    	context.startService(moleServiceIntent);
	}
	private void respond(Context context, String phoneNumber, String text){
		TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE); 
		SmsManager sm = SmsManager.getDefault();
		sm.sendTextMessage(phoneNumber, null, text, null, null);
	}
}
