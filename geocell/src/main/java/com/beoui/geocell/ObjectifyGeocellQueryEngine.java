package com.beoui.geocell;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.util.List;

import com.beoui.geocell.GeocellQueryEngine;
import com.beoui.geocell.GeocellUtils;
import com.beoui.geocell.model.GeocellQuery;
import com.googlecode.objectify.cmd.Query;

/**
 * GeocellQueryEngine for running Geocell queries against Objectify entities.
 * 
 * N.B. You MUST add the '@javax.persistence.Id' annotation in addition to the '@com.googlecode.objectify.annotation.Id' to the Id field of the entity you will
 * be querying, so that GeocellUtils can identify the primary key in {@link com.beoui.geocell.GeocellUtils#getKeyString(Object) GeocellUtils}.
 *
 * @see <a href="https://code.google.com/p/objectify-appengine/">Objectify Project Home</a>
 * 
 * @see com.beoui.geocell.GeocellUtils#getKeyString(Object)
 * 
 * @author unparalleled
 *
 */
public class ObjectifyGeocellQueryEngine implements GeocellQueryEngine {

	/**
	 * @see com.beoui.geocell.GeocellQueryEngine#query(com.beoui.geocell.model.GeocellQuery, java.util.List, java.lang.Class)
	 */
	@Override
	public <T> List<T> query(GeocellQuery baseQuery, List<String> curGeocellsUnique, Class<T> entityClass) {

		Query<T> ofyQuery = ofy().load().type(entityClass);

		// add geocells filter
		String geocellsField = GeocellUtils.getGeocellsFieldName(entityClass);
		ofyQuery.filter(geocellsField + " in", curGeocellsUnique);

		// add additional filters if base query is not null
		if (baseQuery != null && baseQuery.getBaseQuery() != null && !baseQuery.getBaseQuery().equals("")) {

			// extract filters from JDO/JPA style query
			String query = baseQuery.getBaseQuery();
			String queryParams = baseQuery.getDeclaredParameters(); // not necessary for Objectify (low-level API)

			// make sure parameters are present
			List<Object> params = baseQuery.getParameters();
			if (params == null || params.isEmpty())
				throw new IllegalArgumentException("parameters are null or empty");

			// split base query into discrete filters for Objectifiy (low-level API)
			String[] filters = query.split(" && ");
			if (filters.length != params.size())
				throw new IllegalArgumentException("number of filters does not match number of parameters");

			// add filters
			for (int i = 0; i < filters.length; i++) {
				String filter = filters[i];

				// cut off filter's 'param' place holder
				int indexLastSpace = filter.lastIndexOf(" ");
				String condition = filter.substring(0, indexLastSpace);

				ofyQuery.filter(condition, params.get(i));
			}
		}

		// execute and return list
		return ofyQuery.list();
	}

}
