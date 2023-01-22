package io.roach.pipeline.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Profile;

public abstract class ApplicationProfiles {
    public static List<String> all() {
        return Arrays.asList("h2", "psql", "crdb", "offline", "verbose", "dev");
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Profile("h2")
    public @interface H2 {
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Profile("psql")
    public @interface PostgreSQL {
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Profile("crdb")
    public @interface CockroachDB {
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Profile("!offline")
    public @interface Online {

    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Profile("offline")
    public @interface Offline {

    }

    private ApplicationProfiles() {
    }
}
