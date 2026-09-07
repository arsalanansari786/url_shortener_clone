package org.arsalanansari.usp.singleInstanceCounter.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.arsalanansari.usp.singleInstanceCounter.Repository.UrlRepository;
import org.arsalanansari.usp.singleInstanceCounter.model.UrlModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UrlService {
    @Autowired
    private UrlRepository urlRepository;

         @Autowired
    private HelperService helperService;

    private static final long MOD = (long) 1e9 + 7;
    private static AtomicLong counterVar = new AtomicLong(0);

    private ConcurrentHashMap<String, UrlModel> map = new ConcurrentHashMap<>();

    public String fetchLongUrl(String url){
        try {
            if(map.containsKey(url)){
                UrlModel urlModel=map.get(url);
                if(urlModel.getExpireEpoch()>System.currentTimeMillis()){
                    urlModel.setClickCount(urlModel.getClickCount()+1);
                    saveInternally(url, urlModel);
                    log.debug("Cache hit for short code {}", url);
                    return urlModel.getLongUrl();
                }
                log.info("Short code {} has expired", url);
                return helperService.computeTypeUrl(HelperService.expired);
            }
            return fetchFromDb(url);
        } catch (Exception e) {
            log.error("Error fetching long url for short code: {}", url, e);
        }
        return null;
    }

    protected String fetchFromDb(String shortUrl) {
        try {
            Optional<UrlModel> urlModeOptional = urlRepository.findById(shortUrl);
            if (urlModeOptional.isPresent()) {
                UrlModel urlModel = urlModeOptional.get();
                long current = System.currentTimeMillis();
                if (urlModel.getExpireEpoch() > current) {
                    urlModel.setClickCount(urlModel.getClickCount() + 1);
                    urlRepository.save(urlModel);
                    //update in Map
                    map.put(shortUrl,urlModel);
                    log.debug("DB hit for short code {}", shortUrl);
                    return urlModel.getLongUrl();
                }
             log.info("Short code {} has expired", shortUrl);
             return helperService.computeTypeUrl(HelperService.expired);
            }
        } catch (Exception e) {
            log.error("Error fetching short code {} from DB", shortUrl, e);
        }
        return null;
    }

    public String getShortUrl(String url) {
        try {
            if(url==null || url.isBlank()){
                return null;
            }
            String shorturl = getBase64Hash(url);
            int count=1;
            while (shorturl != null && !shorturl.isBlank() && (map.containsKey(shorturl) || this.existsById(shorturl))) {
                if(count>10){
                    shorturl=null;
                    break;
                }
                count++;
                shorturl = getBase64Hash(url);
            }
            if(shorturl == null || shorturl.isBlank()){
                log.warn("Exhausted retry limit generating a unique short code for: {}", url);
                return helperService.computeTypeUrl(HelperService.exhaust);
            }
            // expire after 5 minutes
            long expireTime = 120 * 1000 + System.currentTimeMillis();
            UrlModel urlModel = new UrlModel(shorturl, url, expireTime, 0,
                    HelperService.getDateAndTimeFromEpoch(expireTime));
                    // if(map.size()==Integer.MAX_VALUE){
                    //   //offload the some entry nad put new one 
                    //   map.rem(Integer.MAX_VALUE-1);  
                    // }
            map.put(shorturl, urlModel);
           // save in cache and db
           saveInternally(shorturl,urlModel);
            log.info("Generated short code {} for {}", shorturl, url);
            return helperService.computeUrl(shorturl);
        } catch (Exception e) {
            log.error("Error generating short code for: {}", url, e);
        }
        return null;
    }

    protected String getBase64Hash(String url) {
        String result=null;
        result= Base64.getEncoder().encodeToString(((counterVar.updateAndGet(v->(v+1)%MOD)+url).getBytes()));
        if(result!=null && !result.isBlank()){
            return result.substring(0, 6);
        }
        return null;
    }

    protected void saveInternally(String shortUrl, UrlModel urlModel){
        // implement to save in redus cache and db in multithread environment
        urlRepository.save(urlModel);
        return;
    }
    public Map<String, UrlModel> getMap(){
        return new HashMap<>(this.map);
    }

    protected boolean existsById(String shorturl){
        if(urlRepository.existsById(shorturl)){
            log.debug("Short code collision detected in DB for {}", shorturl);
            // collision found in DB but not in the local cache (e.g. after a restart,
            // or a row created by another instance) - warm the cache so future
            // lookups for this code don't need another DB round trip
            urlRepository.findById(shorturl).ifPresent(existing -> map.put(shorturl, existing));
            return true;
        }
        return false;
    }

}
