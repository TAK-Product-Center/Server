package com.bbn.marti.util;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.Map;

public class TimeUtils {

    public static final Date MIN_TS = new Date(0L);
    public static final Date MAX_TS = new Date(253370764800000L);

    // make sure that secago, start and end are consistent
    public static Map.Entry<Date, Date> validateTimeInterval(@Nullable Long secago, @Nullable Date start, @Nullable Date end) {
        // override start with secago if it is provided
        if (secago != null && secago > 0) {
            start = new DateTime().minusSeconds(secago.intValue()).toDate();
        }

        if (start == null) {
            start = MIN_TS;
        }

        if (end == null) {
            end = MAX_TS;
        }

        // validate time interval
        if (!start.before(end)) {
            throw new IllegalArgumentException("invalid time range");
        }

        return Maps.immutableEntry(start, end);
    }
}
