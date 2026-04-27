package com.shu.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;


@Configuration
//@Profile("prod")
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(accessKey, secretKey);

        System.out.println("cloud.aws.region.static = " + region);
        System.out.println("cloud.aws.credentials.access-key startsWith = " + accessKey.substring(0, 4));

        return S3Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .endpointOverride(
                        URI.create("https://s3.ap-northeast-2.amazonaws.com")
                )
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(accessKey, secretKey);

        return S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .build();
    }
}