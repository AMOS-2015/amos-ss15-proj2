/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.activities;


import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.google.inject.Key;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import roboguice.RoboGuice;
import roboguice.activity.event.OnActivityResultEvent;
import roboguice.activity.event.OnContentChangedEvent;
import roboguice.activity.event.OnNewIntentEvent;
import roboguice.activity.event.OnPauseEvent;
import roboguice.activity.event.OnRestartEvent;
import roboguice.activity.event.OnResumeEvent;
import roboguice.activity.event.OnSaveInstanceStateEvent;
import roboguice.activity.event.OnStopEvent;
import roboguice.context.event.OnConfigurationChangedEvent;
import roboguice.context.event.OnCreateEvent;
import roboguice.context.event.OnDestroyEvent;
import roboguice.context.event.OnStartEvent;
import roboguice.event.EventManager;
import roboguice.inject.ContentViewListener;
import roboguice.inject.RoboInjector;
import roboguice.util.RoboContext;


/**
 * Combines {@link MaterialNavigationDrawer} with an {@link roboguice.activity.RoboActionBarActivity}.
 * Code has been copied from https://github.com/roboguice/roboguice/blob/master/roboguice/src/main/java/roboguice/activity/RoboActionBarActivity.java.
 * <p/>
 * The sole purpose of this class is to provide DI and should hence be ignored for the most part ...
 */
abstract class AbstractRoboDrawerActivity extends MaterialNavigationDrawer<Fragment> implements RoboContext {

	protected EventManager eventManager;
	protected HashMap<Key<?>, Object> scopedObjects = new HashMap<Key<?>, Object>();

	@Inject
	ContentViewListener ignored; // BUG find a better place to put this

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		final RoboInjector injector = RoboGuice.getInjector(this);
		eventManager = injector.getInstance(EventManager.class);
		injector.injectMembersWithoutViews(this);
		super.onCreate(savedInstanceState);
		eventManager.fire(new OnCreateEvent<Activity>(this, savedInstanceState));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		eventManager.fire(new OnSaveInstanceStateEvent(this, outState));
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		eventManager.fire(new OnRestartEvent(this));
	}

	@Override
	protected void onStart() {
		super.onStart();
		eventManager.fire(new OnStartEvent<Activity>(this));
	}

	@Override
	protected void onResume() {
		super.onResume();
		eventManager.fire(new OnResumeEvent(this));
	}

	@Override
	protected void onPause() {
		super.onPause();
		eventManager.fire(new OnPauseEvent(this));
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		eventManager.fire(new OnNewIntentEvent(this));
	}

	@Override
	protected void onStop() {
		try {
			eventManager.fire(new OnStopEvent(this));
		} finally {
			super.onStop();
		}
	}

	@Override
	protected void onDestroy() {
		try {
			eventManager.fire(new OnDestroyEvent<Activity>(this));
		} finally {
			try {
				RoboGuice.destroyInjector(this);
			} finally {
				super.onDestroy();
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final Configuration currentConfig = getResources().getConfiguration();
		super.onConfigurationChanged(newConfig);
		eventManager.fire(new OnConfigurationChangedEvent<Activity>(this, currentConfig, newConfig));
	}

	@Override
	public void onSupportContentChanged() {
		super.onSupportContentChanged();
		RoboGuice.getInjector(this).injectViewMembers(this);
		eventManager.fire(new OnContentChangedEvent(this));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		eventManager.fire(new OnActivityResultEvent(this, requestCode, resultCode, data));
	}

	@Override
	public Map<Key<?>, Object> getScopedObjectMap() {
		return scopedObjects;
	}

}