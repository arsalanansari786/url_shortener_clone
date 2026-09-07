package org.arsalanansari.usp.singleInstanceCounter.Repository;

import org.arsalanansari.usp.singleInstanceCounter.model.UrlModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlRepository extends JpaRepository<UrlModel,String> {
    
}
