/*
 * 
 * The VideoConnections class stores a collection of Feeds. 
 * 
 */


package com.bbn.marti.video;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "videoConnections")
public class VideoConnections {

    public VideoConnections() {
		this.feeds = new ArrayList<Feed>();
    }

	@XmlElement(name = "feed")
	private List<Feed> feeds = null;

	public List<Feed> getFeeds() {
		return feeds;
	}
}
