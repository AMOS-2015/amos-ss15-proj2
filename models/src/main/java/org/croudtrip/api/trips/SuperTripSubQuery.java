package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Created by Frederik Simon on 20.06.2015.
 */
@Embeddable
public class SuperTripSubQuery {

    @Embedded
    private Route passengerRoute;

    @AttributeOverrides({
            @AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "sLat")),
            @AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "sLng"))
    })
    private RouteLocation startLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "eLat")),
            @AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "eLng"))
    })
    private RouteLocation destinationLocation;

    @ManyToOne
    @JoinColumn(name = User.COLUMN_ID + "_passenger", nullable = false)
    private User passenger;

    public SuperTripSubQuery() { }

    public SuperTripSubQuery( TripQuery query ) {
        this.passengerRoute = query.getPassengerRoute();
        this.startLocation = query.getStartLocation();
        this.destinationLocation = query.getDestinationLocation();
        this.passenger = query.getPassenger();
    }

    @JsonCreator
    public SuperTripSubQuery(
            @JsonProperty("passengerRoute") Route passengerRoute,
            @JsonProperty("startLocation") RouteLocation startLocation,
            @JsonProperty("destinationLocation") RouteLocation destinationLocation,
            @JsonProperty("passenger") User passenger) {

        this.passengerRoute = passengerRoute;
        this.startLocation = startLocation;
        this.destinationLocation = destinationLocation;
        this.passenger = passenger;
    }

    public Route getPassengerRoute() {
        return passengerRoute;
    }

    public RouteLocation getStartLocation() {
        return startLocation;
    }

    public RouteLocation getDestinationLocation() {
        return destinationLocation;
    }

    public User getPassenger() {
        return passenger;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuperTripSubQuery that = (SuperTripSubQuery) o;

        if (destinationLocation != null ? !destinationLocation.equals(that.destinationLocation) : that.destinationLocation != null)
            return false;
        if (passenger != null ? !passenger.equals(that.passenger) : that.passenger != null)
            return false;
        if (passengerRoute != null ? !passengerRoute.equals(that.passengerRoute) : that.passengerRoute != null)
            return false;
        if (startLocation != null ? !startLocation.equals(that.startLocation) : that.startLocation != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = passengerRoute != null ? passengerRoute.hashCode() : 0;
        result = 31 * result + (startLocation != null ? startLocation.hashCode() : 0);
        result = 31 * result + (destinationLocation != null ? destinationLocation.hashCode() : 0);
        result = 31 * result + (passenger != null ? passenger.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private Route passengerRoute;
        private RouteLocation startLocation;
        private RouteLocation destinationLocation;
        private User passenger;

        public Builder setPassengerRoute(Route passengerRoute) {
            this.passengerRoute = passengerRoute;
            return this;
        }

        public Builder setStartLocation(RouteLocation startLocation) {
            this.startLocation = startLocation;
            return this;
        }

        public Builder setDestinationLocation(RouteLocation destinationLocation) {
            this.destinationLocation = destinationLocation;
            return this;
        }

        public Builder setPassenger(User passenger) {
            this.passenger = passenger;
            return this;
        }

        public SuperTripSubQuery build(){
            return new SuperTripSubQuery( passengerRoute, startLocation, destinationLocation, passenger );
        }
    }
}
