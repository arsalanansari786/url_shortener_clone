package org.arsalanansari.usp.Repository;

import org.arsalanansari.usp.model.UrlModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlRepository extends JpaRepository<UrlModel,Long> {
    
   public UrlModel findByShortUrl(String shortUrl);
}
