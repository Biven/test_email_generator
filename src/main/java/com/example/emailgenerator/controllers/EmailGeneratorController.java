package com.example.emailgenerator.controllers;

import com.example.emailgenerator.RequestInfo;
import com.example.emailgenerator.RequestsStorage;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/generator")
public class EmailGeneratorController {

    private final Logger logger = LoggerFactory.getLogger(EmailGeneratorController.class);

    public static final String X_TRACE_ID_HEADER = "X-Trace-id";

    private final CopyOnWriteArrayList<String> names = new CopyOnWriteArrayList<>();

    private final RequestsStorage requestsStorage;

    @Autowired
    public EmailGeneratorController(RequestsStorage requestsStorage) {
        this.requestsStorage = requestsStorage;
    }

    @PostMapping("/upload")
    @ApiOperation(value = "Upload file which contains names. Excepted that each line has new name")
    public ResponseEntity<String> uploadFile(@RequestPart MultipartFile file, HttpServletRequest request) {
        String traceId = requestsStorage.cacheRequest(request);

        if (file != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                while (reader.ready()) {
                    String line = reader.readLine();
                    names.add(line);
                }
            } catch (IOException ex) {
                logger.error("Failure reading file", ex);
                return ResponseEntity
                        .badRequest()
                        .header(X_TRACE_ID_HEADER, traceId)
                        .body("Failure reading file. Exception: " + ex.getMessage());
            }
        }

        return ResponseEntity
                .ok()
                .header(X_TRACE_ID_HEADER, traceId)
                .body("Names imported");
    }

    @GetMapping("/emails")
    @ApiOperation(value = "Return list of distinct names concatenated by @revevol.it and ordered")
    public ResponseEntity<List<String>> getEmails(HttpServletRequest request) {
        String traceId = requestsStorage.cacheRequest(request);

        List<String> emails = names.stream()
                .distinct()
                .sorted()
                .map(name -> name + "@revevol.it")
                .collect(Collectors.toList());

        return ResponseEntity
                .ok()
                .header(X_TRACE_ID_HEADER, traceId)
                .body(emails);
    }


    @GetMapping("/requestsHistory")
    @ApiOperation(value = "Return previous calls history")
    public ResponseEntity<List<RequestInfo>> getRequestsHistory(
            @ApiParam(value = "Optional parameter for searching specific request by traceId") @RequestParam(required = false) String searchTraceId,
            HttpServletRequest request) {

        String traceId = requestsStorage.cacheRequest(request);
        if (searchTraceId != null) {
            return ResponseEntity
                    .ok()
                    .header(X_TRACE_ID_HEADER, traceId)
                    .body(Collections.singletonList(requestsStorage.loadRequest(searchTraceId)));
        }
        return ResponseEntity
                .ok()
                .header(X_TRACE_ID_HEADER, traceId)
                .body(requestsStorage.loadRequests());
    }

    @DeleteMapping("/clearNames")
    @ApiOperation(value = "Remove all the names currently stored in memory")
    public ResponseEntity<String> clearNames(HttpServletRequest request) {
        String traceId = requestsStorage.cacheRequest(request);

        names.clear();

        return ResponseEntity
                .ok()
                .header(X_TRACE_ID_HEADER, traceId)
                .body("Names removed");
    }
}
