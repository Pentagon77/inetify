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

import net.luniks.android.inetify.DatabaseAdapterImpl;
import net.luniks.android.inetify.WifiLocation;
import android.database.Cursor;
import android.location.Location;
import android.test.AndroidTestCase;

public class DatabaseAdapterImplLocationListTest extends AndroidTestCase {
	
	public void setUp() throws Exception {
		super.setUp();
		this.getContext().deleteDatabase("inetifydb");
		this.getContext().deleteDatabase("inetifydb-journal");
	}
	
	public void testDatabaseNotOpen() {
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		assertFalse(adapter.isOpen());
	}
	
	public void testAddLocationOpensDatabase() {
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		adapter.addLocation("00:21:29:A2:48:80", "Celsten", "Celsten", TestUtils.createLocation(0.1, 0.1, 10));
		
		assertTrue(adapter.isOpen());
		
		adapter.close();
		
		assertFalse(adapter.isOpen());
	}
	
	public void testDeleteLocationOpensDatabase() {
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		adapter.deleteLocation("00:21:29:A2:48:80");
		
		assertTrue(adapter.isOpen());
		
		adapter.close();
		
		assertFalse(adapter.isOpen());
	}
	
	public void testRenameLocationOpensDatabase() {
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		adapter.renameLocation("00:21:29:A2:48:80", "CelstenName");
		
		assertTrue(adapter.isOpen());
		
		adapter.close();
		
		assertFalse(adapter.isOpen());
	}
	
	public void testFetchLocationsWifisOpensDatabase() {
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		adapter.fetchLocations();
		
		assertTrue(adapter.isOpen());
		
		adapter.close();
		
		assertFalse(adapter.isOpen());
	}
	
	public void testHasLocationsOpensDatabase() {
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		adapter.hasLocations();
		
		assertTrue(adapter.isOpen());
		
		adapter.close();
		
		assertFalse(adapter.isOpen());
	}
	
	public void testGetNearestLocationToOpensDatabase() {
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		adapter.getNearestLocationTo(null);
		
		assertTrue(adapter.isOpen());
		
		adapter.close();
		
		assertFalse(adapter.isOpen());
	}
	
	public void testAddLocation() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		assertTrue(adapter.addLocation("00:21:29:A2:48:80", "Celsten", "CelstenName", TestUtils.createLocation(0.1, 0.1, 10)));
		assertTrue(adapter.addLocation("00:11:22:33:44:55", "TestSSID1", "", TestUtils.createLocation(0.2, 0.2, 20)));
		assertTrue(adapter.addLocation("00:66:77:88:99:00", "TestSSID2", null, TestUtils.createLocation(0.3, 0.3, 30)));
		assertFalse(adapter.addLocation(null, null, null, null));

		Cursor cursor = adapter.fetchLocations();
		
		assertEquals(3, cursor.getCount());
		assertTrue(cursor.moveToNext());
		assertEquals("00:21:29:A2:48:80", cursor.getString(1));
		assertEquals("Celsten", cursor.getString(2));
		assertEquals("CelstenName", cursor.getString(3));
		assertEquals(0.1, cursor.getDouble(4));
		assertEquals(0.1, cursor.getDouble(5));
		assertEquals(10, cursor.getInt(6));
		assertTrue(cursor.moveToNext());
		assertEquals("00:11:22:33:44:55", cursor.getString(1));
		assertEquals("TestSSID1", cursor.getString(2));
		assertEquals("TestSSID1", cursor.getString(3));
		assertEquals(0.2, cursor.getDouble(4));
		assertEquals(0.2, cursor.getDouble(5));
		assertEquals(20, cursor.getInt(6));
		assertTrue(cursor.moveToNext());
		assertEquals("00:66:77:88:99:00", cursor.getString(1));
		assertEquals("TestSSID2", cursor.getString(2));
		assertEquals("TestSSID2", cursor.getString(3));
		assertEquals(0.3, cursor.getDouble(4));
		assertEquals(0.3, cursor.getDouble(5));
		assertEquals(30, cursor.getInt(6));
		assertFalse(cursor.moveToNext());
		
		cursor.close();
		adapter.close();
	}
	
	public void testAddLocationUpdatesExistingRenamedLocation() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		assertTrue(adapter.addLocation("00:11:22:33:44:55", "TestSSID", "TestName", TestUtils.createLocation(0.3, 0.3, 30)));
		
		assertTrue(adapter.renameLocation("00:11:22:33:44:55", "TestRenamed"));
		
		assertTrue(adapter.addLocation("00:11:22:33:44:55", "TestSSID", "TestName", TestUtils.createLocation(0.9, 0.9, 90)));
		
		Cursor cursor = adapter.fetchLocations();
		
		assertEquals(1, cursor.getCount());
		assertTrue(cursor.moveToNext());
		assertEquals("00:11:22:33:44:55", cursor.getString(1));
		assertEquals("TestSSID", cursor.getString(2));
		assertEquals("TestRenamed", cursor.getString(3));
		assertEquals(0.9, cursor.getDouble(4));
		assertEquals(0.9, cursor.getDouble(5));
		assertEquals(90, cursor.getInt(6));
		assertFalse(cursor.moveToNext());
		
		cursor.close();
		adapter.close();
	}
	
	public void testDeleteLocation() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		insertTestLocations(adapter);
		
		assertTrue(adapter.deleteLocation("00:11:22:33:44:55"));
		assertFalse(adapter.deleteLocation("xx:xx:xx:xx:xx:xx"));
		assertFalse(adapter.deleteLocation(null));
		
		Cursor cursor = adapter.fetchLocations();
		
		assertEquals(2, cursor.getCount());
		assertTrue(cursor.moveToNext());
		assertEquals("00:21:29:A2:48:80", cursor.getString(1));
		assertEquals("Celsten", cursor.getString(2));
		assertTrue(cursor.moveToNext());
		assertEquals("00:66:77:88:99:00", cursor.getString(1));
		assertEquals("TestSSID2", cursor.getString(2));
		assertFalse(cursor.moveToNext());
		
		cursor.close();
		adapter.close();
	}
	
	public void testRenameLocation() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		insertTestLocations(adapter);
		
		assertTrue(adapter.renameLocation("00:11:22:33:44:55", "Test1Renamed"));
		
		Cursor cursor = adapter.fetchLocations();
		
		assertEquals(3, cursor.getCount());
		assertTrue(cursor.moveToNext());
		assertEquals("00:21:29:A2:48:80", cursor.getString(1));
		assertEquals("Celsten", cursor.getString(2));
		assertEquals("Celsten", cursor.getString(3));
		assertTrue(cursor.moveToNext());
		assertEquals("00:11:22:33:44:55", cursor.getString(1));
		assertEquals("TestSSID1", cursor.getString(2));
		assertEquals("Test1Renamed", cursor.getString(3));
		assertTrue(cursor.moveToNext());
		assertEquals("00:66:77:88:99:00", cursor.getString(1));
		assertEquals("TestSSID2", cursor.getString(2));
		assertEquals("Test2", cursor.getString(3));
		assertFalse(cursor.moveToNext());
		
		cursor.close();
		adapter.close();
	}
	
	public void testRenameLocationNameNull() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		assertTrue(adapter.addLocation("00:21:29:A2:48:80", "Celsten", "CelstenName", TestUtils.createLocation(0.1, 0.1, 10)));
		
		assertFalse(adapter.renameLocation("00:21:29:A2:48:80", null));
		
		Cursor cursor = adapter.fetchLocations();
		
		assertEquals(1, cursor.getCount());
		assertTrue(cursor.moveToNext());
		assertEquals("CelstenName", cursor.getString(3));
		assertFalse(cursor.moveToNext());
		
		cursor.close();
		adapter.close();
	}
	
	public void testRenameLocationNameEmpty() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		assertTrue(adapter.addLocation("00:21:29:A2:48:80", "Celsten", "CelstenName", TestUtils.createLocation(0.1, 0.1, 10)));
		
		assertFalse(adapter.renameLocation("00:21:29:A2:48:80", ""));
		
		Cursor cursor = adapter.fetchLocations();
		
		assertEquals(1, cursor.getCount());
		assertTrue(cursor.moveToNext());
		assertEquals("CelstenName", cursor.getString(3));
		assertFalse(cursor.moveToNext());
		
		cursor.close();
		adapter.close();
	}
	
	public void testRenameLocationNameLongerThan32Chars() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		assertTrue(adapter.addLocation("00:21:29:A2:48:80", "Celsten", "CelstenName", TestUtils.createLocation(0.1, 0.1, 10)));
		
		assertTrue(adapter.renameLocation("00:21:29:A2:48:80", String.valueOf(new char[33])));
		
		Cursor cursor = adapter.fetchLocations();
		
		assertEquals(1, cursor.getCount());
		assertTrue(cursor.moveToNext());
		assertEquals(32, cursor.getString(3).length());
		assertFalse(cursor.moveToNext());
		
		cursor.close();
		adapter.close();
	}
	
	public void testFetchLocations() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		insertTestLocations(adapter);
		
		Cursor cursor = adapter.fetchLocations();
		
		assertEquals(3, cursor.getCount());
		assertTrue(cursor.moveToNext());
		assertEquals("00:21:29:A2:48:80", cursor.getString(1));
		assertEquals("Celsten", cursor.getString(2));
		assertTrue(cursor.moveToNext());
		assertEquals("00:11:22:33:44:55", cursor.getString(1));
		assertEquals("TestSSID1", cursor.getString(2));
		assertTrue(cursor.moveToNext());
		assertEquals("00:66:77:88:99:00", cursor.getString(1));
		assertEquals("TestSSID2", cursor.getString(2));
		assertFalse(cursor.moveToNext());
		
		cursor.close();
		adapter.close();
	}
	
	public void testFetchLocationsOrderedByName() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		adapter.addLocation("01:00:00:00:00:00", "TestSSID1", "b", TestUtils.createLocation(0.1, 0.1, 10));
		adapter.addLocation("02:00:00:00:00:00", "TestSSID2", "1", TestUtils.createLocation(0.2, 0.2, 20));
		adapter.addLocation("03:00:00:00:00:00", "TestSSID3", "ä", TestUtils.createLocation(0.3, 0.3, 30));
		adapter.addLocation("04:00:00:00:00:00", "TestSSID4", "C", TestUtils.createLocation(0.4, 0.4, 40));
		adapter.addLocation("05:00:00:00:00:00", "TestSSID5", "2", TestUtils.createLocation(0.5, 0.5, 50));
		
		Cursor cursor = adapter.fetchLocations();
		
		assertEquals(5, cursor.getCount());
		assertTrue(cursor.moveToNext());
		assertEquals("02:00:00:00:00:00", cursor.getString(1));
		assertEquals("1", cursor.getString(3));
		assertTrue(cursor.moveToNext());
		assertEquals("05:00:00:00:00:00", cursor.getString(1));
		assertEquals("2", cursor.getString(3));
		assertTrue(cursor.moveToNext());
		assertEquals("03:00:00:00:00:00", cursor.getString(1));
		assertEquals("ä", cursor.getString(3));
		assertTrue(cursor.moveToNext());
		assertEquals("01:00:00:00:00:00", cursor.getString(1));
		assertEquals("b", cursor.getString(3));
		assertTrue(cursor.moveToNext());
		assertEquals("04:00:00:00:00:00", cursor.getString(1));
		assertEquals("C", cursor.getString(3));
		assertFalse(cursor.moveToNext());
		
		cursor.close();
		adapter.close();
	}
	
	public void testHasLocationsNone() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		assertFalse(adapter.hasLocations());
		
		adapter.close();
	}
	
	public void testHasLocations() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		insertTestLocations(adapter);
		
		assertTrue(adapter.hasLocations());
		
		adapter.close();
	}
	
	public void testGetNearestLocationToNull() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		WifiLocation wifiLocation = adapter.getNearestLocationTo(null);
		
		assertNull(wifiLocation);
		
		adapter.close();
	}
	
	public void testGetNearestLocationToHasNone() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		WifiLocation wifiLocation = adapter.getNearestLocationTo(new Location("test"));
		
		assertNull(wifiLocation);
		
		adapter.close();
	}
	
	public void testGetNearestLocationTo() {
		
		DatabaseAdapterImpl adapter = new DatabaseAdapterImpl(this.getContext());
		
		adapter.addLocation("BSSID1", "SSID1", "Name1", TestUtils.createLocation(50, 3, 10));
		adapter.addLocation("BSSID2", "SSID2", "Name2", TestUtils.createLocation(50.5, 3.5, 10));
		adapter.addLocation("BSSID3", "SSID3", "Name3", TestUtils.createLocation(51, 4, 10));
		
		Location location = new Location("test");
		location.setLatitude(50.628707);
		location.setLongitude(3.538688);
		location.setAccuracy(0);
		
		WifiLocation wifiLocation = adapter.getNearestLocationTo(location);
		
		assertNotNull(wifiLocation);
		assertEquals("Name2", wifiLocation.getName());
		assertEquals(15, Math.round(wifiLocation.getDistance() / 1000));
		
		adapter.close();
	}
	
	private void insertTestLocations(final DatabaseAdapterImpl adapter) {
		adapter.addLocation("00:21:29:A2:48:80", "Celsten", "Celsten", TestUtils.createLocation(0.1, 0.1, 10));
		adapter.addLocation("00:11:22:33:44:55", "TestSSID1", "Test1", TestUtils.createLocation(0.2, 0.2, 20));
		adapter.addLocation("00:66:77:88:99:00", "TestSSID2", "Test2", TestUtils.createLocation(0.3, 0.3, 30));
	}

}
