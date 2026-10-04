package com.aishield.fraud.controller;

import com.aishield.fraud.dto.ApiResponse;
import com.aishield.fraud.dto.GeminiChatDtos;
import com.aishield.fraud.service.GeminiCopilotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gemini")
@Tag(name = "Gemini Copilot", description = "Endpoints for real-time Gemini AI chat and assistant interactions")
public class GeminiCopilotController {

    private final GeminiCopilotService geminiCopilotService;

    public GeminiCopilotController(GeminiCopilotService geminiCopilotService) {
        this.geminiCopilotService = geminiCopilotService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Send a question to Gemini AI Copilot", description = "Processes general questions or project-specific fraud inquiries via Gemini AI")
    public ResponseEntity<ApiResponse<GeminiChatDtos.ChatResponse>> chat(
            @RequestBody GeminiChatDtos.ChatRequest request,
            @RequestHeader(value = "X-Gemini-Api-Key", required = false) String headerApiKey
    ) {
        String effectiveKey = request.getApiKey();
        if (effectiveKey == null || effectiveKey.trim().isEmpty()) {
            effectiveKey = headerApiKey;
        }

        GeminiChatDtos.ChatRequest resolvedRequest = new GeminiChatDtos.ChatRequest(
                request.getMessage(),
                request.getHistory(),
                effectiveKey
        );

        GeminiChatDtos.ChatResponse response = geminiCopilotService.processChat(resolvedRequest);
        return ResponseEntity.ok(ApiResponse.success("Gemini response retrieved successfully", response));
    }
}
