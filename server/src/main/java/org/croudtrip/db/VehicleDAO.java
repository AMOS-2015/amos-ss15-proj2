package org.croudtrip.db;


import com.google.common.base.Optional;

import org.croudtrip.api.account.Vehicle;
import org.hibernate.SessionFactory;

import javax.inject.Inject;

public class VehicleDAO extends AbstractDAO<Vehicle> {

	@Inject
	VehicleDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public Optional<Vehicle> findByUserId(long userId) {
		return Optional.fromNullable(
				uniqueResult(
						namedQuery(Vehicle.QUERY_NAME_FIND_BY_USER_ID)
								.setParameter(Vehicle.QUERY_PARAM_USER_ID, userId)));
	}

}
