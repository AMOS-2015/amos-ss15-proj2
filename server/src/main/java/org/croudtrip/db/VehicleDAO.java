package org.croudtrip.db;


import org.croudtrip.api.account.Vehicle;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

public class VehicleDAO extends AbstractDAO<Vehicle> {

	@Inject
	VehicleDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public List<Vehicle> findByUserId(long userId) {
		return list(namedQuery(Vehicle.QUERY_NAME_FIND_BY_USER_ID)
				.setParameter(Vehicle.QUERY_PARAM_USER_ID, userId));
	}

}
