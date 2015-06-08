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

package org.croudtrip.api.gcm;

/**
 * That class contains the common gcm tags that are used by our server to send messages to the client.
 * The client has to use these tags to extract the sent information from the server message.
 * Created by Frederik Simon on 07.05.2015.
 */
public class GcmConstants {
    /** This tag is sent with every message so that you can check what the type of a message is
     * The content of this message data is the string that describes the following message
     * (e.g. GCM_MSG_JOIN_REQUEST, GCM_MSG_REQUECT_ACCEPTED etc.).
     */
    public static final String GCM_TYPE = "MSG_TYPE";

    /** Just a dummy tag that is used for testing purposes */
    public static final String GCM_MSG_DUMMY = "DUMMY";

    /** Messages with this tag are sent to the diver, if there is a request by the passenger.
     * <br>
     * contains the id of the {@link org.croudtrip.api.trips.JoinTripRequest} that is sent by the passenger
     * and can be downloaded from the server */
    public static final String GCM_MSG_JOIN_REQUEST = "JOIN_REQUEST";

    /** Messages with this tag are sent to the diver, if there is a request by the passenger.
     * <br>
     * contains the id of the {@link org.croudtrip.api.trips.JoinTripRequest} that is sent by the passenger
     * and can be downloaded from the server */
    public static final String GCM_MSG_JOIN_REQUEST_ID = "JOIN_REQUEST_ID";

    /** Messages with this tag are sent to the diver, if there is a request by the passenger.
     * <br>
     * contains the id of the {@link org.croudtrip.api.trips.TripOffer} that has to be used to get the
     * {@link org.croudtrip.api.trips.JoinTripRequest} from the server */
    public static final String GCM_MSG_JOIN_REQUEST_OFFER_ID = "JOIN_REQUEST_OFFER_ID";

    /** Contains the id of the user where the message is sent to. The user has to check, if it's him,
     * otherwise it could come to problems if there are multiple user on the same device.
     */
    public static final String GCM_MSG_USER_MAIL = "USER_MAIL";


    /** Messages with this tag are sent to the passenger, if the driver accepted the offer.
     * <br>
     * Contains just a simple string that tells you that your request has been accepted */
    public static final String GCM_MSG_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";

    /** Messages with this tag are sent to the passenger, if the driver declined to take the passenger with him.
     * * <br>
     * Contains just a simple string that tells you that your request has been declined */
    public static final String GCM_MSG_REQUEST_DECLINED = "REQUEST_DECLINED";

    /** Sent to passengers once their {@link org.croudtrip.api.trips.JoinTripRequest} expires (max waiting time has passed.). */
    public static final String GCM_MSG_REQUEST_EXPIRED = "REQUEST_EXPIRED";

    /** Messages with this tag are sent to the passenger, if there is something wrong with the requested trip (e.g. the trip does not exist anymore).
     ** It could be handled in the same way as GCM_MSG_REQUEST_DECLINED, or there is a different error message. */
    public static final String GCM_MSG_REQUEST_ERROR = "REQUEST_ERROR";

    /** Sent to passengers, if the trip has been cancelled by the driver. */
    public static final String GCM_MESSAGE_TRIP_CANCELLED_BY_PASSENGER = "TRIP_CANCELLED_BY_PASSENGER";

    /** Sent to drivers, if the trip has been cancelled by a passenger. */
    public static final String GCM_MESSAGE_TRIP_CANCELLED_BY_DRIVER = "TRIP_CANCELLED_BY_DRIVER";

    /** Messages with this tag are sent to the passenger, if there are new results for the matching process that were found by the server in background
     * <br>
     * Contains a list of the reservation ids that can be downloaded by the client. The list is represented by a string separated by spaces.*/
    public static final String GCM_MSG_FOUND_MATCHES = "FOUND_MATCHES";

    /** The ID for the {@link org.croudtrip.api.trips.RunningTripQuery} which has found a potential
     *  match.
     */
    public static final String GCM_MSG_FOUND_MATCHES_QUERY_ID = "QUERY_ID";

    public static final String GCM_MSG_PASSENGER_AT_DESTINATION = "PASSENGER_AT_DESTINATION";

    public static final String GCM_MSG_ARRIVAL_TIME_UPDATE = "ARRIVAL_TIME_UPDATE";

    /** Notifications IDs for android notifications */
    public static final int GCM_NOTIFICATION_JOIN_REQUEST_ID = 1;
    public static final int GCM_NOTIFICATION_REQUEST_ACCEPTED_ID = 2;
    public static final int GCM_NOTIFICATION_REQUEST_DECLINED_ID = 3;
    public static final int GCM_NOTIFICATION_REQUEST_ERROR_ID = 4;
    public static final int GCM_NOTIFICATION_TRIP_CANCELLED_ID = 5;
    public static final int GCM_NOTIFICATION_FOUND_MATCHES_ID = 6;
}
