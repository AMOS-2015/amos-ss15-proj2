package org.croudtrip.db;


import org.croudtrip.api.trips.JoinTripRequest;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

public class JoinTripRequestDAO extends AbstractDAO<JoinTripRequest> {

    @Inject
    JoinTripRequestDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }


    public List<JoinTripRequest> findAll() {
        return list(namedQuery(JoinTripRequest.QUERY_NAME_FIND_ALL));
    }


    public List<JoinTripRequest> findByOfferId(long offerId) {
        return list(namedQuery(JoinTripRequest.QUERY_FIND_BY_OFFER_ID)
                .setParameter(JoinTripRequest.QUERY_PARAM_OFFER_ID, offerId));
    }


    public List<JoinTripRequest> findByOfferIdAndStatusPassengerAccepted(long offerId) {
        return list(namedQuery(JoinTripRequest.QUERY_FIND_BY_OFFER_ID_AND_PASSENGER_ACCEPTED_STATUS)
                .setParameter(JoinTripRequest.QUERY_PARAM_OFFER_ID, offerId));
    }


    public List<JoinTripRequest> findDeclinedRequests( long passengerId ) {
        return list( namedQuery(JoinTripRequest.QUERY_FIND_BY_PASSENGER_ID_AND_DECLINED_STATUS)
                .setParameter(JoinTripRequest.QUERY_PARAM_PASSENGER_ID, passengerId)  );
    }



}
