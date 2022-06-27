package com.bbn.marti.citrap;

import java.util.List;
import java.util.NavigableSet;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.citrap.reports.ReportType;
import com.bbn.marti.config.Citrap;
import com.bbn.marti.remote.SubmissionInterface;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.util.DateUtil;

public class CITrapReportNotifications {

    private static final int STALE = 7 * 24 * 60 * 60 * 1000; // 1 week

    @Autowired
    private PersistenceStore persistenceStore;

    @Autowired
    private SubmissionInterface submission;

    private static String getReportNotificationCot(String senderUid, String senderCallsign, String destUid, double lon, double lat, String reportSummary, String cotType) {
        String time = DateUtil.toCotTime(System.currentTimeMillis());
        String staleTime = DateUtil.toCotTime(System.currentTimeMillis() + STALE);
        String cot = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
                + "<event version='2.0' uid='" + senderUid + "' type='" + cotType + "' "
                + "time='" + time + "' start='" +time + "' stale='" + staleTime + "' how='m-g'>"
                +		"<point lat='" + lat + "' lon='" + lon + "' hae='0.0' ce='0.0' le='0.0' />"
                + 		"<detail>"
                +           "<archive />"
                +           "<contact callsign='" +  senderCallsign + " CI Report'/>"
                + 			"<marti><dest uid='" + destUid + "'/></marti>"
                +           "<remarks>" + reportSummary + "</remarks>"
                + 		"</detail>"
                + "</event>";
        return cot;
    }

    private static String getSummary(ReportType report) {
        String summary = "CI-TRAP Report Info - ";
        summary += "Type: " + report.getType() + " - ";
        summary += "Title: " + report.getTitle() + " - ";
        summary += "Callsign: " + report.getUserCallsign() + " - ";
        summary += "User Desc: " + report.getUserDescription() + " - ";
        summary += "Date: " + report.getDateTime() + " - ";
        summary += "Date Desc: " + report.getDateTimeDescription() + " - ";
        summary += "Location Desc: " + report.getLocationDescription() + " - ";
        summary += "Event Scale: " + report.getEventScale() + " - ";
        summary += "Scale Desc: " + report.getScaleDescription() + " - ";
        summary += "Importance: " + report.getImportance();
        return summary;
    }

    public void notifyNonMissionSubscribersWithinRange(
            String groupVector, ReportType report, String missionName, double lon, double lat,
            SubscriptionManagerLite subscriptionManager,
            NavigableSet<Group> groups, Citrap config) throws  Exception {

        String notificationCotType = config == null ? "a-h-G-U-C-R" : config.getNotificationCot();
        String nonsubscriberCotFilter = config == null ? "a-f%" : config.getNonsubscriberCotFilter();
        int searchRadius = config == null ? 100000 : config.getSearchRadius();
        int searchSecago = config == null ? 300 : config.getSearchSecago();

        // get everyone in range of the report
        List<String> inRange = persistenceStore.getUidsInRangeFromPoint(
                nonsubscriberCotFilter, groupVector, searchSecago, lon, lat, searchRadius);

        // get everyone subscribed to the top level mission
        List<String> missionSubscribers = subscriptionManager.getMissionSubscriptions(missionName, true);

        String senderUid = UUID.randomUUID().toString();
        // iterate over users in range who are not subscribed to the top level mission
        for (String nonSubscriber : CollectionUtils.subtract(inRange, missionSubscribers)) {
            // notify users of new report
            submission.submitCot(
                    getReportNotificationCot(senderUid, report.getUserCallsign(), nonSubscriber, lon, lat, getSummary(report), notificationCotType),
                    groups);
        }
    }
}
