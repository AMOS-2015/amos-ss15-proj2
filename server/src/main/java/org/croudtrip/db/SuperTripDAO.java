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


import org.croudtrip.api.trips.SuperTrip;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

public class SuperTripDAO extends AbstractDAO<SuperTrip> {

    @Inject
    SuperTripDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }


    public List<SuperTrip> findAll() {
        return list(namedQuery(SuperTrip.QUERY_NAME_FIND_ALL));
    }


    public List<SuperTrip> findByPassengerId(long passengerId) {
        return list(namedQuery(SuperTrip.QUERY_FIND_BY_PASSENGER_ID)
                .setParameter(SuperTrip.QUERY_PARAM_USER_ID, passengerId));
    }

}
