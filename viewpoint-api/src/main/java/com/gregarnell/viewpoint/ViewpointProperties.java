package com.gregarnell.viewpoint;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("viewpoint")
public class ViewpointProperties {
    private String imagesRoot;
}
