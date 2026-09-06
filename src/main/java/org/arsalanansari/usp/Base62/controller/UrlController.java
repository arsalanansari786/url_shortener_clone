package org.arsalanansari.usp.Base62.controller;

import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;

import org.arsalanansari.usp.Base62.service.HelperService;
import org.arsalanansari.usp.Base62.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/")
public class UrlController {

    @Autowired
    private UrlService urlService;

     @Autowired
    private HelperService helperService;

    @GetMapping("/{shorturl}")
    public ResponseEntity<?> redirect(@PathVariable String shorturl) {
        if(shorturl==null || shorturl.isBlank() || shorturl.length()>6){
        log.warn("Rejected redirect request for invalid short code: {}", shorturl);
        return  new ResponseEntity<>(HttpStatus.URI_TOO_LONG);
        }
        String url=urlService.fetchLongUrl(shorturl);
        if(url!=null  && !url.isEmpty()){
            log.info("Redirecting short code {} to {}", shorturl, url);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
        }else{
            log.warn("No long URL found for short code: {}", shorturl);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/short")
    public  ResponseEntity<?>  getshortUrl(@RequestBody String longurl) {
        if(longurl==null || longurl.isBlank() || !isValidUrl(longurl)){
            log.warn("Rejected short-url request for invalid input: {}", longurl);
            return new ResponseEntity<>("Invalid Url", HttpStatus.BAD_REQUEST);
        }
        String url =urlService.getShortUrl(longurl);
        if(url==null || url.isBlank()){
            log.error("Failed to generate a short url for: {}", longurl);
            return new ResponseEntity<>("Cannot short the url", HttpStatus.BAD_REQUEST);
        }else if(url.equalsIgnoreCase(helperService.computeTypeUrl(HelperService.exhaust))){
              log.warn("Short code generation exhausted retry limit for: {}", longurl);
              return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
        }else{
            log.info("Created short url {} for {}", url, longurl);
            return ResponseEntity.status(HttpStatus.CREATED).body(url);
        }
    }
    @GetMapping("/view")
    public ResponseEntity<?> view() {
        if(urlService.getMap().isEmpty()){
            return new ResponseEntity<>("No url is present to show",HttpStatus.OK);
        }
        return new ResponseEntity<>(urlService.getMap(), HttpStatus.OK);
    }
     @GetMapping("/expired")
    public ResponseEntity<?> expired() {
            return new ResponseEntity<>("Link is expired.",HttpStatus.FORBIDDEN);
        }
     @GetMapping("/exhaustlimit")
    public ResponseEntity<?> exhaustLimit() {
            return new ResponseEntity<>("Limit is exhausted to create new unique short URL.",HttpStatus.FORBIDDEN);
        }

    private boolean isValidUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (URISyntaxException e) {
            return false;
        }
    }

}
