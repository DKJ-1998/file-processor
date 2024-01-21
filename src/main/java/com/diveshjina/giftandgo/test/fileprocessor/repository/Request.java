package com.diveshjina.giftandgo.test.fileprocessor.repository;

import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "requests")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Request {

    @Id
    @NonNull
    private UUID requestId;

    @NonNull
    private String requestUri;

    @NonNull
    private Timestamp requestTimestamp;

    private int httpResponseCode;

    @NonNull
    private String requestIpAddress;

    @NonNull
    private String requestCountryCode;

    @NonNull
    private String requestIpProvider;

    private long timeLapsedInMillis;
}
