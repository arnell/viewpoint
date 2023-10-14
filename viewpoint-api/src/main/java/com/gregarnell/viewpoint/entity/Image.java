package com.gregarnell.viewpoint.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    String name;
    String path;
    String thumbPath;
    Integer width;
    Integer height;
    BigDecimal ratio;
    LocalDate taken;
    String checksumMd5;
    String checksumSha;
}
