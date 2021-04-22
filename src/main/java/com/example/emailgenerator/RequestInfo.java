package com.example.emailgenerator;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RequestInfo {

    private LocalDateTime timeStamp;
    private String traceId;
    private String method;
    private String requestUri;
    private MultiValueMap<String, String> headers;

}
