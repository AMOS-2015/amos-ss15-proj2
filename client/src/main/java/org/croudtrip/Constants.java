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

package org.croudtrip;

/**
 * Created by alex on 26.04.15.
 */
public class Constants {
    public final static String SHARED_PREF_FILE_PREFERENCES = "org.croudtrip.preferences";

    // OfferDispatchFragment
    public final static String SHARED_PREF_KEY_RUNNING_TRIP_OFFER = "running_trip_offer";

    public final static String SHARED_PREF_KEY_WAITING_TIME = "waitingTime";
    public final static String SHARED_PREF_KEY_SEARCHING = "searching";
    public final static String SHARED_PREF_KEY_WAITING= "waiting";
    public final static String SHARED_PREF_KEY_DRIVING = "driving";
    public final static String SHARED_PREF_KEY_TRIP_ID = "trip_id";
    public final static String SHARED_PREF_KEY_ACCEPTED = "accepted";
    public final static String SHARED_PREF_KEY_QUERY_ID = "queryId";
    public final static String SHARED_PREF_KEY_DIVERSION = "maxDiversion";
    public final static String SHARED_PREF_KEY_PRICE = "price";
    public final static String SHARED_PREF_KEY_SKIP_ENABLE_GPS = "skipEnableGPS";
    public final static String SHARED_PREF_KEY_PROFILE_IMAGE_URI = "profileImg";

    public final static String EVENT_JOIN_REQUEST_EXPIRED = "request_expired";
    public final static String EVENT_CHANGE_JOIN_UI = "change_join_ui";
    public final static String EVENT_SECONDARY_DRIVER_ACCEPTED = "secondary_driver_accepted";
    public final static String EVENT_PASSENGER_CANCELLED_TRIP = "passenger_cancelled_trip";
    public final static String EVENT_PASSENGER_ENTERED_CAR = "passenger_entered_car";
    public final static String EVENT_PASSENGER_REACHED_DESTINATION = "passenger_reached_destination";
    public final static String EVENT_NFC_TAG_SCANNED = "nfc_tag_scanned";

}
