package org.croudtrip.trips;

import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.logs.LogManager;
import org.hibernate.SessionFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Created by Frederik Simon on 18.05.2015.
 */
public class TripOfferChecker extends AbstractGarbageCollection {

    private final long MAX_TIME_UNTIL_LOST = 360;

    private TripOfferDAO tripOfferDAO;
    private TripsManager tripsManager;

    @Inject
    TripOfferChecker( TripOfferDAO tripOfferDAO, TripsManager tripsManager, SessionFactory sessionFactory, LogManager logManager) {
        super( sessionFactory, logManager, 180, TimeUnit.SECONDS );

        this.tripOfferDAO = tripOfferDAO;
        this.tripsManager = tripsManager;
    }

    @Override
    protected void doRun() throws Exception {

        for( TripOffer offer : tripOfferDAO.findAll() ) {

            if( offer.getStatus() == TripOfferStatus.FINISHED )
                continue;

            long lastUpdateSeconds = (System.currentTimeMillis() / 1000 - offer.getLastPositonUpdate());

            if( offer.getStatus() != TripOfferStatus.DISABLED ) {

                // disable offer if there was no position update for a long period of time
                if (lastUpdateSeconds > MAX_TIME_UNTIL_LOST) {

                    TripOffer updatedOffer = new TripOffer(
                            offer.getId(),
                            offer.getDriverRoute(),
                            offer.getMaxDiversionInMeters(),
                            offer.getPricePerKmInCents(),
                            offer.getDriver(),
                            offer.getVehicle(),
                            TripOfferStatus.DISABLED,
                            offer.getLastPositonUpdate()
                    );
                    tripOfferDAO.update(updatedOffer);

                    logManager.d("Disabled offer: " + updatedOffer.getId());
                }
            }
            else {
                // enable offer if there was a position update again
                if (lastUpdateSeconds < MAX_TIME_UNTIL_LOST) {

                    int passengers = tripsManager.getActiveJoinRequestsForOffer(offer);

                    TripOffer updatedOffer = new TripOffer(
                            offer.getId(),
                            offer.getDriverRoute(),
                            offer.getMaxDiversionInMeters(),
                            offer.getPricePerKmInCents(),
                            offer.getDriver(),
                            offer.getVehicle(),
                            passengers >= offer.getVehicle().getCapacity() ? TripOfferStatus.ACTIVE_FULL : TripOfferStatus.ACTIVE_NOT_FULL,
                            offer.getLastPositonUpdate()
                    );
                    tripOfferDAO.update(updatedOffer);

                    logManager.d("Enabled offer: " + updatedOffer.getId());
                }
            }
        }

    }
}
