package org.arsalanansari.usp.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.arsalanansari.usp.Repository.UrlRepository;
import org.arsalanansari.usp.model.UrlModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UrlService {
    @Autowired
    private UrlRepository urlRepository;
    @Autowired
    private HelperService helperService;

    private long MOD = (long) 1e9 + 7;
    private long counterVar = 0;

    private ConcurrentHashMap<String, UrlModel> map = new ConcurrentHashMap<>();

    public String fetchLongUrl(String url){
        try {
            if(map.containsKey(url)){
                UrlModel urlModel=map.get(url);
                if(urlModel.getExpireEpoch()>System.currentTimeMillis()){
                    urlModel.setClickCount(urlModel.getClickCount()+1);
                    saveInternally(url, urlModel);
                    return urlModel.getLongUrl();
                }
                return HelperService.expiredLink;
            }
            return fetchFromDb(url);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    public String fetchFromDb(String shortUrl) {
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
                    return urlModel.getLongUrl();
                }
             return HelperService.expiredLink;
            }
        } catch (Exception e) {
            // todoo
        }
        return null;
    }

    public String getShortUrl(String url) {
        try {
            if(url==null || url.isBlank()){
                return null;
            }
            String shorturl = getBase64Hash(url);
            while (shorturl != null && !shorturl.isBlank() && map.containsKey(shorturl)) {
                shorturl = getBase64Hash(url);
            }
            if(shorturl == null || shorturl.isBlank()){
                return null;
            }
            // expire after 5 minutes
            long expireTime = 300 * 1000 + System.currentTimeMillis();
            UrlModel urlModel = new UrlModel(shorturl, url, expireTime, 0,
                    helperService.getDateAndTimeFromEpoch(expireTime));
                    // if(map.size()==Integer.MAX_VALUE){
                    //   //offload the some entry nad put new one 
                    //   map.rem(Integer.MAX_VALUE-1);  
                    // }
            map.put(shorturl, urlModel);
           // save in cache and db
           saveInternally(shorturl,urlModel);
            return shorturl;
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    public String getBase64Hash(String url) {
        counterVar=(counterVar+1)%MOD;
        String result=null;
        result= Base64.getEncoder().encodeToString(((counterVar+url).getBytes()));
        if(result!=null && !result.isBlank()){
            return result.substring(0, 6);
        }
        return null;
    }

    public void saveInternally(String shortUrl, UrlModel urlModel){
        // implement to save in redus cache and db in multithread environment
        urlRepository.save(urlModel);
        return;
    }
        public Map<String, UrlModel> getMap() {
        return new HashMap<>(this.map);
    }

}
