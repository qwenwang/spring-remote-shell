package com.qwen.spring.shell;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ShellApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder().sources(ShellApplication.class).bannerMode(Banner.Mode.OFF).run(args);
    }
}
