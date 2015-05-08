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

    /** Messages with this tag are sent to the passenger, if the driver accepted the offer.
     * <br>
     * Contains just a simple string that tells you that your request has been accepted */
    public static final String GCM_MSG_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";

    /** Messages with this tag are sent to the passenger, if the driver declined to take the passenger with him.
     * * <br>
     * Contains just a simple string that tells you that your request has been declined */
    public static final String GCM_MSG_REQUEST_DECLINED = "REQUEST_DECLINED";

    /** Messages with this tag are sent to the passenger, if there is something wrong with the requested trip (e.g. the trip does not exist anymore).
     ** It could be handled in the same way as GCM_MSG_REQUEST_DECLINED, or there is a different error message. */
    public static final String GCM_MSG_REQUEST_ERROR = "REQUEST_ERROR";

    /** Messages with this tag are sent to the passenger or driver, if the runing trip has been cancelled by the other party.
     * * <br>
     * Contains just a simple string that tells you that your trip has been cancelled */
    public static final String GCM_MSG_TRIP_CANCELLED = "TRIP_CANCELLED";

    /** Messages with this tag are sent to the passenger, if there are new results for the matching process that were found by the server in background
     * <br>
     * Contains a list of the reservation ids that can be downloaded by the client. The list is represented by a string separated by spaces.*/
    public static final String GCM_MSG_FOUND_MATCHES = "FOUND_MATCHES";

    /** Notifications IDs for android notifications */
    public static final int GCM_NOTIFICATION_JOIN_REQUEST_ID = 1;
    public static final int GCM_NOTIFICATION_REQUEST_ACCEPTED_ID = 2;
    public static final int GCM_NOTIFICATION_REQUEST_DECLINED_ID = 3;
    public static final int GCM_NOTIFICATION_REQUEST_ERROR_ID = 4;
    public static final int GCM_NOTIFICATION_TRIP_CANCELLED_ID = 5;
    public static final int GCM_NOTIFICATION_FOUND_MATCHES_ID = 6;
}
