package org.croudtrip.places;

import com.google.inject.AbstractModule;

import org.croudtrip.app.CroudTripConfig;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;

public class PlacesModule extends AbstractModule {

    private final String googleApiKey;

    public PlacesModule(CroudTripConfig config) {
        this.googleApiKey = config.getGoogleAPIKey();
    }


    @Override
    protected void configure() {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint("https://maps.googleapis.com/maps/api/place/")
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addQueryParam("key", googleApiKey);
                        //request.addQueryParam("types", "bus_station|parking|train_station|store|gas_station");
                        request.addQueryParam("types", "accounting|airport|amusement_park|aquarium|art_gallery|atm|bakery|bank|bar|beauty_salon|bicycle_store|book_store|bowling_alley|bus_station|cafe|campground|car_dealer|car_rental|car_repair|car_wash|casino|cemetery|church|city_hall|clothing_store|convenience_store|courthouse|dentist|department_store|doctor|electrician|electronics_store|embassy|establishment|finance|fire_station|florist|food|funeral_home|furniture_store|gas_station|general_contractor|grocery_or_supermarket|gym|hair_care|hardware_store|health|hindu_temple|home_goods_store|hospital|insurance_agency|jewelry_store|laundry|lawyer|library|liquor_store|local_government_office|locksmith|lodging|meal_delivery|meal_takeaway|mosque|movie_rental|movie_theater|moving_company|museum|night_club|painter|park|parking|pet_store|pharmacy|physiotherapist|place_of_worship|plumber|police|post_office|real_estate_agency|restaurant|roofing_contractor|rv_park|school|shoe_store|shopping_mall|spa|stadium|storage|store|subway_station|synagogue|taxi_stand|train_station|travel_agency|university|veterinary_care|zoo");
                    }
                })
                .setConverter(new JacksonConverter())
                .build();
        bind(PlacesApi.class).toInstance(adapter.create(PlacesApi.class));
    }

}
