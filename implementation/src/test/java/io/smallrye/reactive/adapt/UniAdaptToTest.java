package io.smallrye.reactive.adapt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.Test;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.converters.*;

public class UniAdaptToTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingCompletionStages() {
        Uni<Integer> valued = Uni.createFrom().item(1);
        Uni<Void> empty = Uni.createFrom().item(null);
        Uni<Void> failure = Uni.createFrom().failure(new Exception("boom"));

        CompletionStage<Integer> stage1 = valued.adapt().toCompletionStage();
        CompletionStage<Void> stage2 = empty.adapt().with(new ToCompletionStage<>());
        CompletionStage<Void> stage3 = failure.adapt().toCompletionStage();

        assertThat(stage1).isCompletedWithValue(1);
        assertThat(stage2).isCompletedWithValue(null);
        assertThat(stage3).isCompletedExceptionally();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingCompletableFutures() {
        Uni<Integer> valued = Uni.createFrom().item(1);
        Uni<Void> empty = Uni.createFrom().item(null);
        Uni<Void> failure = Uni.createFrom().failure(new Exception("boom"));

        CompletableFuture<Integer> stage1 = valued.adapt().toCompletableFuture();
        CompletableFuture<Void> stage2 = empty.adapt().toCompletableFuture();
        CompletableFuture<Void> stage3 = failure.adapt().with(new ToCompletableFuture<>());

        assertThat(stage1).isCompletedWithValue(1);
        assertThat(stage2).isCompletedWithValue(null);
        assertThat(stage3).isCompletedExceptionally();
    }
}
