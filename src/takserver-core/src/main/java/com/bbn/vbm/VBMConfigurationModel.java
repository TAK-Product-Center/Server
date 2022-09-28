package com.bbn.vbm;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VBMConfigurationModel {
	
	private boolean vbmEnabled;
	private boolean saDisabled;
	private boolean chatDisabled;
	
	public VBMConfigurationModel() {
		this.vbmEnabled = false;
	}

	public boolean isVbmEnabled() {
		return vbmEnabled;
	}

	public void setVbmEnabled(boolean vbmEnabled) {
		this.vbmEnabled = vbmEnabled;
	}

	public boolean isChatDisabled() {
		return chatDisabled;
	}

	public void setChatDisabled(boolean chatDisabled) {
		this.chatDisabled = chatDisabled;
	}

	public boolean isSADisabled() {
		return saDisabled;
	}

	public void setSADisabled(boolean saDisabled) {
		this.saDisabled = saDisabled;
	}
}
