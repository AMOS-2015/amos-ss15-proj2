package org.croudtrip.api.gcm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;

/**
 * One GCM registration which can be uploaded to the server.
 */
public class GcmRegistrationDescription {

	@NotNull private final String gcmId;

	@JsonCreator
	public GcmRegistrationDescription(@JsonProperty("gcmId") String gcmId) {
		this.gcmId = gcmId;
	}

	public String getGcmId() {
		return gcmId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GcmRegistrationDescription that = (GcmRegistrationDescription) o;
		return Objects.equal(gcmId, that.gcmId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(gcmId);
	}

}
