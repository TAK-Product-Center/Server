package tak.server.retention.config;


import java.io.Serializable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties
@PropertySource(name="mission-archive-cron-sched", factory=YamlPropertySourceFactory.class, value="file:conf/retention/mission-archiving-config.yml")
public class MissionArchivingCronConfig implements Serializable {

    private String missionCronExpression = "-";
    private double removeFromArchiveAfterDays = 1065;
    private double timeToArchiveAfterNoActivityDays = 365;
    private boolean archiveMissionByNoContentActivity = false;
    private boolean archiveMissionByNoSubscriptionActivity = false;
    
	public String getMissionCronExpression() {
		return missionCronExpression;
	}
	public void setMissionCronExpression(String missionCronExpression) {
		this.missionCronExpression = missionCronExpression;
	}
	public double getRemoveFromArchiveAfterDays() {
		return removeFromArchiveAfterDays;
	}
	public void setRemoveFromArchiveAfterDays(double removeFromArchiveAfterDays) {
		this.removeFromArchiveAfterDays = removeFromArchiveAfterDays;
	}
	public double getTimeToArchiveAfterNoActivityDays() {
		return timeToArchiveAfterNoActivityDays;
	}
	public void setTimeToArchiveAfterNoActivityDays(double timeToArchiveAfterNoActivityDays) {
		this.timeToArchiveAfterNoActivityDays = timeToArchiveAfterNoActivityDays;
	}
	public boolean isArchiveMissionByNoContentActivity() {
		return archiveMissionByNoContentActivity;
	}
	public void setArchiveMissionByNoContentActivity(boolean archiveMissionByNoContentActivity) {
		this.archiveMissionByNoContentActivity = archiveMissionByNoContentActivity;
	}
	public boolean isArchiveMissionByNoSubscriptionActivity() {
		return archiveMissionByNoSubscriptionActivity;
	}
	public void setArchiveMissionByNoSubscriptionActivity(boolean archiveMissionByNoSubscriptionActivity) {
		this.archiveMissionByNoSubscriptionActivity = archiveMissionByNoSubscriptionActivity;
	}
	
	@Override
	public String toString() {
		return "MissionArchivingCronConfig [missionCronExpression=" + missionCronExpression + ", removeFromArchiveAfterDays="
				+ removeFromArchiveAfterDays + ", timeToArchiveAfterNoActivityDays=" + timeToArchiveAfterNoActivityDays
				+ ", archiveMissionByNoContentActivity=" + archiveMissionByNoContentActivity
				+ ", archiveMissionByNoSubscriptionActivity=" + archiveMissionByNoSubscriptionActivity + "]";
	} 
}
