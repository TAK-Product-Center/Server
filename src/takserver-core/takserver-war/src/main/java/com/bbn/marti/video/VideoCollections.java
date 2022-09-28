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


@XmlRootElement(name = "videoCollections")
public class VideoCollections {

    public VideoCollections() {
        this.videoConnections = new ArrayList<VideoConnection>();
    }

    @XmlElement(name = "videoCollection")
    private List<VideoConnection> videoConnections  = null;

    public List<VideoConnection> getVideoConnections() {
        return videoConnections;
    }
}
