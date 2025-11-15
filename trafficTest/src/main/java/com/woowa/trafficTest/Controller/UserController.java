package com.woowa.trafficTest.Controller;

import com.woowa.trafficTest.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/api/process")
    public ResponseEntity<Map<String, Object>> processUsers(
            @RequestParam(defaultValue = "100") int count
    ) {
        int processedCount;

        processedCount = userService.processUsers(count);

        Map<String, Object> body = Map.of(
                "status", "OK",
                "processedCount", processedCount
        );

        return ResponseEntity.ok(body);
    }
}
