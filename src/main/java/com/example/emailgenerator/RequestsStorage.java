package com.example.emailgenerator;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.example.emailgenerator.controllers.EmailGeneratorController.X_TRACE_ID_HEADER;

/**
 * Simple in-memory cache with maximum amount of 10_000 items
 */
@Component
@Scope("singleton")
public class RequestsStorage {

    private final Logger logger = LoggerFactory.getLogger(RequestsStorage.class);

    private final LoadingCache<String, RequestInfo> requestsCache;

    public RequestsStorage() {
        CacheLoader<String, RequestInfo> loader;
        loader = new CacheLoader<>() {
            @Override
            public RequestInfo load(String key) {
                return null;
            }
        };
        requestsCache = CacheBuilder.newBuilder().maximumSize(10_000).build(loader);
    }

    public RequestInfo loadRequest(String traceId) {
        try {
            return requestsCache.get(traceId);
        } catch (ExecutionException e) {
            return null;
        }
    }


    public List<RequestInfo> loadRequests() {
        return new ArrayList<>(requestsCache.asMap().values()).stream().sorted(Comparator.comparing(RequestInfo::getTimeStamp)).collect(Collectors.toList());
    }

    public String cacheRequest(HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        RequestInfo requestInfo = buildRequestInfo(traceId, request);
        requestsCache.put(traceId, requestInfo);
        logger.info("Request cached {}", requestInfo);
        return traceId;
    }

    private RequestInfo buildRequestInfo(String traceId, HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(X_TRACE_ID_HEADER, traceId);
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.add(headerName, request.getHeader(headerName));
            }
        }
        return new RequestInfo(LocalDateTime.now(), traceId, request.getMethod(), request.getRequestURI(), headers);
    }

}
