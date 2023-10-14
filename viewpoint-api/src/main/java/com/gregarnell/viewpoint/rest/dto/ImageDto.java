package com.gregarnell.viewpoint.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ImageDto {
    private Long id;
    private String name;
    private String thumbPath;
    private BigDecimal ratio;
}
