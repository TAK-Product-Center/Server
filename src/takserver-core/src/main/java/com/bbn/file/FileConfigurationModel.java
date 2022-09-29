package com.bbn.file;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileConfigurationModel {
	
	private int uploadSizeLimit;
	
	public FileConfigurationModel() {
		this.uploadSizeLimit = 400;
	}

	public int getUploadSizeLimit() {
		return uploadSizeLimit;
	}

	public void setUploadSizeLimit(int uploadSizeLimit) {
		this.uploadSizeLimit = uploadSizeLimit;
	}

}
