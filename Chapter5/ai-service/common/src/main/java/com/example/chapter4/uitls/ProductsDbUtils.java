package com.example.chapter4.uitls;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;

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
}
