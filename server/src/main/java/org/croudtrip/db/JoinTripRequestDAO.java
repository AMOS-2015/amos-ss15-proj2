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

    public List<JoinTripRequest> findByUserIdAndStatusDriverAccepted(long userId) {
        return list(namedQuery(JoinTripRequest.QUERY_FIND_BY_PASSENGER_OR_DRIVER_ID_AND_DRIVER_ACCEPTED_STATUS)
                .setParameter(JoinTripRequest.QUERY_PARAM_USER_ID, userId));
    }


    public List<JoinTripRequest> findDeclinedRequests(long passengerId) {
        return list( namedQuery(JoinTripRequest.QUERY_FIND_BY_PASSENGER_ID_AND_DECLINED_STATUS)
                .setParameter(JoinTripRequest.QUERY_PARAM_USER_ID, passengerId)  );
    }


    public List<JoinTripRequest> findByOfferId(long offerId) {
        return list(namedQuery(JoinTripRequest.QUERY_FIND_BY_OFFER_ID)
                .setParameter(JoinTripRequest.QUERY_PARAM_OFFER_ID, offerId));
    }

}
