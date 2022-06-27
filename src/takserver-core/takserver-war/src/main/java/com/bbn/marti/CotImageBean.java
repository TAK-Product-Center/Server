

package com.bbn.marti;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.Vector;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;

import tak.server.Constants;

public class CotImageBean implements Serializable {
	
	@Autowired
	private JDBCQueryAuditLogHelper wrap;
	
	@Autowired
	DataSource ds;

	private static final long serialVersionUID = 2347623574L;

	private static final String cotImageChips = "cot_rs_tmp_imagery";
	private static final String cotImageChipColumn = "img_data";
	
	public static enum FmtType {
		COT, THUMB, JPG, NITF, GEOTIFF
	};

	public class ImageWrapper implements Serializable {
		private static final long serialVersionUID = 76572635476321L;

		public byte[] img;
		public String uid;
	}

	/**
	 * Gets a map from CoT UIDs to the list of received-times of events having that UID.
	 * @param start
	 * @param end
	 * @param limit
	 * @return a Map whose keys are all CoT UIDs in the <code>cot_router</code> table and whose values are
	 * the List of received-times for the respective UID
	 * @throws Exception
	 */
	public HashMap<String, List<Long>> getTimes(long start, long end, int limit)
			throws Exception {

		Timestamp timeStart = new Timestamp(start);
		Timestamp timeEnd = new Timestamp(end);

		String queryText = "SELECT cot_router.uid, cot_router.servertime FROM cot_router "
				+ " WHERE cot_router.servertime > TIMESTAMP ? AND cot_router.servertime < TIMESTAMP ?";
		if (limit > 0) {
			queryText += " LIMIT ?";
		}

		try (Connection connection = ds.getConnection(); PreparedStatement statement = wrap.prepareStatement(queryText, connection)) {
			statement.setTimestamp(1, timeStart);
			statement.setTimestamp(2, timeEnd);
			if (limit > 0) {
				statement.setInt(3, limit);
			}

			try (ResultSet results = wrap.doQuery(statement)) {
				HashMap<String, List<Long>> uidMap = new HashMap<String, List<Long>>();
				if (results != null && !results.isClosed()) {
					while (results.next()) {
						String uid = results.getString(1);
						long timeStamp = results.getTimestamp(2).getTime();
						if (!uidMap.containsKey(uid)) {
							uidMap.put(uid, new LinkedList<Long>());
						}
						uidMap.get(uid).add(timeStamp);
					}
				}
				return uidMap;
			}
		}
	}

	public int getLastThumbnailCount() throws Exception {
		try(Connection connection = ds.getConnection(); PreparedStatement ps = wrap.prepareStatement("select count(cotgrp.uid) from cot_thumbnail thumbnail, cot_router cot, "
				+ "(select max(id) as id, uid as uid from cot_router group by uid) cotgrp "
				+ "where cot.id=cotgrp.id and thumbnail.cot_id=cot.id", connection)) {

			int count = 0;

			try (ResultSet results = wrap.doQuery(ps)) {
				if (results != null && !results.isClosed())
					count = results.getInt(1);

				return count;
			}
		}
	}
	
	public byte[] getImageChipById(int chipId) throws Exception {
		try(Connection connection = ds.getConnection(); PreparedStatement query = wrap.prepareStatement("SELECT "
				+ cotImageChipColumn + " FROM " + cotImageChips
				+ "  WHERE id=?", connection)) {
			query.setInt(1, chipId);
			try (ResultSet results = wrap.doQuery(query)) {
			byte[] img = null;
			if (results != null && !results.isClosed()) {
				results.next();
				img = results.getBytes(1);
			}
			return img;
			}
		} 
	}

	public String[][] getImagesByUid(String uid, int pageNo, int numColumns, int numRows) 
			throws SQLException, NamingException {

		/* Using user alias table */
		/*
		 * String sqlQuery =
		 * "select thumbnail.cot_id, cot.time from cot_router cot, cot_thumbnail thumbnail, "
		 * + "(SELECT distinct cot_router.uid as uid " +
		 * "FROM cot_router, users	" + "WHERE cot_router.uid LIKE users.uid	" +
		 * "AND users.name = '"+ uid +"'	) aliases " +
		 * "WHERE cot.uid = aliases.uid  and cot.id=thumbnail.cot_id " +
		 * "order by cot.time desc " +" limit " + rowSize * columnSize +
		 * " offset " + rowSize * columnSize * pageNo; //
		 * System.out.println(sqlQuery);
		 */

		/* Not using alias table */

		try (Connection connection = ds.getConnection(); PreparedStatement sqlQuery = wrap
				.prepareStatement("select cot.id, thumbnail.id, cot.time, cot.detail from cot_router cot, cot_thumbnail thumbnail where cot.uid=?"
						+ " and cot.id=thumbnail.cot_id order by cot.time desc limit ? offset ?", connection)) {
			// sqlQuery.setString(1, Sanitizer.checkUid(uid));
			sqlQuery.setString(1, uid);
			sqlQuery.setInt(2, numRows * numColumns);
			sqlQuery.setInt(3, numRows * numColumns * pageNo);

			ResultSet results = wrap.doQuery(sqlQuery);
			Vector<String[]> resultVec = new Vector<String[]>();

			if (results != null && !results.isClosed()) {
				while (results.next()) {
					String rslArray[] = new String[4];
					rslArray[0] = results.getInt(1) + "";
					rslArray[1] = results.getInt(2) + "";
					rslArray[2] = results.getTimestamp(3).toString();
					rslArray[3] = results.getString(4);
					resultVec.add(rslArray);
				}
			}

			String[][] idList = null;
			if (resultVec != null && resultVec.size() > 0) {
				// idList = new String[resultVec.size()][2];
				// resultVec.copyInto(idList);
				idList = new String[resultVec.size()][5];
				for (int i = 0; i < resultVec.size(); i++) {
					idList[i][0] = resultVec.elementAt(i)[0];
					idList[i][1] = resultVec.elementAt(i)[1];
					idList[i][2] = resultVec.elementAt(i)[2];
					idList[i][3] = hasVideoElement(resultVec.elementAt(i)[3]);
					idList[i][4] = hasShapeElement(resultVec.elementAt(i)[3]);
				}
			}

			return idList;
		}

	}

	private String hasVideoElement(String detailText) {
		return hasSubElementNamed(detailText, "__video") ? "true" : "false";
	}

	private String hasShapeElement(String detailText) {
		return hasSubElementNamed(detailText, "shape") ? "true" : "false";
	}

	private boolean hasSubElementNamed(String detailText, String elementName) {
		try {
			Document cotDoc = DocumentHelper.parseText(detailText);
			Element detailElement = cotDoc.getRootElement();
			return detailElement.element(elementName) != null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public int getCountByUid(String uid) throws Exception {
		/* Not using user alias table */
		try (Connection connection = ds.getConnection(); PreparedStatement sqlQuery = wrap
				.prepareStatement("select count(thumbnail.cot_id) from cot_router cot, cot_thumbnail thumbnail "
						+ "where cot.uid=? and cot.id=thumbnail.cot_id", connection)) {
			// sqlQuery.setString(1, Sanitizer.checkUid(uid));
			sqlQuery.setString(1, uid);

			/* Using user alias table */
			/*
			 * String sqlQuery =
			 * "select count(thumbnail.cot_id) from cot_router cot, cot_thumbnail thumbnail, users "
			 * + " where users.name LIKE  " + "\'" + uid + "\'" +
			 * " and cot.uid LIKE users.uid  and cot.id = thumbnail.cot_id";
			 */

			// System.out.println(sqlQuery);
			ResultSet results = wrap.doQuery(sqlQuery);
			int count = 0;
			if (results != null && !results.isClosed()) {
				if (results.next())
					count = results.getInt(1);
			}
			return count;
		}
	}

	public int getCountOfCoT() throws Exception {
		/* Without user alias table */
		try (Connection connection = ds.getConnection(); PreparedStatement ps = wrap.prepareStatement("SELECT reltuples AS estimate FROM pg_class WHERE relname ='cot_router'", connection)) {
			ResultSet results = wrap.doQuery(ps);
			int count = 0;
			if (results != null && !results.isClosed()) {
				if (results.next())
					count = results.getInt(1);
			}
			return count;
		}
	}

	public int getCountOfImages() throws Exception {
		/* Without user alias table */
		try (Connection connection = ds.getConnection(); PreparedStatement ps = wrap.prepareStatement("SELECT reltuples AS estimate FROM pg_class WHERE relname ='cot_image'", connection)) {
			ResultSet results = wrap.doQuery(ps);
			int count = 0;
			if (results != null && !results.isClosed()) {
				if (results.next())
					count = results.getInt(1);
			}
			return count;
		}
	}


	public static Document buildCot(ResultSet results) throws SQLException,
	DocumentException {
		SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
		dateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));

		Document event = DocumentHelper.createDocument();
		Element eventE = event.addElement("event");
		event.setRootElement(eventE);

		eventE.addAttribute("version", "2.0");
		eventE.addAttribute("uid", results.getString("uid").trim());
		eventE.addAttribute("type", results.getString("cot_type"));
		eventE.addAttribute("how", results.getString("how"));
		eventE.addAttribute("time", dateFormat.format(results.getTimestamp("time")));
		eventE.addAttribute("start",
				dateFormat.format(results.getTimestamp("start")));
		eventE.addAttribute("stale",
				dateFormat.format(results.getTimestamp("stale")));
		String[] optionalAttrs = { "qos", "opex", "access" };
		for (String attrName : optionalAttrs) {
			if (results.getString(attrName) != null) {
				eventE.addAttribute(attrName, results.getString(attrName));
			}
		}

		Element pointE = eventE.addElement("point");
		pointE.addAttribute("ce",
				Double.toString(results.getDouble("point_ce")));
		pointE.addAttribute("le",
				Double.toString(results.getDouble("point_le")));
		pointE.addAttribute("hae",
				Double.toString(results.getDouble("point_hae")));

		String pointStr = results.getString("st_astext"); // format:
		// POINT(-120.794506
		// 35.747143)
		String parsedPoint = pointStr.substring(6, pointStr.length() - 1);
		int sep = parsedPoint.indexOf(' ');
		Double lon = Double.NaN;
		try {
			lon = Double.parseDouble(parsedPoint.substring(0, sep));
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
		}
		Double lat = Double.NaN;
		try {
			lat = Double.parseDouble(parsedPoint.substring(sep + 1));
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
		}

		pointE.addAttribute("lat", Double.toString(lat));
		pointE.addAttribute("lon", Double.toString(lon));

		String detailStr = results.getString("detail");
		Document detailDoc = DocumentHelper.parseText(detailStr);
		eventE.add(detailDoc.getRootElement());

		return event;
	}

	public static class CotMessageList {
		public long serverTimeQueryWasMadeMillis = System.currentTimeMillis();
		public String[][] cotInfo = null;
	}

	public String[][] getCotMessagesInBoundary(int limit, double x1, double y1,
			double x2, double y2, boolean shouldGroup) throws Exception {
		return getCotMessagesInBoundary(limit, x1, y1, x2, y2, shouldGroup, -1).cotInfo;
	}

	public CotMessageList getCotMessagesInBoundary(int limit, double x1, double y1,
			double x2, double y2, boolean shouldGroup, long sinceTimeMillis) throws Exception {
		// Construct SQL query
		String sqlQuery = "SELECT id, uid, ST_AsKML(event_pt), ST_AsText(event_pt), cot_type, servertime, detail "
				+ "FROM cot_router WHERE ";
		if(sinceTimeMillis > 0) {
			sqlQuery += " servertime > ? AND ";
		}
		if (shouldGroup) {
			sqlQuery += "id IN (select MAX(id) FROM cot_router GROUP BY uid) ";
		} else {
			sqlQuery += "event_pt && ST_GeomFromText('POLYGON((? ?, ? ?, ? ?, ? ?, ? ?)', 4326) "
					+ "AND _ST_Within(event_pt,ST_GeomFromText('POLYGON((? ?, ? ?, ? ?, ? ?)', 4326)) "
					+ "ORDER BY servertime desc";
		}
		sqlQuery += " LIMIT ?;";

		try (Connection connection = ds.getConnection(); PreparedStatement preparedQuery = wrap.prepareStatement(sqlQuery, connection)) {

			int columnIndex = 1;

			if(sinceTimeMillis > 0) {
				preparedQuery.setTimestamp(columnIndex++, new Timestamp(sinceTimeMillis));
			}

			if (!shouldGroup) {
				preparedQuery.setDouble(columnIndex++, x1);
				preparedQuery.setDouble(columnIndex++, y1);
				preparedQuery.setDouble(columnIndex++, x1);
				preparedQuery.setDouble(columnIndex++, y2);
				preparedQuery.setDouble(columnIndex++, x2);
				preparedQuery.setDouble(columnIndex++, y2);
				preparedQuery.setDouble(columnIndex++, x2);
				preparedQuery.setDouble(columnIndex++, y1);
				preparedQuery.setDouble(columnIndex++, x1);
				preparedQuery.setDouble(columnIndex++, y1);

				preparedQuery.setDouble(columnIndex++, x1);
				preparedQuery.setDouble(columnIndex++, y1);
				preparedQuery.setDouble(columnIndex++, x1);
				preparedQuery.setDouble(columnIndex++, y2);
				preparedQuery.setDouble(columnIndex++, x2);
				preparedQuery.setDouble(columnIndex++, y2);
				preparedQuery.setDouble(columnIndex++, x2);
				preparedQuery.setDouble(columnIndex++, y1);
				//preparedQuery.setDouble(19, x1);
				//preparedQuery.setDouble(20, y1);
			}
			preparedQuery.setInt(columnIndex++, limit);

			ResultSet results = wrap.doQuery(preparedQuery);
			Vector<String[]> ret = new Vector<String[]>();
			int NUMBER_OF_COLUMNS = 7;

			if (results != null && !results.isClosed()) {
				while (results.next()) {
					// build the String[] for the column
					String[] row = new String[NUMBER_OF_COLUMNS];
					for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
						if(i == 3) { // Strip out POINT(...)
							String s = results.getString(i + 1);
							row[i] = s.substring(6, s.indexOf(')'));
						}
						else
							row[i] = results.getString(i + 1);
					}
					ret.add(row);
				}
			}


			CotMessageList cotMessageList = new CotMessageList();
			cotMessageList.cotInfo = (String[][]) ret
					.toArray(new String[ret.size()][NUMBER_OF_COLUMNS]);
			return cotMessageList;
		}
	}

}
