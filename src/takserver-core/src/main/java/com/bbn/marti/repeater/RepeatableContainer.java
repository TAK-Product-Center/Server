

package com.bbn.marti.repeater;

import java.util.Date;

import com.bbn.marti.remote.Repeatable;

import tak.server.cot.CotEventContainer;

/**
 * A repeatable message logically consists of a CoT message, and some metadata about that message
 * such as when it began to repeat, and what type of repeating message it is (geo-fence breach,
 * 911, ring-the bell, etc.). This metadata along with some of the core fields are extracted to
 * the Repeatable class, to allow sending just that data to the MartiWebApps via RMI.
 */
public class RepeatableContainer {
	private Repeatable repeatable;
	private CotEventContainer cotEventContainer;
	
	public RepeatableContainer(CotEventContainer cotEventContainer, String repeatableType) {
		repeatable = new Repeatable();
		repeatable.setXml(cotEventContainer.asXml());
		repeatable.setUid(cotEventContainer.getUid());
		repeatable.setDateTimeActivated(new Date());
		repeatable.setRepeatType(repeatableType);
		repeatable.setCallsign(cotEventContainer.getCallsign());
		repeatable.setCotType(cotEventContainer.getType());
		
		this.cotEventContainer = cotEventContainer;
	}
	
	public Repeatable getRepeatable() {
		return repeatable;
	}
	public void setRepeatable(Repeatable repeatable) {
		this.repeatable = repeatable;
	}
	public CotEventContainer getCotEventContainer() {
		return cotEventContainer;
	}
	public void setCotEventContainer(CotEventContainer cotEventContainer) {
		this.cotEventContainer = cotEventContainer;
	}
}
