package tak.db.profiler.table;

import java.sql.Connection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseStatistics {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseStatistics.class);
	
	List<TableStatistic> databaseStatistics = new ArrayList<>();
		
	public void initializeTable(DataSource dataSource) {
		Connection conn = null;
		try {		    
			conn = dataSource.getConnection();
		   	ResultSet rs = conn.getMetaData().getTables(dataSource.getConnection().getCatalog(), "", null, new String[]{"TABLE", "SEQUENCE"});
		   	while (rs.next()) {
		   		String tableName = rs.getString(3);
		   		
		   		boolean isSeq = tableName.endsWith("_seq");
		   		
		   		String query = "";
		   		if (isSeq) {
		   			query = "SELECT last_value FROM " + tableName;
		   		} else {
		   			query = "SELECT COUNT(*) FROM " + tableName;
		   		}
		   		
		   		ResultSet rs2 = conn.prepareStatement(query).executeQuery();
		   		while (rs2.next()) {
		   			long res = rs2.getLong(1);
		   			
		   			TableStatistic tableStatistic = new  TableStatistic();
		   			tableStatistic.setTableName(tableName);
		   			
		   			if (isSeq) {
		   				tableStatistic.setLastValue(res);
			   		} else {
			   			tableStatistic.setRowCount(res);
			   		}
		   			
		   			databaseStatistics.add(tableStatistic);
		   		}
		   	}
		} catch (SQLException e) {
			logger.error("Error populating table statistics", e);
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.error("error closing connection", e);
			}
		}
	}
	
	public List<TableStatistic> getDatabaseStatistics() {
		return databaseStatistics;
	}

	public void setDatabaseStatistics(List<TableStatistic> databaseStatistics) {
		this.databaseStatistics = databaseStatistics;
	}

	@Override
	public String toString() {
		return "DatabaseStatistics [databaseStatistics=" + databaseStatistics + "]";
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TableStatistic {
		private String tableName;
		private long rowCount;
		private long lastValue;
		
		public String getTableName() {
			return tableName;
		}
		public long getRowCount() {
			return rowCount;
		}
		public void setTableName(String tableName) {
			this.tableName = tableName;
		}
		public void setRowCount(long rowCount) {
			this.rowCount = rowCount;
		}
		public long getLastValue() {
			return lastValue;
		}
		public void setLastValue(long lastValue) {
			this.lastValue = lastValue;
		}
		@Override
		public String toString() {
			return "TableStatistic [tableName=" + tableName + ", rowCount=" + rowCount + ", lastValue=" + lastValue
					+ "]";
		}
	}
}
