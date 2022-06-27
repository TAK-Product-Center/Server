package com.bbn.marti.remote;

import java.io.Serializable;

public class MissionArchiveConfig implements Serializable {
	
	String cronExpression = "-";
	Boolean archiveMissionByNoContentActivity = false;
	Boolean archiveMissionByNoSubscriptionActivity = false;
	double timeToArchiveAfterNoActivityDays = 365;
	double removeFromArchiveAfterDays = 1065;
	
	public String getCronExpression() {
		return cronExpression;
	}
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}
	public Boolean getArchiveMissionByNoContentActivity() {
		return archiveMissionByNoContentActivity;
	}
	public void setArchiveMissionByNoContentActivity(Boolean archiveMissionByNoContentActivity) {
		this.archiveMissionByNoContentActivity = archiveMissionByNoContentActivity;
	}
	public Boolean getArchiveMissionByNoSubscriptionActivity() {
		return archiveMissionByNoSubscriptionActivity;
	}
	public void setArchiveMissionByNoSubscriptionActivity(Boolean archiveMissionByNoSubscriptionActivity) {
		this.archiveMissionByNoSubscriptionActivity = archiveMissionByNoSubscriptionActivity;
	}
	public double getTimeToArchiveAfterNoActivityDays() {
		return timeToArchiveAfterNoActivityDays;
	}
	public void setTimeToArchiveAfterNoActivityDays(double timeToArchiveAfterNoActivityDays) {
		this.timeToArchiveAfterNoActivityDays = timeToArchiveAfterNoActivityDays;
	}
	public double getRemoveFromArchiveAfterDays() {
		return removeFromArchiveAfterDays;
	}
	public void setRemoveFromArchiveAfterDays(double removeFromArchiveAfterDays) {
		this.removeFromArchiveAfterDays = removeFromArchiveAfterDays;
	}
	
	@Override
	public String toString() {
		return "MissionArchiveConfig [cronExpression=" + cronExpression + ", archiveMissionByNoContentActivity="
				+ archiveMissionByNoContentActivity + ", archiveMissionByNoSubscriptionActivity="
				+ archiveMissionByNoSubscriptionActivity + ", timeToArchiveAfterNoActivityDays="
				+ timeToArchiveAfterNoActivityDays + ", removeFromArchiveAfterDays=" + removeFromArchiveAfterDays + "]";
	}
}
