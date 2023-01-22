package io.roach.pipeline.cloud;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class S3BucketResource extends AbstractFileResolvingResource {
    public static final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";

    public static final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";

    public static final String AWS_DEFAULT_REGION = "AWS_DEFAULT_REGION";

    private final URI resourceUri;

    private final S3UriHelper resourceUriHelper;

    private final Map<String, String> allParams;

    public S3BucketResource(String resourceUri) {
        this(resourceUri, Collections.emptyMap());
    }

    public S3BucketResource(String resourceUri, Map<String, String> allParams) {
        this.resourceUri = URI.create(UriUtils.decode(resourceUri, Charset.defaultCharset()));
        this.resourceUriHelper = S3UriHelper.create(resourceUri);
        this.allParams = allParams;
    }

    @Override
    public String getFilename() {
        return StringUtils.getFilename(this.resourceUri.getPath());
    }

    @Override
    public String getDescription() {
        return "S3 bucket [" + this.resourceUri + "]";
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
    public InputStream getInputStream() {
        final S3Client s3Client;

        String accessKey = allParams.getOrDefault("AWS_ACCESS_KEY_ID", "");
        String secretAccessKey = allParams.getOrDefault("AWS_SECRET_ACCESS_KEY", "");
        String region = allParams.getOrDefault("AWS_DEFAULT_REGION", resourceUriHelper.getRegion());

        if (!StringUtils.hasLength(accessKey)) {
            s3Client = S3Client.builder()
                    .credentialsProvider(ProfileCredentialsProvider.create())
                    .build();
        } else {
            AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider
                    .create(AwsBasicCredentials.create(accessKey, secretAccessKey));

            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(resourceUriHelper.getBucket())
                .versionId(resourceUriHelper.getVersionId())
                .key(resourceUriHelper.getKey())
                .build();

        return s3Client.getObject(getObjectRequest);
    }
}
