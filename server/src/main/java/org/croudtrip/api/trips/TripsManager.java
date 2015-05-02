package org.croudtrip.api.trips;


import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.api.directions.DirectionsManager;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TripsManager {

	private final TripOfferDAO tripOfferDAO;
	private final DirectionsManager directionsManager;


	@Inject
	TripsManager(TripOfferDAO tripOfferDAO, DirectionsManager directionsManager) {
		this.tripOfferDAO = tripOfferDAO;
		this.directionsManager = directionsManager;
	}


	public TripOffer addOffer(User owner, TripOfferDescription description) throws Exception {
		List<Route> route = directionsManager.getDirections(description.getStart(), description.getEnd());
		if (route.size() == 0) throw new Exception("not route found");
		TripOffer offer = new TripOffer(0, route.get(0), description.getMaxDiversionInMeters(), description.getPricePerKmInCents(), owner);
		tripOfferDAO.save(offer);
		return offer;
	}


	public List<TripOffer> findAllOffers() {
		return tripOfferDAO.findAll();
	}


	public Optional<TripOffer> findOffer(long offerId) {
		return tripOfferDAO.findById(offerId);
	}


	public void deleteOffer(TripOffer offer) {
		tripOfferDAO.delete(offer);
	}


	public List<TripMatch> findMatches(User passenger, TripRequestDescription requestDescription) throws Exception {
		List<TripMatch> matches = new ArrayList<>();

		List<TripOffer> offers = findAllOffers();
		if (offers.size() == 0) return matches;

        List<Route> diversionRoutes = new ArrayList<>();

        for( TripOffer offer : offers ) {
            Route route = offer.getRoute();
            RouteLocation startLocation = route.getStart();
            RouteLocation endLocation = route.getEnd();

            List<RouteLocation> waypoints = new ArrayList<>();
            waypoints.add( requestDescription.getStart() );
            waypoints.add( requestDescription.getEnd() );

            diversionRoutes = directionsManager.getDirections( startLocation, endLocation, waypoints );

            if( diversionRoutes == null || diversionRoutes.size() == 0 )
                return new ArrayList<>();

            Route diversionRoute = diversionRoutes.get(0);
            System.out.println("Additional meters: " + (diversionRoute.getDistanceInMeters() - route.getDistanceInMeters()));
            System.out.println("Max Diversion: " + (offer.getMaxDiversionInMeters()));
            if( diversionRoute.getDistanceInMeters() - route.getDistanceInMeters() < offer.getMaxDiversionInMeters() )
            {
                // TODO: What is the trip length from the point of view of our customers?
                List<Route> passengerRoutes = directionsManager.getDirections( requestDescription.getStart(), requestDescription.getEnd() );
                if(passengerRoutes == null || passengerRoutes.isEmpty())
                    return new ArrayList<>();

                Route passengerRoute = passengerRoutes.get(0);

                long tripLength = (passengerRoute.getDistanceInMeters());
                long tripDuration = passengerRoute.getDurationInSeconds();
                int price = (int) (tripLength/100.0f * offer.getPricePerKmInCents());

                TripMatch match = new TripMatch(
                        0,
                        diversionRoute, tripLength,
                        tripDuration,
                        price,
                        offer.getPricePerKmInCents(),
                        offer.getDriver(),
                        passenger);

                matches.add(match);
            }
        }

        // if there is no or only one match, we can return immediately, since there is no price comparation necessary
        if( matches.size() < 2 )
            return matches;


        // sort matches ascending based on the price
        Collections.sort(matches, new Comparator<TripMatch>() {
            @Override
            public int compare(TripMatch m1, TripMatch m2) {
                if ( m1.getPricePerKilometerInCents() == m2.getPricePerKilometerInCents() )
                    return 0;

                return (m1.getPricePerKilometerInCents() < m2.getPricePerKilometerInCents()) ? -1 : 1;
            }
        });

        int samePrice = 1;
        int lowestPrice = matches.get(0).getPricePerKilometerInCents();
        while( samePrice < matches.size() && matches.get(samePrice).getPricePerKilometerInCents() == lowestPrice ) {
            System.out.println("Match: " + matches.get(samePrice).getEstimatedPriceInCents() + " "+  matches.get(samePrice).getPricePerKilometerInCents());
            samePrice++;
        }

        // if all matches have the same price no adaption is necessary
        if( samePrice == matches.size() )
            return matches;

        int secondLowestPrice = matches.get(samePrice).getPricePerKilometerInCents();
        System.out.println("Second lowest price: " + secondLowestPrice);
        while( matches.size() > samePrice )
            matches.remove( matches.size() - 1);

        // adjust price value for all possible matches
        for( TripMatch m : matches ) {
            m.setPricePerKilometerInCents( secondLowestPrice );
            m.setEstimatedPriceInCents( (int)(m.getDiversionInMeters()/1000.0f * m.getPricePerKilometerInCents()) );

            System.out.println("Match: " + m.getEstimatedPriceInCents() + " "+  m.getPricePerKilometerInCents());
        }

		return matches;
	}

}

