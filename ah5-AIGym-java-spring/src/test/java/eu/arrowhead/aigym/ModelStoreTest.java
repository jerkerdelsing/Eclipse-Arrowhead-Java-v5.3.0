package eu.arrowhead.aigym;

import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelStoreTest {
    @Test void predictsFromNearestSamplesAndSeparatesModels() {
        var store = new ModelStore();
        store.add("temperature", Map.of("x", 0d), 10d);
        store.add("temperature", Map.of("x", 10d), 30d);
        store.add("other", Map.of("x", 10d), 99d);
        assertThat(store.predict("temperature", Map.of("x", 9d), 1).value()).isEqualTo(30d);
        assertThat(store.inventory()).containsEntry("temperature", 2).containsEntry("other", 1);
    }
}
