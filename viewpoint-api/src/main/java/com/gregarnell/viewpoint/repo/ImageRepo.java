package com.gregarnell.viewpoint.repo;

import com.gregarnell.viewpoint.entity.Image;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface ImageRepo extends CrudRepository<Image, String> {
    List<Image> findByTakenBetweenOrderByTakenDesc(LocalDate startDateTaken, LocalDate endDateTaken);
}
