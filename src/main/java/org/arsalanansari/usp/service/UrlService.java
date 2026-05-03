package org.arsalanansari.usp.service;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.arsalanansari.usp.Repository.UrlRepository;
import org.arsalanansari.usp.model.UrlModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UrlService {
    @Autowired
    private UrlRepository urlRepository;

    public String fetchLongUrl(String shortUrl){
        String ss=new String(Base64.getDecoder().decode(shortUrl));
        long id =Long.parseLong(ss);
        Optional<UrlModel> longurl=urlRepository.findById(id);
        if(longurl.isPresent()){

        if(longurl!=null){
            long current=System.currentTimeMillis();
            if(longurl.get().getExpiry()>current){
                return longurl.get().getLongUrl();
            }  
        }
        }
        return null;
    }
    public String getShortUrl(String url){
        if(url.startsWith("http://") || url.startsWith("https://")){
            long now =System.currentTimeMillis();
            UrlModel urlModel=new UrlModel();
            urlModel.setLongUrl(url);
            urlModel.setTimeOfCreation(Instant.ofEpochMilli(now).toString());
            urlModel.setExpiry(now+60000);
            // urlRepository.deleteAll();
           UrlModel savedUrl= urlRepository.save(urlModel);
            return Base64.getEncoder().encodeToString(String.valueOf(savedUrl.getId()).getBytes());
        }
        return null ;
    }

}
