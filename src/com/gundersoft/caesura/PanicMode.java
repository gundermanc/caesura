package com.gundersoft.caesura;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.gundersoft.caesura.Tools.Torch;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

/**
 * Makes phone Panic, ring, and Flash, if supported.
 * @author Christian Gunderman
 */
public class PanicMode {
	/** The time interval, in ms at which the flash will strobe */
	private static int strobeLightInterval = 200;
	/** The MediaPlayer Instance used to play the ring tone */
	MediaPlayer mMediaPlayer = null;
	/** The title for the Panic Mode window */
	private String pmTitle = "";
	/** The Text for the Panic mode window */
	private String pmText = "";
	private static Timer eventTimer = new Timer();
	private static Torch strobeLight = null;
	public PanicMode(boolean useStrobe,  boolean playSound) {
		//Set up Strobe
		if(useStrobe) {
			strobeLight = new Torch();
			strobeLight.initTorch();
		}
		if(playSound) {
			mMediaPlayer = new MediaPlayer();
		}
	}
	public void setBannerText(String title, String text) {
		pmTitle = title;
		pmText = text;
	}
	public void setStrobeInterval(int interval) {
		strobeLightInterval = interval;
	}
	public void activatePanic(Application application, Context context, boolean activate, boolean displayBanner){
		if(activate){
			if(strobeLight != null) {
				eventTimer.scheduleAtFixedRate(new TimerTask() {
	
					@Override
					public void run() {
			        	if(!strobeLight.isTorchOn()) {
			        		strobeLight.activateTorch(true);
			        	} else {
			        		strobeLight.activateTorch(false);
			        	}
						
					}
		        	
		        }, 0, strobeLightInterval);
			}
			if(mMediaPlayer != null) {
				Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			     try {
					mMediaPlayer.setDataSource(context, alert);
				} catch (IllegalArgumentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SecurityException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IllegalStateException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			     final AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
			     audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 
			    		 audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
			     {
			    	 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
			    	 mMediaPlayer.setLooping(true);
			    	 try {
						mMediaPlayer.prepare();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    	 mMediaPlayer.start();
			      }
			}
			if(displayBanner) {
				 Intent panicModeBannerIntent = new Intent(context, PanicBannerActivity.class);
			     panicModeBannerIntent.putExtra("PM_TITLE", pmTitle);
			     panicModeBannerIntent.putExtra("PM_TEXT", pmText);
			     panicModeBannerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			     context.startActivity(panicModeBannerIntent); 
			     PanicBannerActivity.registerPanicMode(this);
			}
		    
		} else {
			//Kill strobe
			eventTimer.cancel();
			eventTimer.purge();
			
			//Kill Ringer
			mMediaPlayer.stop();
			
		}
	}	
	public void closePanicMode() {
		if(strobeLight != null) {
			strobeLight.closeTorch();
		}
		if(eventTimer != null) {
			eventTimer.cancel();
			eventTimer.purge();
		}
		if(mMediaPlayer != null) {
			mMediaPlayer.release();
		}
	}
}
