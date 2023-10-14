package com.gregarnell.viewpoint.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@AllArgsConstructor
@Data
public class SegmentDto {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer count = 0;
}
