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

import net.luniks.android.impl.ConnectivityManagerImpl;
import net.luniks.android.impl.NotificationManagerImpl;
import net.luniks.android.impl.WifiManagerImpl;
import net.luniks.android.interfaces.IWifiInfo;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;

/**
 * IntentService that is started by ConnectivityActionReceiver when Wifi connects
 * or disconnects, performs the internet connectivity test and creates or cancels
 * the notifications. If the service receives an intent while is busy testing internet
 * connectivity it cancels the test and starts a new test run. 
 * 
 * @author torsten.roemer@luniks.net
 */
public class InetifyIntentService extends IntentService {
	
	/** Delay before/between each (re)try to test internet connectivity */
	public static final int TEST_DELAY_SECS = 10;
	
	/** Number of retries to test internet connectivity */
	private static final int TEST_RETRIES = 3;
	
	/** Tag of the wake lock */
	public static final String WAKE_LOCK_TAG = "net.luniks.android.inetify.InetifyIntentService";
	
	/** Wake lock kept until the test is done */
	static volatile PowerManager.WakeLock wakeLock;
	
	/** UI thread handler */
	private Handler handler;
	
	/** Tester */
	private Tester tester;
	
	/** Notifier */
	private Notifier notifier;
	
	/** Database adapter */
	private DatabaseAdapter databaseAdapter;
	
	/**
	 * Creates an instance with a name.
	 */
	public InetifyIntentService() {
		super("InetifyIntentService");
		this.setIntentRedelivery(true);
	}

	/**
	 * Performs initialization.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		this.handler = new Handler();
		if(tester == null) {
			tester = new TesterImpl(this,
					new ConnectivityManagerImpl((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)), 
					new WifiManagerImpl((WifiManager)getSystemService(WIFI_SERVICE)),
					new TitleVerifierImpl());
		}
		if(notifier == null) {
			notifier = new NotifierImpl(this,
					new NotificationManagerImpl((NotificationManager)getSystemService(NOTIFICATION_SERVICE)));
		}
		if(databaseAdapter == null) {
			databaseAdapter = new DatabaseAdapterImpl(this);
		}
	}

	/**
	 * Overridden to cancel a possibly ongoing internet connectivity test so the next
	 * one can be started instead.
	 * NOTE: ServiceTestCase and pre 1.5 API call onStart()!
	 * @see android.app.IntentService#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		cancelTester();
		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * Cancels a possibly ongoing internet connectivity test and
	 * closes the database adapter.
	 */
	@Override
	public void onDestroy() {
		cancelTester();
		databaseAdapter.close();
	}

	/**
	 * Acquires a wake lock in case of intent redelivery, does the work and
	 * releases the wake lock.
	 */
	@Override
	protected void onHandleIntent(final Intent intent) {
				
		// Log.d(Inetify.LOG_TAG, String.format("InetifyIntentService onHandleIntent called with intent: %s", intent));
		
		try {
			acquireWakeLockIfNeeded(this);
			
			if(intent != null) {
				boolean wifiConnected = intent.getBooleanExtra(ConnectivityActionReceiver.EXTRA_IS_WIFI_CONNECTED, false);
				test(wifiConnected);
			}
		} catch(Exception e) {
			// Log.w(Inetify.LOG_TAG, String.format("Test threw exception: %s", e.getMessage()));
		} finally {
			if(wakeLock != null) {
				if(wakeLock.isHeld()) {
					wakeLock.release();
					
					// Log.d(Inetify.LOG_TAG, String.format("Released wake lock"));
				}
				wakeLock = null;
			}
		}
	}
	
	/**
	 * Creates a new wake lock and acquires it if the current one is null.
	 * @param context
	 */
	synchronized private static void acquireWakeLockIfNeeded(final Context context) {
		if(wakeLock == null) {
			PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
		}
		if(! wakeLock.isHeld()) {
			wakeLock.acquire();
			
			// Log.d(Inetify.LOG_TAG, String.format("Acquired wake lock since it was null"));
		}
	}
	
	/**
	 * Runs an internet connectivity test if wifiConnected is true, clears an
	 * existing notification otherwise.
	 */	
	private void test(final boolean wifiConnected) {
		/*
		 * Ignore if Wifi says it connected or disconnected, as when moving from one neighbouring Wifi to another (roaming?),
		 * it seems the sequence can be:
		 * 1. Connect to "new" Wifi
		 * 2. Disconnect from "old" Wifi
		 * So we just check the actual state of Wifi connection.
		 */
		// Log.d(Inetify.LOG_TAG, "InetifyIntentService.test() called");		
		IWifiInfo wifiInfo = tester.getWifiInfo();
		if(wifiInfo != null && databaseAdapter.isIgnoredWifi(wifiInfo.getSSID())) {
			// Log.d(Inetify.LOG_TAG, String.format("Wifi %s is connected but ignored, skipping test", wifiInfo.getSSID()));
			return;
		} else {
			TestInfo info = tester.testWifi(TEST_RETRIES, TEST_DELAY_SECS);
			
			databaseAdapter.updateTestResult(info.getTimestamp(), info.getType(), info.getExtra(), info.getIsExpectedTitle());
			this.sendBroadcast(new Intent(Inetify.UPDATE_TESTRESULT_ACTION));
			
			// Log.d(Inetify.LOG_TAG, String.format("Updated test results in database: %s", info));
			
			handler.post(new InetifyRunner(info));
		}
	}
	
	/**
	 * Cancelling the tester, catching any exception it may throw.
	 */
	private void cancelTester() {
		try {
			tester.cancel();
		} catch(Exception e) {
			// Log.w(Inetify.LOG_TAG, String.format("Cancelling test threw exception: %s", e.getMessage()));
		}
	}
	
	/**
	 * Runnable that calls inetify(TestInfo) with the given TestInfo.
	 * A null TestInfo causes any existing notification to be cancelled.
	 * 
	 * @author torsten.roemer@luniks.net
	 */
	private class InetifyRunner implements Runnable {
		
		private final TestInfo info;
		
		public InetifyRunner(final TestInfo info) {
			this.info = info;
		}
		
		public void run() {
			notifier.inetify(info);
		}		
	}

}
