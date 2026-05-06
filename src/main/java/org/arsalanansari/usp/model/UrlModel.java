package org.arsalanansari.usp.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UrlModel {

    @Id
    private String shortUrl;
    @Column(nullable =false)
    private String longUrl;
    private long expireEpoch;
    private long clickCount;
    private String expireTime;
}
