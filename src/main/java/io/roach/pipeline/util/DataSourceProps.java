package io.roach.pipeline.util;

import org.springframework.util.Assert;

public class DataSourceProps {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DataSourceProps instance = new DataSourceProps();

        public Builder withUrl(String url) {
            instance.url = url;
            return this;
        }

        public Builder withUsername(String username) {
            instance.userName = username;
            return this;
        }

        public Builder withPassword(String password) {
            instance.password = password;
            return this;
        }

        public Builder withName(String name) {
            instance.name = name;
            return this;
        }

        public Builder withConcurrency(int concurrency) {
            instance.concurrency = concurrency;
            return this;
        }

        public DataSourceProps build() {
            Assert.hasLength(instance.name, "name is required");
            Assert.notNull(instance.url, "url is required");
            return instance;
        }
    }

    private String url;

    private String userName;

    private String password;

    private String name;

    private int concurrency;

    protected DataSourceProps() {
    }

    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public int getConcurrency() {
        return concurrency;
    }
}
