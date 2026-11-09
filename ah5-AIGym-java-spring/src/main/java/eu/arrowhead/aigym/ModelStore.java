package eu.arrowhead.aigym;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class ModelStore {
    public record Sample(String id, String model, Map<String, Double> features, double target, Instant createdAt) {}
    public record Prediction(String model, double value, int neighbours, int trainingSamples, Instant createdAt) {}
    private final Map<String, CopyOnWriteArrayList<Sample>> samples = new ConcurrentHashMap<>();

    public Sample add(String model, Map<String, Double> features, double target) {
        requireFeatures(features);
        var item = new Sample(UUID.randomUUID().toString(), normalizedModel(model), Map.copyOf(features), target, Instant.now());
        samples.computeIfAbsent(item.model(), ignored -> new CopyOnWriteArrayList<>()).add(item);
        return item;
    }

    public Prediction predict(String model, Map<String, Double> features, int requestedK) {
        requireFeatures(features);
        String name = normalizedModel(model);
        List<Sample> training = samples.getOrDefault(name, new CopyOnWriteArrayList<>());
        if (training.isEmpty()) throw new IllegalStateException("Model has no training samples");
        int k = Math.max(1, Math.min(requestedK, training.size()));
        double result = training.stream().sorted(Comparator.comparingDouble(s -> distance(s.features(), features)))
                .limit(k).mapToDouble(Sample::target).average().orElseThrow();
        return new Prediction(name, result, k, training.size(), Instant.now());
    }

    public Map<String, Integer> inventory() {
        var result = new TreeMap<String, Integer>();
        samples.forEach((name, values) -> result.put(name, values.size()));
        return result;
    }

    private static double distance(Map<String, Double> left, Map<String, Double> right) {
        Set<String> keys = new HashSet<>(left.keySet()); keys.addAll(right.keySet());
        return Math.sqrt(keys.stream().mapToDouble(key -> { double d = left.getOrDefault(key, 0d) - right.getOrDefault(key, 0d); return d * d; }).sum());
    }
    private static String normalizedModel(String model) { return model == null || model.isBlank() ? "default" : model.trim(); }
    private static void requireFeatures(Map<String, Double> features) {
        if (features == null || features.isEmpty() || features.values().stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("At least one numeric feature is required");
    }
}
