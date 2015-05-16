package org.croudtrip.api.trips;


public enum TripOfferStatus {

	ACTIVE_NOT_FULL,		// offer is active and passengers can still join
	ACTIVE_FULL,			// offer is active but passenger can no longer join
	DISABLED,				// offer is (temporarily) disabled
	FINISHED				// offer has been finished

}
