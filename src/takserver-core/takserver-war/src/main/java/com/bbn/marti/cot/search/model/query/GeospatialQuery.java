

package com.bbn.marti.cot.search.model.query;

import com.bbn.marti.cot.search.service.CotQueryParameter;
import com.bbn.marti.cot.search.service.CotQueryWorker;

public enum GeospatialQuery {
	CIRCLE(
			// " || " is the SQL string concatenation operator, needed so the ? placeholders are outside the
			//  quoted string
			"ST_DWITHIN(event_pt, ST_GeographyFromText('SRID=" + CotQueryWorker.SRID + ";POINT(' || ? || ' ' || ? || ')'), ?)", 
			new CotQueryParameter[] {	CotQueryParameter.centerLongitude, 
										CotQueryParameter.centerLatitude,
										CotQueryParameter.radius}),
			
			
			// longitude (degrees), latitude (degrees), radius (meters)
	RECTANGLE("ST_WITHIN(event_pt, ST_MakeEnvelope(?, ?, ?, ?, " + CotQueryWorker.SRID + "))",
			new CotQueryParameter[] {	CotQueryParameter.rectangleLeft,
										CotQueryParameter.rectangleBottom,
										CotQueryParameter.rectangleRight,
										CotQueryParameter.rectangleTop});
	private String sqlFragment;
	private CotQueryParameter[] requiredParameters;
	
	private GeospatialQuery(String expression, CotQueryParameter[] required) {
		this.sqlFragment = expression;
		this.requiredParameters = required;
	}
	
	public String getGeometryExpression() {
		return this.sqlFragment;
	}
	
	public CotQueryParameter[] getRequiredParameters() {
		return this.requiredParameters;
	}
	
	public static GeospatialQuery fromString(String token) {
		for (GeospatialQuery definedQuery : GeospatialQuery.values()) {
			if (definedQuery.toString().compareToIgnoreCase(token) == 0) {
				return definedQuery;
			}
		}
		return null;
	}
}