package com.bbn.marti.util.xml;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.Constants;

public class DateAdapter extends XmlAdapter<String, Date> {

    protected static final Logger logger = LoggerFactory.getLogger(DateAdapter.class);
    private SimpleDateFormat format = new SimpleDateFormat(Constants.COT_DATE_FORMAT);

    public DateAdapter() {
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String marshal(Date d) throws Exception {
        try {
            return format.format(d);
        } catch (Exception e) {
            logger.error("Failed to format date %s", d.toString(), e);
            return null;
        }
    }

    @Override
    public Date unmarshal(String d) throws Exception {
        if (d == null) {
            return null;
        }

        try {
            return format.parse(d);
        } catch (ParseException e) {
            logger.error("Failed to parse date %s", d, e);
            return null;
        }
    }
}