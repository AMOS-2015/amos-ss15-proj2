package org.croudtrip.api.trips;

import com.google.common.base.Objects;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.RouteLocation;

public class UserWayPoint {

	private final User user;
	private final RouteLocation location;
	private final boolean isStartOfTrip;
	private final long arrivalTimestamp; // unix timestamp in seconds

	public UserWayPoint(User user, RouteLocation location, boolean isStart, long arrivalTimestamp) {
		this.user = user;
		this.location = location;
		this.isStartOfTrip = isStart;
		this.arrivalTimestamp = arrivalTimestamp;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserWayPoint that = (UserWayPoint) o;
		return Objects.equal(isStartOfTrip, that.isStartOfTrip) &&
				Objects.equal(arrivalTimestamp, that.arrivalTimestamp) &&
				Objects.equal(user, that.user) &&
				Objects.equal(location, that.location);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(user, location, isStartOfTrip, arrivalTimestamp);
	}

}
