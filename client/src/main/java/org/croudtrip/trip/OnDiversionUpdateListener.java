package org.croudtrip.trip;

import android.widget.TextView;

import org.croudtrip.api.trips.JoinTripRequest;

/**
 * Simple interface for the JoinTripRequestsAdapter/JoinTripRequestsFragment to
 * receive results from the server async.
 * Created by Vanessa Lange on 14.05.15.
 */
public interface OnDiversionUpdateListener {

    /**
     * As soon as the diversion has been received from the server this method is called
     * to work with the results.
     * @param joinRequest
     * @param textView
     * @param diversionInMinutes
     */
    void onDiversionUpdate(JoinTripRequest joinRequest, TextView textView, int diversionInMinutes);
}