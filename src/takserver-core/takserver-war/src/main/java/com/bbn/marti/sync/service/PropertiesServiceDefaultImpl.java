package com.bbn.marti.sync.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.bbn.marti.remote.util.ConcurrentMultiHashMap;
import com.google.common.collect.Multimap;

public class PropertiesServiceDefaultImpl  implements PropertiesService {
	
	private static final Logger logger = LoggerFactory.getLogger(PropertiesServiceDefaultImpl.class);
	
	private final DataSource dataSource;
	
	public PropertiesServiceDefaultImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	@SuppressWarnings("deprecation")
	public List<String> findAllUids() {
		String sql = "select uid from properties_uid;";

		return new JdbcTemplate(dataSource).query(sql,
				new Object[] { },
				new ResultSetExtractor<List<String>>() {
			@Override
			public List<String> extractData(ResultSet results) throws SQLException {
				if (results == null) {
					throw new IllegalStateException("null result set");
				}

				List<String> values = new ArrayList<String>();
				
				while (results.next()) {
					String value = results.getString(1);

					values.add(value);
				}

				return values;
			}
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	public Map<String, Collection<String>> getKeyValuesByUid(String uid) {
		String sql = "select key, value from properties_uid  "
				+ "join properties_keys on properties_uid.id = properties_keys.properties_uid_id " 
				+ "join properties_value on properties_value.properties_key_id = properties_keys.id where uid = ?;";

		return new JdbcTemplate(dataSource).query(sql,
				new Object[] { uid },
				new ResultSetExtractor<Map<String, Collection<String>>>() {
			@Override
			public Map<String, Collection<String>> extractData(ResultSet results) throws SQLException {
				if (results == null) {
					throw new IllegalStateException("null result set");
				}

				Multimap<String, String> uidKvMap = new ConcurrentMultiHashMap<String, String>();
				
				while (results.next()) {
					String key = results.getString(1);
					String value = results.getString(2);

					uidKvMap.put(key,value);
				}

				return uidKvMap.asMap();
			}
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<String> getValuesByKeyAndUid(String uid, String key) {
		String sql = "select value from properties_uid  "
				+ "join properties_keys on properties_uid.id = properties_keys.properties_uid_id " 
				+ "join properties_value on properties_value.properties_key_id = properties_keys.id "
				+" where uid = ? and key = ?;";

		return new JdbcTemplate(dataSource).query(sql,
				new Object[] { uid, key },
				new ResultSetExtractor<List<String>>() {
			@Override
			public List<String> extractData(ResultSet results) throws SQLException {
				if (results == null) {
					throw new IllegalStateException("null result set");
				}

				List<String> values = new ArrayList<String>();
				
				while (results.next()) {
					String value = results.getString(1);

					values.add(value);
				}

				return values;
			}
		});
	}

	@Override
	public void putKeyValue(String uid, String key, String value) {
		//First merge uid table, next merge key table, then add value
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("uid", uid);
		String sqlMergeUid = "merge into properties_uid pu " 
				+ " using (values(:uid)) nu "
				+ " on nu.column1 = pu.uid"
				+ " when not matched then insert (uid) values (nu.column1);";
		new NamedParameterJdbcTemplate(dataSource).update(sqlMergeUid, namedParameters);
		
		namedParameters.addValue("key", key);
		String sqlMergeKey = "merge into properties_keys pk " 
				+ " using (values(:key, (select id from properties_uid where uid=:uid))) nk "
				+ " on nk.column1 = pk.key and nk.column2 = pk.properties_uid_id"
				+ " when not matched then insert (key, properties_uid_id) values(nk.column1, nk.column2);";
		new NamedParameterJdbcTemplate(dataSource).update(sqlMergeKey, namedParameters);
		
		namedParameters.addValue("value", value);
		String sqlAddValue = "insert into properties_value (value, properties_key_id) values"
				+ " (:value, (select id from properties_keys where key=:key"
				+ " and properties_uid_id = (select id from properties_uid where uid=:uid)));";
		new NamedParameterJdbcTemplate(dataSource).update(sqlAddValue, namedParameters);
		
	}

	@Override
	public void deleteKey(String uid, String key) {
		//Delete from keys table, cascades by foreign key to properties_values
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("uid", uid);
		namedParameters.addValue("key", key);
		String sqlDeleteKey = "delete from properties_keys where " 
				+ " key =:key and properties_uid_id =(select id from properties_uid where uid=:uid);";
		new NamedParameterJdbcTemplate(dataSource).update(sqlDeleteKey, namedParameters);
	}

	@Override
	public void deleteAllKeysByUid(String uid) {
		//Delete from keys table, cascades by foreign key to properties_values
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("uid", uid);		
		String sqlDeleteKey = "delete from properties_keys where" 
				+ " properties_uid_id =(select id from properties_uid where uid=:uid);";
		new NamedParameterJdbcTemplate(dataSource).update(sqlDeleteKey, namedParameters);
	}
	

}
