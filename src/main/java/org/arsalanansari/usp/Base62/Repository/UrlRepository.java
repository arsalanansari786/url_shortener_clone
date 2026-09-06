package org.arsalanansari.usp.Base62.Repository;

import org.arsalanansari.usp.Base62.model.UrlModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlRepository extends JpaRepository<UrlModel,String> {
    
}
