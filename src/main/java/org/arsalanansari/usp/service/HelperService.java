package org.arsalanansari.usp.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

@Service
public class HelperService {

    DateTimeFormatter dtf=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    ZoneId zoneId=ZoneId.systemDefault();
    public static String expiredLink="http://dev.local:8991/expired";

    public String getDateAndTimeFromEpoch(long epoch){
        Instant instant=Instant.ofEpochMilli(epoch);
        ZonedDateTime zdt=instant.atZone(zoneId);
        return zdt.format(dtf);
    }
    
}
