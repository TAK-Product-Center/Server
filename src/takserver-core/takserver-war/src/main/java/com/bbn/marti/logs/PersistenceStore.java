/*
 */

package com.bbn.marti.logs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.JDBCQueryAuditLogHelper;
import com.bbn.security.web.MartiValidator;

public class PersistenceStore {

	protected static MartiValidator validator = new MartiValidator();
	protected static final Logger logger = LoggerFactory.getLogger(PersistenceStore.class);
	
	@Autowired
	private JDBCQueryAuditLogHelper wrapper;
	
	@Autowired
	private DataSource ds;
			
	public boolean addLog(Log errorLog) {

    	boolean status = false;
    	
    	try (Connection connection = ds.getConnection(); PreparedStatement insert = wrapper.prepareStatement(
				"insert into error_logs ( uid, callsign, platform, " +
				"major_version, minor_version, filename, contents ) " +
				"values ( ?, ?, ?, ?, ?, ?, ? )", connection)) {
    		
    		validator.getValidInput("errorLog", errorLog.getUid(), 
					MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
			validator.getValidInput("errorLog", errorLog.getCallsign(), 
					MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
			validator.getValidInput("errorLog", errorLog.getPlatform(), 
					MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
			validator.getValidInput("errorLog", errorLog.getMajorVersion(), 
					MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
			validator.getValidInput("errorLog", errorLog.getMinorVersion(), 
					MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
			validator.getValidInput("errorLog", errorLog.getFilename(), 
					MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
    		
			insert.setString(1, errorLog.getUid());
			insert.setString(2, errorLog.getCallsign());
			insert.setString(3, errorLog.getPlatform());
			insert.setString(4, errorLog.getMajorVersion());
			insert.setString(5, errorLog.getMinorVersion());
			insert.setString(6, errorLog.getFilename());
			insert.setBytes(7, errorLog.getContents());
			
			insert.executeUpdate();
			insert.close(); 	    		
			
	    } catch (Exception e) {
	    	status = false;
	        logger.error("Exception!", e);
	    } 
    	
    	return status;
	}

	public static Log logFromResultSet(ResultSet results, boolean includeLogContents, boolean onlyCallstack) {
		
		Log log = new Log();
    	try
    	{
			int id = Integer.valueOf(results.getString("id"));
			log.setId(id);
			log.setUid(results.getString("uid"));
			log.setCallsign(results.getString("callsign"));
			log.setPlatform(results.getString("platform"));
			log.setMajorVersion(results.getString("major_version"));
			log.setMinorVersion(results.getString("minor_version"));
			log.setFilename(results.getString("filename"));
			log.setTime(results.getTimestamp("time"));

			if (includeLogContents) {
				String logz = null;
				try
				{
					logz = results.getString("log");
				} catch (SQLException e)
				{
					logz = null;
				}

				if (logz != null && logz.length() > 0) {
					log.setContents(logz.getBytes());
				} else {
					log.setContents(results.getBytes("contents"));
				}

				if (onlyCallstack) {
					log.storeCallstacks();
					log.setLog(null);
					log.setContents(null);
				}
			}

	    } catch (Exception e) {
			log = null;
	        e.printStackTrace();
	        logger.error("Exception!", e);
	    } 
    	
		return log;
	}
	
	public Log getLog(int id) {

		Log log = null;
		
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("select * from error_logs where id = ?", connection)) {
			query.setInt(1, id);
			try (ResultSet results =  query.executeQuery()) {        	
				if (results.next()) {
					log = logFromResultSet(results, true, false);
				}
			}

		} catch (Exception e) {
			logger.error("Exception!", e);
		}     


		return log;
	}

	public HashMap<String, List<Log>> getUniqueErrorLogs(String query) {

		HashMap<String, List<Log>> uniques = new HashMap<String, List<Log>>();

		for (Log log : getLogs(query, false, true, true)) {
			List<String> callstacks = log.getCallstacks();
			if (callstacks == null) {
				logger.error("found null callstacks!");
				continue;
			}

			for (String callstack : callstacks) {
				List<Log> errorLogs = uniques.get(callstack);
				if (errorLogs == null) {
					errorLogs = new ArrayList<Log>();
					uniques.put(callstack, errorLogs);
				}
				errorLogs.add(log);
			}
		}

		return uniques;
	}

	public List<Log> getLogs(String query, boolean metrics, boolean includeLogContents, boolean onlyCallstacks) {

		List<Log> logs = new ArrayList<Log>();
		try 	{
			String sql = "select id, uid, callsign, platform, major_version, minor_version, ";

			if (includeLogContents) {
				sql += "log, contents, ";
			}

			sql +=	"filename, time " +
					" from error_logs where filename ";
			if (!metrics) {
				sql += " not ";
			}
			sql += " like 'metric%'";

			boolean hasQuery = query != null && query.length() != 0;
			if (hasQuery) {
				validator.getValidInput("feed", query, 
						MartiValidator.Regex.MartiSafeString.name(), MartiValidator.LONG_STRING_CHARS, true);
				sql += " and ( ";
				sql += " uid like ? or ";
				sql += " callsign like ? or ";
				sql += " platform like ? or ";
				sql += " major_version like ? or ";
				sql += " minor_version like ? or ";
				sql += " filename like ? or ";
				sql += " log like ?";
				sql += " ) ";
			}

			sql += " order by time desc";

			try (Connection connection = ds.getConnection(); PreparedStatement select = wrapper.prepareStatement(sql, connection)) {

				if (hasQuery) {
					select.setString(1, "%" + query + "%");
					select.setString(2, "%" + query + "%");
					select.setString(3, "%" + query + "%");
					select.setString(4, "%" + query + "%");
					select.setString(5, "%" + query + "%");
					select.setString(6, "%" + query + "%");
					select.setString(7, "%" + query + "%");
				}

				ResultSet results = select.executeQuery();

				while (results.next()) {
					Log log = logFromResultSet(results, includeLogContents, onlyCallstacks);
					logs.add(log);
				}

				results.close();

			} catch (Exception e) {
				logger.error("Exception!", e);
			}     

		} catch (ValidationException e) {
			logger.warn("validation exception in error logs peristence store", e);
		} finally { }
		return logs;
	}	

	public void deleteLog(String id) {

		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement(
				"delete from error_logs where id = ANY ( ? )", connection)){
			String[] ids = id.split(",");

			query.setArray(1, wrapper.createArrayOf("INTEGER", ids, connection));
			String sql = query.toString();
			logger.debug(sql);

			query.executeUpdate();
			query.close();

		} catch (Exception e) {
			logger.error("Exception deleting from error logs table", e);
		}       	
	}
}
