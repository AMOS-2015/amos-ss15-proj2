package org.croudtrip.trips;

import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.AbstractScheduledTaskExecutor;
import org.hibernate.SessionFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Cancels {@link TripOffer}s after 12 hours of inactivity.
 */
public class ExpireTripOffersExecutor extends AbstractScheduledTaskExecutor {

    private static final long MAX_ARRIVAL_DELAY = 12 * 60 * 60; // 12 hours
    private TripOfferDAO tripOfferDAO;
    private TripsManager tripsManager;

    @Inject
    ExpireTripOffersExecutor(TripOfferDAO tripOfferDAO, TripsManager tripsManager, SessionFactory sessionFactory, LogManager logManager) {
        super( sessionFactory, logManager, 3, TimeUnit.HOURS);

        this.tripOfferDAO = tripOfferDAO;
        this.tripsManager = tripsManager;
    }

    @Override
    protected void doRun() throws Exception {

        for (TripOffer offer : tripOfferDAO.findAll()) {
            if( offer.getStatus() == TripOfferStatus.FINISHED || offer.getStatus() == TripOfferStatus.CANCELLED)
                continue;

            if (offer.getEstimatedArrivalTimeInSeconds() + MAX_ARRIVAL_DELAY < System.currentTimeMillis()/1000) {
                logManager.d("Offer " + offer.getId() + " expired and will be deleted");
                tripsManager.updateOffer(offer, TripOfferUpdate.createCancelUpdate());
            }
            else
            {
                long deltaTime = offer.getEstimatedArrivalTimeInSeconds() + MAX_ARRIVAL_DELAY - System.currentTimeMillis()/1000;
                long hours = deltaTime / 3600;
                deltaTime %= 3600;
                long minutes = deltaTime / 60;
                deltaTime %= 60;
                long seconds = deltaTime;
                logManager.d("Offer " + offer.getId() + " will expire in " + hours + "h " + minutes + "min " + seconds + "s.");
            }
        }
    }
}
