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


    public List<JoinTripRequest> findByUserId(long userId) {
        return list(namedQuery(JoinTripRequest.QUERY_FIND_BY_PASSENGER_OR_DRIVER_ID)
                .setParameter(JoinTripRequest.QUERY_PARAM_USER_ID, userId));
    }


    public List<JoinTripRequest> findByUserIdAndStatusPassengerAccepted(long userId) {
        return list(namedQuery(JoinTripRequest.QUERY_FIND_BY_PASSENGER_OR_DRIVER_ID_AND_PASSENGER_ACCEPTED_STATUS)
                .setParameter(JoinTripRequest.QUERY_PARAM_USER_ID, userId));
    }


    public List<JoinTripRequest> findDeclinedRequests( long passengerId ) {
        return list( namedQuery(JoinTripRequest.QUERY_FIND_BY_PASSENGER_ID_AND_DECLINED_STATUS)
                .setParameter(JoinTripRequest.QUERY_PARAM_USER_ID, passengerId)  );
    }



}
