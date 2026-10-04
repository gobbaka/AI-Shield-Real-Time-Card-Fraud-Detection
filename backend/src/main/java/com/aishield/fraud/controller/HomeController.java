package com.aishield.fraud.controller;

import com.aishield.fraud.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class HomeController {

    /**
     * Redirects root URL requests (http://localhost:8080) directly to Swagger OpenAPI UI
     * to ensure immediate interactive testing and eliminate HTTP 403 confusion.
     */
    @GetMapping("/")
    public RedirectView redirectToSwagger() {
        return new RedirectView("/swagger-ui/index.html");
    }

    /**
     * Standard health check probe for orchestration and monitoring.
     */
    @GetMapping("/api/v1/health")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "AI Shield Fraud Detection Engine",
                "version", "1.0.0",
                "timestamp", LocalDateTime.now().toString(),
                "engineStatus", "ACTIVE",
                "rulesEngine", "READY",
                "mlScorer", "READY"
        );
        return ResponseEntity.ok(ApiResponse.success("AI Shield Backend Service is operating normally", health));
    }
}
