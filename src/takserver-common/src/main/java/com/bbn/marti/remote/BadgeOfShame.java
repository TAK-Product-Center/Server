

package com.bbn.marti.remote;

import java.io.Serializable;

/**
 * Data structure that captures information about a malformed CoT message, for display on the Wall of Shame.
 *
 */
public class BadgeOfShame implements Serializable {
  
  public static String SHAMEWALL_KEY = "wallOfShame";
  
	/**
   * 
   */
  private static final long serialVersionUID = 181542079187794296L;
  /**
	 * The offending CoT message or garbled attempt at a CoT message. Can be empty, can't be null.
	 */
	private String cot = "";
  private String source = "";

  /**
	 * The Exception that the CoT router caught while trying to process the message. Can be null.
	 */
	private Exception exception = null;
	
	/**
	 * @return the offending CoT message, or as much as could be stored. Can return an empty string.
	 */
	public String getCot() {
		return this.cot;
	}
	
	public String getSource() {
	  return this.source;
	}
	
	/**
	 * @return the Exception that the CoT router caught while trying to process the message, or null if not available.
	 */
	public Exception getException() {
		return this.exception;
	}
	
	public BadgeOfShame(String content, String source, Exception exception) {
		if (content != null) {
			this.cot = content;
		}
		if(source != null) {
		  this.source = source;
		}
		this.exception = exception;
	}
	
}
