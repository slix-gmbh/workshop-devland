package org.example.rideshare;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    public static @NotNull String env(@NotNull String key, @NotNull String fallback) {
        final String env = System.getenv(key);
        if (env == null) {
            return fallback;
        }
        return env;
    }
}
