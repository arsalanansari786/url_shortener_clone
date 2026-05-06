package org.arsalanansari.usp.service;

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

    public String fetchLongUrl(String shortUrl) {
        try {
            Optional<UrlModel> urlModeOptional = urlRepository.findById(shortUrl);
            if (urlModeOptional.isPresent()) {
                UrlModel urlModel = urlModeOptional.get();
                long current = System.currentTimeMillis();
                if (urlModel.getExpireEpoch() > current) {
                    urlModel.setClickCount(urlModel.getClickCount() + 1);
                    urlRepository.save(urlModel);
                    return urlModel.getLongUrl();
                }
            }
        } catch (Exception e) {
            // todoo
        }
        return null;
    }

    public String getShortUrl(String url) {
        try {
            String shorturl = getBase64Hash(url).substring(0, 6);
            while (urlRepository.existsById(shorturl)) {
                shorturl = getBase64Hash(url);
                if (shorturl != null && !shorturl.isBlank()) {
                    shorturl = shorturl.substring(0, 6);
                }
            }
            // expire after 5 minutes
            long expireTime = 300 * 1000 + System.currentTimeMillis();
            UrlModel urlModel = new UrlModel(shorturl, url, expireTime, 0, "");
            urlRepository.save(urlModel);
            return shorturl;
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    public String getBase64Hash(String url) {
        return Base64.getEncoder().encodeToString(url.getBytes());
    }
}
