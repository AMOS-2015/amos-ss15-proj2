package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.RouteLocation;

public class UserWayPoint {

	private final User user;
	private final RouteLocation location;
	private final boolean isStartOfTrip;
	private final long arrivalTimestamp; // unix timestamp in seconds
	private final long distanceToDriverInMeters;

    @JsonCreator
	public UserWayPoint(
			@JsonProperty("user") User user,
			@JsonProperty("routeLocation") RouteLocation location,
            @JsonProperty("startOfTrip") boolean isStart,
            @JsonProperty("arrivalTimestamp") long arrivalTimestamp,
            @JsonProperty("distanceToDriverInMeters") long distanceToDriverInMeters) {

		this.user = user;
		this.location = location;
		this.isStartOfTrip = isStart;
		this.arrivalTimestamp = arrivalTimestamp;
		this.distanceToDriverInMeters = distanceToDriverInMeters;
	}

	public User getUser() {
		return user;
	}

	public RouteLocation getLocation() {
		return location;
	}

	public boolean isStartOfTrip() {
		return isStartOfTrip;
	}

	public long getArrivalTimestamp() {
		return arrivalTimestamp;
	}

	public long getDistanceToDriverInMeters() {
		return distanceToDriverInMeters;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserWayPoint that = (UserWayPoint) o;
		return Objects.equal(isStartOfTrip, that.isStartOfTrip) &&
				Objects.equal(arrivalTimestamp, that.arrivalTimestamp) &&
				Objects.equal(user, that.user) &&
				Objects.equal(location, that.location) &&
				Objects.equal(distanceToDriverInMeters, that.distanceToDriverInMeters);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(user, location, isStartOfTrip, arrivalTimestamp, distanceToDriverInMeters);
	}


	public static class Builder {

		private User user;
		private RouteLocation location;
		private boolean isStartOfTrip;
		private long arrivalTimestamp; // unix timestamp in seconds
		private long distanceToDriverInMeters;

		public Builder setUser(User user) {
			this.user = user;
			return this;
		}

		public Builder setLocation(RouteLocation location) {
			this.location = location;
			return this;
		}

		public Builder setIsStartOfTrip(boolean isStartOfTrip) {
			this.isStartOfTrip = isStartOfTrip;
			return this;
		}

		public Builder setArrivalTimestamp(long arrivalTimestamp) {
			this.arrivalTimestamp = arrivalTimestamp;
			return this;
		}

		public Builder setDistanceToDriverInMeters(long distanceToDriverInMeters) {
			this.distanceToDriverInMeters = distanceToDriverInMeters;
			return this;
		}

		public UserWayPoint build() {
			return new UserWayPoint(user, location, isStartOfTrip, arrivalTimestamp, distanceToDriverInMeters);
		}
	}

}
