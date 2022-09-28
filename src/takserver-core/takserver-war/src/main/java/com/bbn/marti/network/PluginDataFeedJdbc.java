/*
 * Copyright (c) 2013-2015 Raytheon BBN Technologies. Licensed to US Government with unlimited rights.
 */

package com.bbn.marti.network;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import tak.server.feeds.DataFeed.DataFeedType;
import tak.server.plugins.PluginDataFeed;


public class PluginDataFeedJdbc {

	Logger logger = LoggerFactory.getLogger(PluginDataFeedJdbc.class);

	@Autowired
	private DataSource dataSource;
	
	public List<PluginDataFeed> getPluginDataFeeds(){
		
		HashMap<String, PluginDataFeed> hashTable = new HashMap<>();
		
		List<PluginDataFeedWithTagEntry> dbItems = queryPluginDataFeedWithTagEntries();

		for (PluginDataFeedWithTagEntry dbItem: dbItems) {
			PluginDataFeed pluginFeed = hashTable.get(dbItem.getUuid());
			if (pluginFeed == null) {
				List<String> tags = new ArrayList<String>();
				if (dbItem.getTag() != null) {
					tags.add(dbItem.getTag());
				}
				pluginFeed = new PluginDataFeed(dbItem.getUuid(), dbItem.getName(), tags, dbItem.isArchive(), dbItem.isSync());
				hashTable.put(dbItem.getUuid(),pluginFeed);
			}else {
				if (dbItem.getTag() != null) {
					pluginFeed.getTags().add(dbItem.getTag());
				}
			}
		}
    	
		List<PluginDataFeed> allPluginDatafeeds = new ArrayList<PluginDataFeed>(hashTable.values());
		return allPluginDatafeeds;
	}

	private List<PluginDataFeedWithTagEntry> queryPluginDataFeedWithTagEntries(){
		
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		String query = "select uuid, name, tag, archive, sync from data_feed left join data_feed_tag on data_feed_tag.data_feed_id = data_feed.id where type = " + DataFeedType.Plugin.ordinal();
		
		List<PluginDataFeedWithTagEntry> result = jdbcTemplate.query(query, new PluginDataFeedWithTagEntryMapper());

		if (logger.isDebugEnabled()) {
			logger.debug("result size: " + result.size());
		}

		return result;
	}

	private class PluginDataFeedWithTagEntryMapper implements RowMapper<PluginDataFeedWithTagEntry> {

		@Override
		public PluginDataFeedWithTagEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
			PluginDataFeedWithTagEntry pluginDataFeedWithTagEntry = new PluginDataFeedWithTagEntry();
			pluginDataFeedWithTagEntry.setUuid(rs.getString("uuid"));
			pluginDataFeedWithTagEntry.setName(rs.getString("name"));
			pluginDataFeedWithTagEntry.setTag(rs.getString("tag"));
			pluginDataFeedWithTagEntry.setArchive(rs.getBoolean("archive"));
			pluginDataFeedWithTagEntry.setSync(rs.getBoolean("sync"));

			return pluginDataFeedWithTagEntry;
		}
			
	}
	
	class PluginDataFeedWithTagEntry implements Serializable{
		
		private static final long serialVersionUID = 5919115432876484174L;

		private String uuid;
		
		private String name;
		
		private String tag;
		
		private boolean archive;
		
		private boolean sync;
		
		public PluginDataFeedWithTagEntry() {
		}
				
		public PluginDataFeedWithTagEntry(String uuid, String name, String tag, boolean archive, boolean sync) {
			super();
			this.uuid = uuid;
			this.name = name;
			this.tag = tag;
			this.archive = archive;
			this.sync = sync;
		}

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		public boolean isArchive() {
			return archive;
		}

		public void setArchive(boolean archive) {
			this.archive = archive;
		}

		public boolean isSync() {
			return sync;
		}

		public void setSync(boolean sync) {
			this.sync = sync;
		}

		@Override
		public String toString() {
			return "PluginDataFeedWithTagEntry [uuid=" + uuid + ", name=" + name + ", tag=" + tag + ", archive="
					+ archive + ", sync=" + sync + "]";
		}
		
	}

}
