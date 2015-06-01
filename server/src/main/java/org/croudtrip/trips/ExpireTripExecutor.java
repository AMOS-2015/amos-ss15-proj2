package org.croudtrip.trips;

import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.AbstractScheduledTaskExecutor;
import org.hibernate.SessionFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Scheduled executor task that is called to remove expired trips if necessary
 * Created by Frederik Simon on 31.05.2015.
 */
public class ExpireTripExecutor extends AbstractScheduledTaskExecutor {

    private static final long MAX_ARRIVAL_DELAY = 12 * 60 * 60; // 12 hours
    private TripOfferDAO tripOfferDAO;
    private TripsManager tripsManager;

    @Inject
    ExpireTripExecutor( TripOfferDAO tripOfferDAO, TripsManager tripsManager, SessionFactory sessionFactory, LogManager logManager) {
        super( sessionFactory, logManager, 3, TimeUnit.HOURS);

        this.tripOfferDAO = tripOfferDAO;
        this.tripsManager = tripsManager;
    }

    @Override
    protected void doRun() throws Exception {

        for (TripOffer offer : tripOfferDAO.findAll()) {
            if (offer.getEstimatedArrivalTimeInSeconds() + MAX_ARRIVAL_DELAY > System.currentTimeMillis()/1000) {
                logManager.d("Offer " + offer.getId() + " expired and will be deleted");
                tripsManager.updateOffer(offer, TripOfferUpdate.createCancelUpdate());
            }
        }
    }
}
