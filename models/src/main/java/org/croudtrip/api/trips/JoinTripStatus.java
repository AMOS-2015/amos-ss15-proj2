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

package org.croudtrip.api.trips;


public enum JoinTripStatus {

	PASSENGER_ACCEPTED, 		// passenger accepted, driver pending
	DRIVER_ACCEPTED, 			// passenger accepted, driver accepted
	DRIVER_DECLINED,			// passenger accepted, driver declined (end state)
	PASSENGER_IN_CAR,			// both accepted, physical trip is happening
	PASSENGER_AT_DESTINATION,	// passenger dropped of (end state)
	PASSENGER_CANCELLED,		// passenger cancelled trip (end state)
	DRIVER_CANCELLED			// driver cancelled trip (end state)

}
