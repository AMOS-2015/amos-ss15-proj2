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

package org.croudtrip.trips;

import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.AbstractScheduledTaskExecutor;
import org.hibernate.SessionFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Disables {@link TripOffer} when they do not receive location
 * updates from drivers for some time.
 */
public class DisableTripOffersExecutor extends AbstractScheduledTaskExecutor {

    private static final long MAX_TIME_UNTIL_LOST = 360;

    private TripOfferDAO tripOfferDAO;
    private TripsUtils tripsUtils;

    @Inject
    DisableTripOffersExecutor(
            TripOfferDAO tripOfferDAO,
            TripsUtils tripsUtils,
            SessionFactory sessionFactory,
            LogManager logManager) {

        super( sessionFactory, logManager, 180, TimeUnit.SECONDS );

        this.tripOfferDAO = tripOfferDAO;
        this.tripsUtils = tripsUtils;
    }

    @Override
    protected void doRun() throws Exception {

        for( TripOffer offer : tripOfferDAO.findAll() ) {

            if( offer.getStatus() == TripOfferStatus.FINISHED || offer.getStatus() == TripOfferStatus.CANCELLED )
                continue;

            long lastUpdateSeconds = (System.currentTimeMillis() / 1000 - offer.getLastPositonUpdateInSeconds());

            if( offer.getStatus() != TripOfferStatus.DISABLED ) {

                // disable offer if there was no position update for a long period of time
                if (lastUpdateSeconds > MAX_TIME_UNTIL_LOST) {

                    TripOffer updatedOffer = new TripOffer(
                            offer.getId(),
                            offer.getDriverRoute(),
                            offer.getEstimatedArrivalTimeInSeconds(),
                            offer.getCurrentLocation(),
                            offer.getMaxDiversionInMeters(),
                            offer.getPricePerKmInCents(),
                            offer.getDriver(),
                            offer.getVehicle(),
                            TripOfferStatus.DISABLED,
                            offer.getLastPositonUpdateInSeconds()
                    );
                    tripOfferDAO.update(updatedOffer);

                    logManager.d("Disabled offer: " + updatedOffer.getId());
                }
            }
            else {
                // enable offer if there was a position update again
                if (lastUpdateSeconds < MAX_TIME_UNTIL_LOST) {

                    int passengerCount = tripsUtils.getActivePassengerCountForOffer(offer);

                    TripOffer updatedOffer = new TripOffer(
                            offer.getId(),
                            offer.getDriverRoute(),
                            offer.getEstimatedArrivalTimeInSeconds(),
                            offer.getCurrentLocation(),
                            offer.getMaxDiversionInMeters(),
                            offer.getPricePerKmInCents(),
                            offer.getDriver(),
                            offer.getVehicle(),
                            passengerCount >= offer.getVehicle().getCapacity() ? TripOfferStatus.ACTIVE_FULL : TripOfferStatus.ACTIVE_NOT_FULL,
                            offer.getLastPositonUpdateInSeconds()
                    );
                    tripOfferDAO.update(updatedOffer);

                    logManager.d("Enabled offer: " + updatedOffer.getId());
                }
            }
        }

    }
}
