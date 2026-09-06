package org.arsalanansari.usp.Base62.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HelperService {

    @Value("${app.base-url}")
    private String baseUrl;

    private static DateTimeFormatter dtf=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static ZoneId zoneId=ZoneId.systemDefault();
    public static final String expired="expired";
    public static final String exhaust="exhaust";

    public static String getDateAndTimeFromEpoch(long epoch){
        Instant instant=Instant.ofEpochMilli(epoch);
        ZonedDateTime zdt=instant.atZone(zoneId);
        return zdt.format(dtf);
    }
    public String computeUrl(String url){
        return baseUrl+"/"+url;
    }
    public String computeTypeUrl(String type){
        if(type==null || type.isBlank()){
            return null;
        }
        switch(type){
            case HelperService.expired:
                return baseUrl+"/expired";
            case HelperService.exhaust:
                return baseUrl+"/exhaustlimit";
            default:
                log.warn("Unknown link type requested: {}", type);
                return null;
        }
    }
    
}
