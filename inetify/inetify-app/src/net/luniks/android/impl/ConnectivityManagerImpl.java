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
package net.luniks.android.impl;

import net.luniks.android.interfaces.IConnectivityManager;
import net.luniks.android.interfaces.INetworkInfo;
import android.net.ConnectivityManager;

/**
 * Implementation of IConnectivityManager.
 * @see android.net.ConnectivityManager
 * 
 * @author torsten.roemer@luniks.net
 */
public class ConnectivityManagerImpl implements IConnectivityManager {
	
	private final ConnectivityManager connectivityManager;
	
	public ConnectivityManagerImpl(final ConnectivityManager connectivityManager) {
		this.connectivityManager = connectivityManager;
	}

	/**
	 * Returns the wrapped active NetworkInfo from the wrapped ConnectivityManager,
	 * null if it was null.
	 */
	public INetworkInfo getActiveNetworkInfo() {
		return NetworkInfoImpl.getInstance(connectivityManager.getActiveNetworkInfo());
	}

}
