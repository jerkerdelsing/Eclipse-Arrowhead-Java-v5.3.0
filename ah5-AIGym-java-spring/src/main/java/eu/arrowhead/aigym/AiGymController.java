package eu.arrowhead.aigym;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class AiGymController {
    public record SampleRequest(String model, Map<String,Double> features, Double target) {}
    public record PredictionRequest(String model, Map<String,Double> features, Integer neighbours) {}
    private final ModelStore models;
    private final AiGymRuntime runtime;
    AiGymController(ModelStore models, AiGymRuntime runtime) { this.models = models; this.runtime = runtime; }

    @GetMapping("/health") Map<String,Object> health() { return Map.of("status", "up", "system", "AIGym", "time", Instant.now()); }
    @GetMapping("/aigym/mgmt/general/status") Map<String,Object> managementStatus() { return runtime.status(); }
    @GetMapping("/aigym/status") Map<String,Object> status() { return runtime.status(); }

    @PostMapping("/aigym/samples") ResponseEntity<ModelStore.Sample> add(@RequestBody SampleRequest input, HttpServletRequest request) {
        if (input.target() == null) throw new IllegalArgumentException("target is required");
        ModelStore.Sample sample = models.add(input.model(), input.features(), input.target());
        runtime.publishObservation("trainingSample", input.target(), "1", Map.of("model", sample.model(), "consumer", consumer(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(sample);
    }

    @PostMapping("/aigym/predict") ModelStore.Prediction predict(@RequestBody PredictionRequest input, HttpServletRequest request) {
        ModelStore.Prediction prediction = models.predict(input.model(), input.features(), input.neighbours() == null ? 3 : input.neighbours());
        runtime.publishObservation("inference", prediction.value(), "1", Map.of("model", prediction.model(), "consumer", consumer(request), "neighbours", prediction.neighbours()));
        return prediction;
    }

    @GetMapping("/aigym/observations") List<Map<String,Object>> observations() { return runtime.observations(); }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<Map<String,Object>> badRequest(RuntimeException error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }
    private static String consumer(HttpServletRequest request) { return String.valueOf(request.getAttribute("arrowhead.consumer")); }
}
