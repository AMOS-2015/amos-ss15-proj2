package org.croudtrip.places;

/**
 * The PlacesApiContext contains the google api key that is needed to create a {@link PlacesApiRequest}.
 * This class can be injected by Guice wherever you need it and you can create multiple PlacesApiRequests,
 * if you wish so.
 */
public class PlacesApiContext {
    private final String googleApiKey;

    public PlacesApiContext(String googleApiKey) {
        this.googleApiKey = googleApiKey;
    }

    /**
     * Retrieves the google api key to use it in an {@link PlacesApiRequest}
     * @return the google api key that is set in the config file of the server
     */
    public String getGoogleApiKey() {
        return googleApiKey;
    }
}
