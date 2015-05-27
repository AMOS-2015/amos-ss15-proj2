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

package org.croudtrip.location;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.croudtrip.R;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.utils.LifecycleHandler;

import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import roboguice.receiver.RoboBroadcastReceiver;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by Frederik Simon on 17.05.2015.
 */
public class LocationUploadTimerReceiver extends RoboBroadcastReceiver {

    private static final int NOTIFICATION_UPLOAD_TIMER = 654;

    private final float MIN_ACCURACY = 200.0f;
    private final int MAX_FAILURE_COUNT = 3;

    @Inject
    private LocationUpdater locationUpdater;

    @Inject
    private TripsResource tripsResource;
    private static AtomicInteger failureCount =  new AtomicInteger();

    @Override
    protected void handleReceive( final Context context, Intent intent ) {
        tripsResource.getOffers(false)
                .subscribe(new Action1<List<TripOffer>>() {
                               @Override
                               public void call(List<TripOffer> tripOffers) {

                                   Location location = locationUpdater.getLastLocation();

                                   if( location == null ) {
                                       // null is not good, but happens on startup, so that's okay.
                                       Timber.e("No Update of location was possible, since location was null");
                                       return;
                                   }

                                   Timber.d("Your location accuracy is " + location.getAccuracy());

                                   if( location.getAccuracy() > MIN_ACCURACY ) {
                                       Timber.e("Your location is not accurate enough: " + location.getAccuracy());
                                       handleError(context);
                                       return;
                                   }

                                   RouteLocation routeLocation = new RouteLocation( location.getLatitude(), location.getLongitude() );

                                   for (final TripOffer offer : tripOffers) {

                                       // TODO: There should only be one offer
                                       TripOfferUpdate offerUpdate = new TripOfferUpdate( routeLocation );
                                       tripsResource.updateOffer(offer.getId(), offerUpdate)
                                               .subscribe( new Action1<TripOffer>() {
                                                   @Override
                                                   public void call(TripOffer tripOffer) {
                                                       Timber.d("Updated your location on the server for offer " + tripOffer.getId());
                                                       handleSuccess(context);
                                                   }
                                               }, new Action1<Throwable>() {
                                                   @Override
                                                   public void call(Throwable throwable) {
                                                       Timber.e("Was not able to update your location on the server " + offer.getId() + " : " + throwable.getMessage());
                                                       handleError(context);
                                                   }
                                               });
                                   }
                               }
                           },
                        new Action1<Throwable>(){

                            @Override
                            public void call(Throwable throwable) {
                                Timber.e("Was not able to update your location on the server. Could not download your offers: " + throwable.getMessage());
                                handleError(context);
                            }
                        });
    }


    private void handleSuccess( Context context ) {
        int fc = failureCount.getAndSet(0);
        Timber.d("Failure Count is " + fc);
        if( fc >= MAX_FAILURE_COUNT ){
            // show a notification that no position upload was possible
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            Notification notification  =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_directions_car_white)
                            .setContentTitle(context.getString(R.string.success_position_update_title))
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.success_position_update_message)))
                            .setContentText(context.getString(R.string.success_position_update_message))
                            .build();

            notification.flags |= Notification.FLAG_AUTO_CANCEL;

            notificationManager.notify( NOTIFICATION_UPLOAD_TIMER, notification );
        }
    }

    private void handleError( Context context ) {
        int fc = failureCount.incrementAndGet();
        Timber.d("Failure Count is " + fc);
        if( fc == MAX_FAILURE_COUNT ){
            // show a notification that no position upload was possible
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            Notification notification  =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_directions_car_white)
                            .setContentTitle(context.getString(R.string.error_position_update_title))
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.error_position_update_message)))
                            .setContentText(context.getString(R.string.error_position_update_message))
                            .build();

            notification.flags |= Notification.FLAG_AUTO_CANCEL;

            notificationManager.notify( NOTIFICATION_UPLOAD_TIMER, notification );
        }
    }
}
