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
package net.luniks.android.inetify.test;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.luniks.android.inetify.DatabaseAdapter;
import net.luniks.android.inetify.TestInfo;
import net.luniks.android.inetify.WifiLocation;
import android.database.Cursor;
import android.location.Location;

public class TestDatabaseAdapter implements DatabaseAdapter {
	
	private final Map<String, String> ignoredWifis = new ConcurrentHashMap<String, String>();
	private final Map<String, String> wifiLocations = new ConcurrentHashMap<String, String>();
	private final AtomicBoolean isOpen = new AtomicBoolean(false);
	
	private WifiLocation nearestLocation;
	private TestInfo lastTestResult;
	
	public void setNearestLocation(final WifiLocation nearestLocation) {
		this.nearestLocation = nearestLocation;
	}
	
	// Non interface method
	public void clearLocations() {
		wifiLocations.clear();
	}

	public boolean addIgnoredWifi(String bssid, String ssid) {
		isOpen.set(true);
		ignoredWifis.put(bssid, ssid);
		return true;
	}

	public boolean isIgnoredWifi(String ssid) {
		isOpen.set(true);
		return ignoredWifis.containsValue(ssid);
	}

	public boolean deleteIgnoredWifi(String ssid) {
		isOpen.set(true);
		Iterator<Entry<String, String>> it = ignoredWifis.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, String> entry = it.next();
			if(entry.getValue().equals(ssid)) {
				it.remove();
			}
		}
		return true;
	}

	// TODO Implement when needed
	public Cursor fetchIgnoredWifis() {
		isOpen.set(true);
		return null;
	}

	public boolean addLocation(String bssid, String ssid, String name, Location location) {
		isOpen.set(true);
		wifiLocations.put(bssid, ssid);
		return true;
	}

	public boolean deleteLocation(String bssid) {
		isOpen.set(true);
		return false;
	}
	
	public boolean renameLocation(String bssid, String name) {
		isOpen.set(true);
		return false;
	}

	public Cursor fetchLocations() {
		isOpen.set(true);
		return null;
	}
	
	public boolean hasLocations() {
		isOpen.set(true);
		return wifiLocations.size() > 0;
	}

	public WifiLocation getNearestLocationTo(Location location) {
		isOpen.set(true);
		return nearestLocation;
	}
	
	public boolean updateTestResult(long timestamp, int type, String subtype, boolean status) {
		isOpen.set(true);
		TestInfo info = new TestInfo();
		info.setTimestamp(timestamp);
		info.setType(type);
		info.setExtra(subtype);
		info.setIsExpectedTitle(status);
		lastTestResult = info;
		return true;
	}

	public TestInfo fetchTestResult() {
		isOpen.set(true);
		return lastTestResult;
	}

	public int getDatabaseVersion() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void close() {
		isOpen.set(false);
	}

	public boolean isOpen() {
		return isOpen.get();
	}

}
