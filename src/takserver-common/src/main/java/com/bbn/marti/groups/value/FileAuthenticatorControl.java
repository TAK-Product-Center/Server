package com.bbn.marti.groups.value;

import com.bbn.marti.xml.bindings.UserAuthenticationFile;

public class FileAuthenticatorControl {
	
	public enum ControlType { USER_UPDATE, USER_ADD, USER_DELETE, USER_PASSWORD_CHANGE_WITHOUT_OLD_PASSWORD }
	
	private ControlType controlType;
	private UserAuthenticationFile.User fileUser;
	
	public static FileAuthenticatorControl add(UserAuthenticationFile.User fileUser) {
		return new FileAuthenticatorControl(ControlType.USER_ADD, fileUser);
	}
	
	public static FileAuthenticatorControl update(UserAuthenticationFile.User fileUser) {
		return new FileAuthenticatorControl(ControlType.USER_UPDATE, fileUser);
	}
	
	public static FileAuthenticatorControl delete(UserAuthenticationFile.User fileUser) {
		return new FileAuthenticatorControl(ControlType.USER_DELETE, fileUser);
	}
	
	public static FileAuthenticatorControl passwordChangeWithoutOldPassword(UserAuthenticationFile.User fileUser) {
		return new FileAuthenticatorControl(ControlType.USER_PASSWORD_CHANGE_WITHOUT_OLD_PASSWORD, fileUser);
	}
	
	private FileAuthenticatorControl(ControlType controlType, UserAuthenticationFile.User fileUser) {
		this.controlType = controlType;
		this.fileUser = fileUser;
	}
	
	public ControlType getControlType() {
		return this.controlType;
	}
	
	public UserAuthenticationFile.User getFileUser() {
		return this.fileUser;
	}
}
