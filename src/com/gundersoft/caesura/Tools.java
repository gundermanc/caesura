package com.gundersoft.caesura;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.KeyguardManager.KeyguardLock;
import android.app.admin.DevicePolicyManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

public abstract class Tools {
	public static class GPS {
		public static void setGPSState(Context context, boolean enabled){
	        String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
	        if(enabled) {
		        if(!provider.contains("gps")){ //if gps is disabled
		        	pokeGPS(context);
		        }
	        } else {
	            if(provider.contains("gps")){ //if gps is enabled
	            	pokeGPS(context);
	            }
	        }
	    }
		public static class LocationInterface {
        	Context gContext;
        	LocationManager mlocManager;
        	public LocationInterface (Context context) {
        		gContext = context;
                mlocManager = (LocationManager)gContext.getSystemService(Context.LOCATION_SERVICE);
                Location sd = mlocManager.getLastKnownLocation(null);
                String res = Double.toString(sd.getLatitude());
                res += ":";
                res += Double.toString(sd.getLongitude());
        	}
        	public static int loc() {
        	return 0;
        	}
        }
		public static boolean getGPSState(Context context) {
	        String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
	        if(provider.contains("gps")){ //if gps is disabled
	        	return true;
	        } else {
	        	return false;
	        }
		}
		public static boolean canToggleGPS(Context context) {
	        PackageManager pacman = context.getPackageManager();
	        PackageInfo pacInfo = null;

	        try {
	            pacInfo = pacman.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS);
	        } catch (NameNotFoundException e) {
	            return false; //package not found
	        }

	        if(pacInfo != null){
	            for(ActivityInfo actInfo : pacInfo.receivers){
	                //test if receiver is exported. if so, we can toggle GPS.
	                if(actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported){
	                    return true;
	                }
	            }
	        }

	        return false; //default
	    }
		private static void pokeGPS (Context context) {
			final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3")); 
            context.sendBroadcast(poke);
		}
	}	
	public static class WIFI {
		public static boolean setWifiState(Context context, boolean enabled) {
	        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
	        return wifiManager.setWifiEnabled(enabled);
		}
		public static boolean isWifiOn(Context context) {
			WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
			if(wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
				 return true;
			} else {
				return false;
			}
		}
	}
	public static class Torch {
		//Constants
		public static final int TORCH_ERROR_SUCCESS = 0;
		public static final int TORCH_ERROR_NO_CAMERA_OPEN = 1;
		public static final int TORCH_ERROR_UNABLE_GET_PARAMS = 2;
		public static final int TORCH_ERROR_NO_FLASH = 3;
		public static final int TORCH_ERROR_NOT_SUPPORTED = 4;
		private boolean lightOn = false;
		private Camera mCamera;
		public Torch(){
			
		}
		public boolean initTorch() {
			if (mCamera == null) {
				try {
					mCamera = Camera.open();
					if(mCamera != null){
						mCamera.startPreview();
						return true;
					}
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		    return false;
		}
		public boolean isTorchOn() {
			return lightOn;
		}
		public void closeTorch() {
			if(mCamera != null) {
				mCamera.release();
				lightOn = false;
			}
		}
		public int activateTorch(boolean activateLight) {
			if (mCamera == null) {
				return TORCH_ERROR_NO_CAMERA_OPEN;
			}
			Parameters parameters = mCamera.getParameters();
			if (parameters == null) {
				return TORCH_ERROR_UNABLE_GET_PARAMS;
			}
			List<String> flashModes = parameters.getSupportedFlashModes();
			// Check if camera flash exists
			if (flashModes == null) {
				// No Flash
				return TORCH_ERROR_NO_FLASH;
			}
			String flashMode = parameters.getFlashMode();
			if(activateLight) {
				if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
					// Turn on the flash
					if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
						parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
				        mCamera.setParameters(parameters);
				        lightOn = true;
						return TORCH_ERROR_SUCCESS;
					}
				}
			} else {
				if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
			        // Turn off the flash
			        if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
			          parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
			          mCamera.setParameters(parameters);
			          lightOn = false;
			          return TORCH_ERROR_SUCCESS;
			        }
			      }
			}
	       	return TORCH_ERROR_NOT_SUPPORTED;
		}
	}
	public static class SmsInbox {
		public static int getUnread(Context context) {
			final Uri SMS_INBOX = Uri.parse("content://sms/inbox");
	
			Cursor c = context.getContentResolver().query(SMS_INBOX, null, "read = 0", null, null);
			int unreadMessagesCount = c.getCount();
			c.deactivate();
			
			return unreadMessagesCount;
		}
		public static void writeMsg(Context context, String addr, String body) {
			ContentValues values = new ContentValues();
			values.put("address", addr);
			values.put("body", body);
			context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
		}		
	}
	public static class Battery {
		public static int getRemainingPercent(Context context){
			Intent bat = context.registerReceiver(null, new 
					IntentFilter(Intent.ACTION_BATTERY_CHANGED)); 
					            int level = bat.getIntExtra("level", 0); 
					            int scale = bat.getIntExtra("scale", 100); 
					            return level * 100 / scale; 
		}
	}
	public static class TTS {
		private static TextToSpeech ttsEngine = null;
		public static final int FAIL = 2;
		public static final int INITIALIZING = 1;
		public static final int SUCCESS = 0;
		
		public static int speak(final Context context, String text){
			if(ttsEngine == null){ //If TTS service is off, start it up
				ttsEngine = new TextToSpeech(context, new OnInitListener() {
					@Override
					public void onInit(int status) {
						if (status == TextToSpeech.SUCCESS) {
				            int result = ttsEngine.setLanguage(Locale.US);
				            if (result == TextToSpeech.LANG_MISSING_DATA ||
				                result == TextToSpeech.LANG_NOT_SUPPORTED) {
				               // Language data is missing or the language is not supported.
				                Toast.makeText(context, R.string.service_msg_error_checktts_language, Toast.LENGTH_LONG);
				            }
				        } else {
				            // Initialization failed.
				        	Toast.makeText(context, R.string.service_msg_error_checktts_generic, Toast.LENGTH_LONG);
				        }
					}
				});
				return INITIALIZING;
			}
			if(ttsEngine.speak(text, TextToSpeech.QUEUE_ADD, null) == TextToSpeech.SUCCESS)
				return SUCCESS;
			else 
				return FAIL;
		}
		
		public static void kill() {
			if(ttsEngine != null) {
				ttsEngine.shutdown();
				ttsEngine = null;
			}
		}
	}
	public static class UI {
		public static void popNotification(Class intentClass, Context context, int notificationIcon, String tickerText,
				String contentTitle, String contentText, Uri sound, long[] vibrate, int flags, int defaults) {
	        String ns = Context.NOTIFICATION_SERVICE;
	        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
	        int icon = R.drawable.icon;
	        if(notificationIcon != 0){
	        	icon = notificationIcon;
	        }
	        long when = System.currentTimeMillis();

	        Notification notification = new Notification(icon, tickerText, when);
	        if(vibrate != null){
	        	notification.vibrate = vibrate;
	        }	        
	        if(sound != null){
	        	notification.sound = sound;
	        }
	        notification.defaults = defaults;
	        notification.flags = flags;
	        Intent notificationIntent = new Intent(context, intentClass);
	        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

	        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	        
	        final int HELLO_ID = 1;

	        mNotificationManager.notify(HELLO_ID, notification);
	        
	        
			
		}
	}
	public static class SimpleHttpRequest {
		public static String get(String URL){
			HttpClient httpclient = new DefaultHttpClient();
		    HttpResponse response = null;
			try {
				response = httpclient.execute(new HttpGet(URL));
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		    StatusLine statusLine = response.getStatusLine();
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        try {
					response.getEntity().writeTo(out);
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
		        return out.toString();
		    } else{
		        try {
					response.getEntity().getContent().close();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        return null;
		    }
		}
	}

	public static boolean enumSpinnerFromArray(Context context, Spinner spinnerInQuestion, String [] gamesArray){
        final Spinner gamesListSpinner = spinnerInQuestion;
        if(gamesArray != null) {
            ArrayAdapter<?> spinnerArrayAdapter = new ArrayAdapter<Object>(context,
    		        android.R.layout.simple_spinner_dropdown_item, 
    		        gamesArray);
            gamesListSpinner.setAdapter(spinnerArrayAdapter);
        } else {
        	return false;
        }
        return true;
    }
	public static void makeReadOnly(EditText textView, boolean enable){
		if(enable) {
			textView.setFilters(new InputFilter[] {
				    new InputFilter() {
						@Override
						public CharSequence filter(CharSequence src, int start, 
								int end, Spanned dst, int dstart, int dend) {
		        			return src.length() < 1 ? dst.subSequence(dstart, dend) : "";
						}
				    }
				});
		} else {
			textView.setFilters(null);
		}
	}
	
	/**
	 * Performs MD5 digest on the character string specified
	 * @param in The string to MD5 hash
	 * @return The Hashed Text
	 */
	public String md5(String in) {
		MessageDigest digest;
	    try {
	        digest = MessageDigest.getInstance("MD5");
	        digest.reset();
	        digest.update(in.getBytes());
	        byte[] a = digest.digest();
	        int len = a.length;
	        StringBuilder sb = new StringBuilder(len << 1);
	        for (int i = 0; i < len; i++) {
	            sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
	            sb.append(Character.forDigit(a[i] & 0x0f, 16));
	        }
	        return sb.toString();
	    } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
	    return null;
	}
}
