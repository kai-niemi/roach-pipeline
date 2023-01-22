package io.roach.pipeline.cloud;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class GCSBucketResource extends AbstractFileResolvingResource {
    public static final String CREDENTIALS = "CREDENTIALS";

    public static final String AUTH = "AUTH";

    public static final String SPECIFIED = "specified";

    public static final String IMPLICIT = "implicit";

    public static final String GCE_JSON_KEY = "GCE_JSON_KEY";

    public static final String GCE_PROJECT_ID = "GCE_PROJECT_ID";

    public static final String GOOGLE_APPLICATION_CREDENTIALS = "GOOGLE_APPLICATION_CREDENTIALS";

    private final URI resourceUri;

    private final Map<String, String> allParams;

    public GCSBucketResource(String resourceUri) {
        this(resourceUri, Collections.emptyMap());
    }

    public GCSBucketResource(String resourceUri, Map<String, String> allParams) {
        this.resourceUri = URI.create(UriUtils.decode(resourceUri, Charset.defaultCharset()));
        this.allParams = allParams;
    }

    @Override
    public String getFilename() {
        return StringUtils.getFilename(this.resourceUri.getPath());
    }

    @Override
    public String getDescription() {
        return "GCS bucket [" + this.resourceUri + "]";
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        String projectId = allParams.getOrDefault(GCE_PROJECT_ID, "");
        InputStream credentialsInputStream = getCredentialsInpuStream();
        StorageOptions options = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(GoogleCredentials.fromStream(credentialsInputStream)).build();
        Storage storage = options.getService();
        return getBlobInputStream(storage, resourceUri.toString());
    }

    private InputStream getCredentialsInpuStream() throws IOException {
        InputStream credentialsInputStream;
        String authMethod = allParams.getOrDefault(AUTH, SPECIFIED);
        if (SPECIFIED.equals(authMethod)) {
            String accessKey = allParams.getOrDefault(CREDENTIALS, "");
            if (StringUtils.hasLength(accessKey)) {
                InputStream stream = new ByteArrayInputStream(accessKey.getBytes(StandardCharsets.UTF_8));
                credentialsInputStream = Base64.getDecoder().wrap(stream);
            } else {
                accessKey = allParams.getOrDefault(GCE_JSON_KEY, "");
                if (!StringUtils.hasLength(accessKey)) {
                    throw new IllegalArgumentException("Missing required [CREDENTIALS] or [GCE_JSON_KEY]");
                }
                credentialsInputStream = new ByteArrayInputStream(accessKey.getBytes(StandardCharsets.UTF_8));
            }
        } else if (IMPLICIT.equals(authMethod)) {
            String accessKey = System.getenv(GOOGLE_APPLICATION_CREDENTIALS);
            if (!StringUtils.hasLength(accessKey)) {
                throw new IllegalArgumentException(
                        "Missing required env variable [" + GOOGLE_APPLICATION_CREDENTIALS + "]");
            }
            credentialsInputStream = Files.newInputStream(Paths.get(accessKey));
        } else {
            throw new IllegalArgumentException("Bad auth method [" + authMethod + "]");
        }
        return credentialsInputStream;
    }

    private InputStream getBlobInputStream(Storage storage, String resourceUri) {
        if (resourceUri.startsWith("gs://")) {
            resourceUri = resourceUri.substring(4);
        }
        String[] parts = resourceUri.split("/");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Bad resource URI: " + resourceUri);
        }
        String bucketName = parts[2];
        String objectName = String.join("/", Arrays.copyOfRange(parts, 3, parts.length));
        BlobId blobId = BlobId.of(bucketName, objectName);
        Blob blob = storage.get(blobId);
        if (blob == null || !blob.exists()) {
            throw new IllegalArgumentException("Blob object does not exist: " + resourceUri);
        }
        return Channels.newInputStream(blob.reader());
    }
}

