package io.roach.pipeline.cloud;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

public abstract class ResourceResolver {
    public static final String NODE_LOCAL_PATH = "nodeLocalPath";

    public static final String NODELOCAL_PREFIX = "nodelocal://";

    private ResourceResolver() {
    }

    public static boolean supportedPrefix(String url) {
        if (StringUtils.hasLength(url)) {
            return Stream.of("classpath:", "nodelocal:", "http:", "https:", "s3:", "gce:").anyMatch(url::startsWith);
        }
        return false;
    }

    public static Resource getResource(String url) {
        return getResource(url, Collections.emptyMap());
    }

    public static Resource getResource(String url, Map<String, String> allParams) {
        if (url.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
            String path = url.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length());
            return new ClassPathResource(path);
        }
        if (url.startsWith(NODELOCAL_PREFIX)) {
            Path nodeLocalPath = Paths.get(allParams.getOrDefault(NODE_LOCAL_PATH, "."));
            Path resourcePath = Paths.get(url.substring(ResourceResolver.NODELOCAL_PREFIX.length()));
            Path resourcePathAbs = nodeLocalPath.resolve(resourcePath).normalize().toAbsolutePath();
            if (!resourcePathAbs.startsWith(nodeLocalPath.toAbsolutePath())) {
                throw new IllegalArgumentException("Resource path [" + resourcePath
                        + "] must be relative to node local path ["
                        + nodeLocalPath + "]");
            }
            return new FileSystemResource(resourcePathAbs);
        }
        if (url.startsWith("http:") || url.startsWith("https:")) {
            try {
                return new UrlResource(url);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if (url.startsWith("s3:")) {
            return new S3BucketResource(url, allParams);
        }
        if (url.startsWith("gs:")) {
            return new GCSBucketResource(url, allParams);
        }

        throw new IllegalArgumentException("No resource type matching scheme: " + url);
    }

}
