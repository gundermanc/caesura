package com.gundersoft.caesura;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

public class LiveLocation {
	Context globalContext = null;
	List<String> providersList = null;
	//LocationListener locationListener = null;
	LocationManager locationManager = null;
	
	public LiveLocation(Context context) {
		globalContext = context;
	}
	
	public boolean initProvider() {
		locationManager = (LocationManager) globalContext.getSystemService(Context.LOCATION_SERVICE);
		providersList = locationManager.getProviders(false);
		
		if(!hasGpsProvider() || !hasNetworkProvider()){
			return false;
		}

		final LocationListener locationListener = new LocationListener(){

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				Toast.makeText(globalContext, "Location Changed: " + location.getSpeed() + "; " + location.getProvider(), Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
				Toast.makeText(globalContext, provider + " disabled.", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				Toast.makeText(globalContext, provider + " enabled.", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
				Toast.makeText(globalContext, "Status Changed!", Toast.LENGTH_SHORT).show();
			}			
		};
		//locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 6000, 1, locationListener);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 6000, 1, locationListener);
		return false;
	}
	
	public boolean hasGpsProvider() {
		if(providersList.contains(LocationManager.GPS_PROVIDER))
			return true;
		else
			return false;
	}
	
	public boolean hasNetworkProvider() {
		if(providersList.contains(LocationManager.NETWORK_PROVIDER))
			return true;
		else
			return false;
	}
	
	public boolean hasPassiveProvider() {
		if(providersList.contains(LocationManager.PASSIVE_PROVIDER))
			return true;
		else
			return false;
	}
}
