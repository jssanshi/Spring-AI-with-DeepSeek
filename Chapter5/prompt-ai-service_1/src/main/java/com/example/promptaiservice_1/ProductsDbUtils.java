package com.example.promptaiservice_1;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import java.io.IOException;
import java.nio.charset.Charset;

@Configuration
public class ProductsDbUtils {
    public String getProducts() {
        try {
            var filename = "classpath:/productDB/products.txt";

            return new DefaultResourceLoader()
                    .getResource(filename)
                    .getContentAsString(Charset.defaultCharset());
        } catch (IOException e) {
            return "";
        }
    }

    @Bean
    RestClientCustomizer logbookCustomizer(
            LogbookClientHttpRequestInterceptor interceptor) {
        return restClient -> restClient.requestInterceptor(interceptor);
    }
}
