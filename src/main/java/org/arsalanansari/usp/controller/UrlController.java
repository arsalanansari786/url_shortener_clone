package org.arsalanansari.usp.controller;

import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import org.arsalanansari.usp.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
public class UrlController {

    @Autowired 
    private UrlService urlService;

    @GetMapping("/{shorturl}")
    public ResponseEntity<?> redirect(@PathVariable String shorturl) {
        if(shorturl==null || shorturl.isBlank() || shorturl.length()>6){
        return   new ResponseEntity<>(HttpStatus.URI_TOO_LONG);  
        }
        String url=urlService.fetchLongUrl(shorturl);
        if(url!=null  && !url.isEmpty()){
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/short")
    public  ResponseEntity<?>  getshortUrl(@RequestBody String longurl) {
        if(longurl==null || longurl.isEmpty()){
            return new ResponseEntity<>("Invalid Url", HttpStatus.LENGTH_REQUIRED);
        }
        String url =urlService.getShortUrl(longurl);
        if(url!=null && !url.isBlank()){
            return ResponseEntity.ok().body("http://127.0.0.1:8991/"+url);
        }
        return new ResponseEntity<>("Cannot short the url", HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    
}
