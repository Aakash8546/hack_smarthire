package com.smarthire.config;

import java.time.Duration;

import com.smarthire.config.properties.MlApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfig {

    @Bean
    public RestTemplate mlRestTemplate(MlApiProperties mlApiProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(mlApiProperties.timeoutMillis()));
        requestFactory.setReadTimeout(Duration.ofMillis(mlApiProperties.timeoutMillis()));
        return new RestTemplate(requestFactory);
    }
}
