package com.taskmind.backend.ai;

import com.taskmind.ai.contracts.audit.AiRunSummary;
import com.taskmind.ai.contracts.capability.CapabilitiesResponse;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import jakarta.validation.Valid;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/v1/nova")
public class NovaFacadeController {
    private final NovaClient novaClient;

    public NovaFacadeController(NovaClient novaClient) {
        this.novaClient = novaClient;
    }

    @PostMapping("/chat")
    ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return novaClient.chat(request);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<StreamingResponseBody> chatStream(@Valid @RequestBody ChatRequest request) throws IOException {
        StreamingResponseBody responseBody = outputStream -> novaClient.chatStream(request, new FlushingOutputStream(outputStream));
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(responseBody);
    }

    private static final class FlushingOutputStream extends FilterOutputStream {
        private FlushingOutputStream(java.io.OutputStream outputStream) {
            super(outputStream);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            out.flush();
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            out.write(bytes, offset, length);
            out.flush();
        }
    }

    @GetMapping("/capabilities")
    CapabilitiesResponse capabilities() {
        return novaClient.capabilities();
    }

    @GetMapping("/runs/{runId}")
    AiRunSummary run(@PathVariable UUID runId) {
        return novaClient.run(runId);
    }

    @ExceptionHandler(NovaClientException.class)
    ResponseEntity<ProblemDetail> novaFailure(NovaClientException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                exception.statusCode(), exception.getMessage());
        problem.setTitle(title(exception));
        problem.setType(URI.create("https://taskmind.local/problems/nova-facade"));
        problem.setProperty("code", exception.errorCode());
        return ResponseEntity.status(exception.statusCode()).body(problem);
    }

    private String title(NovaClientException exception) {
        if (exception.statusCode().value() == 400) {
            return "Invalid AI request";
        }
        if (exception.statusCode().value() == 404) {
            return "AI resource not found";
        }
        return "AI service unavailable";
    }
}
