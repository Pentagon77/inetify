/*
 * Copyright 2011 Torsten Römer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.luniks.android.inetify;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.luniks.android.impl.LocationManagerImpl;
import net.luniks.android.inetify.Locater.LocaterLocationListener;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.TwoLineListItem;

/**
 * MapActivity that shows a location (of a Wifi network) on a map
 * or finds a location and adds it to the list by broadcasting an intent to
 * LocationList.
 * 
 * @author torsten.roemer@luniks.net
 */
public class LocationMapView extends Activity {
	
	/** Action to show the location */
	public static final String SHOW_LOCATION_ACTION = "net.luniks.android.inetify.SHOW_LOCATION";
	
	/** Action to find the location */
	public static final String FIND_LOCATION_ACTION = "net.luniks.android.inetify.FIND_LOCATION";
	
	/** Id of the status view */
	public static final int ID_STATUS_VIEW = 0;
	
	/** Id of the "no location found" dialog */
	private static final int ID_NO_LOCATION_FOUND_DIALOG = 0;
	
	/** Minimum accuracy of a location in meters */
	private static final int MIN_LOCATION_ACCURACY = 100;
	
	/** Maximum age of a last known location in milliseconds */
	private static final long LOCATION_MAX_AGE = 60 * 1000;
	
	/** Timeout in milliseconds for getting a location */
	private static long GET_LOCATION_TIMEOUT = 50 * 1000;
	
	private SharedPreferences sharedPreferences;
	
	/** The map view. */
	private MapView mapView;
	
	/** Icon used as marker */
	private Drawable marker;
	
	/** TwoLineListItem showing the status */
	private TwoLineListItem statusView;
	
	/** LocateTask - retained through config changes */
	private LocateTask locateTask;
	
	// TODO Is there some way to get a reference to the "current" dialog?
	/** For testing only, read using reflection */
	@SuppressWarnings("unused")
	private volatile Dialog currentDialog = null;
	
	/**
	 * Retains the locater AsyncTask before a config change occurs.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		locateTask.setActivity(null);
		return locateTask;
	}

	/**
	 * Creates the map view.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.locationmapview);
		
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		statusView = (TwoLineListItem)this.findViewById(R.id.view_locationstatus);
		statusView.setId(ID_STATUS_VIEW);
		statusView.setBackgroundColor(this.getResources().getColor(R.color.grey_semitransparent));
		
		mapView = (MapView)findViewById(R.id.mapview_location);
		mapView.setBuiltInZoomControls(true);
		mapView.setMultiTouchControls(true);

		marker = this.getResources().getDrawable(R.drawable.marker).mutate();
		// marker.setAlpha(127);
		
		Object retained = this.getLastNonConfigurationInstance();
		if(retained == null) {
			locateTask = new LocateTask(this);
		} else {
			locateTask = (LocateTask)retained;
			locateTask.setActivity(this);
		}
	}

	/**
	 * Sets the zoomlevel from the settings and shows/finds the location.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		
		final int zoomlevel = sharedPreferences.getInt(Settings.ZOOMLEVEL, 16);
		mapView.getController().setZoom(zoomlevel);
		
		Intent intent = this.getIntent();
		if(intent != null) {
			String name = intent.getStringExtra(LocationList.EXTRA_NAME);
			if(intent.getAction().equals(SHOW_LOCATION_ACTION)) {
				Location location = intent.getParcelableExtra(LocationList.EXTRA_LOCATION);
				updateLocation(name, location, locateTask.getLocateStatus());
			} else if(intent.getAction().equals(FIND_LOCATION_ACTION)) {
				findLocation(name);
			}
		}
	}

	/**
	 * Saves the current zoomlevel to the shared preferences.
	 */
	@Override
	protected void onStop() {
		final int zoomlevel = mapView.getZoomLevel();
		sharedPreferences.edit().putInt(Settings.ZOOMLEVEL, zoomlevel).commit();
		
		super.onStop();
	}

	/**
	 * Creates the dialogs managed by this activity.
	 */
	@Override
	protected Dialog onCreateDialog(final int id) {
		Dialog dialog = super.onCreateDialog(id);
		if(id == ID_NO_LOCATION_FOUND_DIALOG) {
			dialog = Dialogs.createOKDialog(this, ID_NO_LOCATION_FOUND_DIALOG,
					this.getString(R.string.locationmapview_location), 
					this.getString(R.string.locationmapview_could_not_get_accurate_location));
		}
		this.currentDialog = dialog;
		return dialog;
	}
	
	/**
	 * Cancels finding the location when the user presses the back button.
	 */
	@Override
	public void onBackPressed() {
		this.locateTask.cancel(false);
		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		currentDialog = null;
		super.onDestroy();
	}
	
	/**
	 * Starts the AsyncTask to find the location of the Wifi with the given name
	 * if it is not already running, and updates the current location otherwise.
	 */
	private void findLocation(final String name) {
		if (locateTask.getLocateStatus() != LocateTask.LocateStatus.PENDING) {
			this.updateLocation(name, locateTask.getCurrentLocation(),
					locateTask.getLocateStatus());
		} else {
			if(name != null) {
				this.setTitle(this.getString(R.string.locationmapview_label_name, name));
			}
			locateTask.execute(new Void[0]);
		}
	}

	
	/**
	 * Moves the marker and the map to the given location, shows the given name in the title if it
	 * is not null, and shows status information depending on the given location and status.
	 * @param name
	 * @param location
	 * @param status
	 */
	private void updateLocation(final String name, final Location location, final LocateTask.LocateStatus status) {
				
		if(name != null) {
			this.setTitle(this.getString(R.string.locationmapview_label_name, name));
		}
		
		if(status == LocateTask.LocateStatus.PENDING) {
			showStatus("", "", View.GONE);
		} else if(status == LocateTask.LocateStatus.NOTFOUND) {
			String status1 = this.getString(R.string.locationmapview_status1_notfound);
			String status2 = this.getString(R.string.locationmapview_status2_notfound);
			showStatus(status1, status2, View.VISIBLE);
		} else if(status == LocateTask.LocateStatus.WAITING) {
			String status1 = this.getString(R.string.locationmapview_status1_searching);
			String status2 = this.getString(R.string.locationmapview_status2_waiting);
			showStatus(status1, status2, View.VISIBLE);
		} else if(status == LocateTask.LocateStatus.UPDATING) {
			String status1 = this.getString(R.string.locationmapview_status1_searching);
			String status2 = "";
			if(location != null) {
				status2 = this.getString(R.string.locationmapview_status2_current, 
						Utils.getLocalizedRoundedMeters(location.getAccuracy()));
			}
			showStatus(status1, status2, View.VISIBLE);
		} else if(status == LocateTask.LocateStatus.FOUND) {
			String status1 = this.getString(R.string.locationmapview_status1_found);
			String status2 = "";
			if(location != null) {
				status2 = this.getString(R.string.locationmapview_status2_current, 
						Utils.getLocalizedRoundedMeters(location.getAccuracy()));
			}
			showStatus(status1, status2, View.VISIBLE);
		}
		
		if(location != null) {
			final Double latE6 = location.getLatitude() * 1E6;
			final Double lonE6 = location.getLongitude() * 1E6;
			
			final GeoPoint point = new GeoPoint(latE6.intValue(), lonE6.intValue());
			
			final OverlayItem overlayItem = new OverlayItem("Inetify", "Location", point);
			overlayItem.setMarker(marker);
			
			final ArrayList<OverlayItem> mapOverlays = new ArrayList<OverlayItem>();
			mapOverlays.add(overlayItem);
			
			final ItemizedIconOverlay<OverlayItem> iconOverlay = new ItemizedIconOverlay<OverlayItem>(this, mapOverlays, null);
			this.mapView.getOverlays().clear();
	        this.mapView.getOverlays().add(iconOverlay);
			
	        this.mapView.getController().animateTo(point);
		}
	}
	
	/**
	 * Shows the given two status messages status and changes the visibility to the given value.
	 * @param status1
	 * @param status2
	 * @param visibility
	 */
	private void showStatus(final String status1, final String status2, final int visibility) {
		statusView.getText1().setText(status1, TextView.BufferType.NORMAL);
		statusView.getText2().setText(status2, TextView.BufferType.NORMAL);
		if(statusView.getVisibility() != visibility) {
			statusView.setVisibility(visibility);
		}
	}
	
	/**
	 * AsyncTask that starts the Locater, listens for location updates and updates the location
	 * when it receives a location update. Stops when it has received a location with
	 * MIN_LOCATION_ACCURACY or when cancelled, and shows a "No accurate location found" dialog
	 * if it did not receive an accurate enough location within GET_LOCATION_TIMEOUT.
	 * 
	 * @author torsten.roemer@luniks.net
	 */
    private static class LocateTask extends AsyncTask<Void, Location, Void> implements LocaterLocationListener {
    	
    	private final AtomicBoolean found = new AtomicBoolean(false);
    	private final CountDownLatch latch = new CountDownLatch(1);
    	
		private Locater locater;
    	private LocationMapView activity;
    	
    	private volatile Location currentLocation = null;
    	private volatile LocateStatus status = LocateStatus.PENDING;
    	
    	private LocateTask(final LocationMapView activity) {
    		this.activity = activity;
    		this.locater = new LocaterImpl(
    				new LocationManagerImpl((LocationManager)activity.getSystemService(LOCATION_SERVICE)));
    	}
    	
    	private void setActivity(final LocationMapView activity) {
    		this.activity = activity;
    	}
    	
    	private Location getCurrentLocation() {
    		return this.currentLocation;
    	}
    	
    	private LocateStatus getLocateStatus() {
    		return status;
    	}
    	
		public void onLocationChanged(final Location location) {
			this.currentLocation = location;
			
			if(locater.isAccurateEnough(location, MIN_LOCATION_ACCURACY)) {
				this.found.set(true);
				latch.countDown();
			} else {
				publishProgress(location);
			}
		}

		@Override
		protected void onPreExecute() {
			// Set minAccuracy to Integer.MAX_VALUE so the locater accepts any location
			// and keeps on listening for location updates until it is stopped.
			locater.start(this, LOCATION_MAX_AGE, Integer.MAX_VALUE, true);
			
			Location initialLocation = locater.getBestLastKnownLocation(Long.MAX_VALUE);
			if(initialLocation == null) {
				initialLocation = new Location(LocationManager.NETWORK_PROVIDER);
			}
			status = LocateStatus.WAITING;
			activity.updateLocation(null, initialLocation, status);
		}

		@Override
		protected void onCancelled() {
			locater.stop();
			latch.countDown();
		}

		@Override
		protected void onProgressUpdate(Location... values) {
			status = LocateStatus.UPDATING;
			activity.updateLocation(null, values[0], status);
		}

		@Override
		protected Void doInBackground(final Void... arg) {			
			try {
				latch.await(GET_LOCATION_TIMEOUT, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// Ignore
			}
			return null;
		}
		
		@Override
	    protected void onPostExecute(final Void result) {
			locater.stop();
			
			if(found.get()) {
				Intent intent = new Intent();
				intent.setAction(LocationList.ADD_LOCATION_ACTION);
				intent.putExtra(LocationList.EXTRA_LOCATION, currentLocation);
				activity.sendBroadcast(intent);
				
				status = LocateStatus.FOUND;
				activity.updateLocation(null, currentLocation, status);
				
				// Log.d(Inetify.LOG_TAG, String.format("Sent broadcast: %s", intent));
			} else {
				status = LocateStatus.NOTFOUND;
				activity.updateLocation(null, null, status);
				activity.showDialog(ID_NO_LOCATION_FOUND_DIALOG);
			}
	    }
		
	    /**
	     * Status of the process finding the location.
	     * 
	     * @author torsten.roemer@luniks.net
	     */
	    private static enum LocateStatus {
	    	PENDING, WAITING, UPDATING, FOUND, NOTFOUND
	    }
		
    }

}
