

package com.bbn.cot.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.InvalidXPathException;
import org.dom4j.Node;

import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.util.DateUtil;
import com.google.common.base.Strings;

import tak.server.cot.CotEventContainer;

public class FlowTagFilter implements CotFilter {
	public static String DEFAULT_FLOWTAG_KEY = "filter.flowtag.text";
	public static String flowTagXPath = "/event/detail/_flow-tags_"; // xpath for getting to the flow tags element
	public static String flowTagAttrXPath = flowTagXPath + "/@*";	 // xpath for getting to the flow tag serverId/timestamp attribute pair
	public static Logger log = Logger.getLogger(FlowTagFilter.class.getCanonicalName());

	private static final String FLOWTAG_BASE = "TAK-Server-";

	private boolean warned = false; // flag for logging an empty server Id warning only once

	private ServerInfo serverInfo;

	public FlowTagFilter(ServerInfo serverInfo) {

		this.serverInfo = serverInfo;
	}

	@Override
	public CotEventContainer filter(CotEventContainer c) {
		if (!Strings.isNullOrEmpty(flowTag())) {
			// check to make sure detail field exists
			Element detailElem = c.getDocument().getRootElement()
					.element("detail");
			if(detailElem == null)
             {
                return c;  // invalid CoT, but this isn't the right place to stop it
            }

			Element flowTagElem = DocumentHelper.makeElement(detailElem, "/_flow-tags_");
			flowTagElem.addAttribute(flowTag(), DateUtil.toCotTime(System.currentTimeMillis()));
		} else if (!warned) {
			log.error("Flow tag filter not working due to an empty serverId -- unable to filter messages. Routing loops could occur with catastrophic results.");
			warned = true;
		}

		return c;
	}

	/**
	* Removes the flow tag filter *for this server* from a message, if there is a filter.
	*
	* Intended to modify this message so that this server's broker will OK the message.
	*
	* @note This method mutates the container's contained xml.
	* @return Flag indicating whether the message was modified.
	*/
	public boolean unfilter(CotEventContainer c) {
		return unfilter(c, flowTag());
	}

	/**
	* Removes all of the flow tag filters from a message.
	*
	* Intended to modify the message so that any federation route will be successful.
	*
	* @throw ClassCastException if the message's flow tag Nodes are not all of type element
	* @return The list of server Ids that were removed from the document. Empty if nothing was removed.
	*/
	public List<String> unfilterAll(CotEventContainer c) {
        return unfilterAll(c.getDocument());
	}

    public List<String> unfilterAll(Document doc) {
        @SuppressWarnings("unchecked")
        List<Node> nodes = doc.selectNodes(flowTagAttrXPath);
        List<String> removed = new ArrayList<String>(nodes.size());

        for (Node node : nodes) {
            removed.add(node.getName());
            node.detach();
        }

        return removed;
    }

	/**
	* Removes the flow tag filter for the given server id.
	*
	* @note In the case that the given xpath is malformed due to a flawed serverId, the error is caught, and false is returned.
	*
	* @return whether an attribute with the given serverId name was actually removed ie, whether the xpath /event/detail/_flow-tags_/@serverId existed
	*/
	public static boolean unfilter(CotEventContainer c, String serverId) {
        return unfilter(c.getDocument(), serverId);
	}

    public static boolean unfilter(Document doc, String serverId) {
        // select the _flow-tags_ attribute with the matching server id, if any
        boolean modified = false;

        try {
            String serverXPath = flowTagXPath + "/@" + serverId;
            Attribute serverAttr = (Attribute) doc.selectSingleNode(serverXPath);
            if (serverAttr != null) {
                serverAttr.detach();
                modified = true;
            }
        } catch (InvalidXPathException e) {
            log.error("Invalid xpath for serverID in unfilterExplicit: " + serverId);
        }

        return modified;
    }

    public String flowTag() {
    	return FLOWTAG_BASE + serverInfo.getServerId();
    }
}
