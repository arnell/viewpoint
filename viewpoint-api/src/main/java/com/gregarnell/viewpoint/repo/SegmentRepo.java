package com.gregarnell.viewpoint.repo;

import com.gregarnell.viewpoint.entity.Segment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SegmentRepo extends CrudRepository<Segment, String> {
    List<Segment> findByOrderByStartDateDesc();
}
