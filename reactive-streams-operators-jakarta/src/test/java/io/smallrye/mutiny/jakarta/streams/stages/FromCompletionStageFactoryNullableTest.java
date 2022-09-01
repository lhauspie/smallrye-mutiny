package io.smallrye.mutiny.jakarta.streams.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.junit.jupiter.api.Test;

/**
 * Checks the behavior of the {@link Stage.FromCompletionStageNullable} class.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FromCompletionStageFactoryNullableTest extends StageTestBase {

    private final FromCompletionStageNullableFactory factory = new FromCompletionStageNullableFactory();

    @Test
    public void createFromAlreadyCompletedFuture() {
        CompletionStage<String> cs = CompletableFuture.completedFuture("hello");
        List<String> list = ReactiveStreams.fromCompletionStageNullable(cs).toList().run().toCompletableFuture().join();
        assertThat(list).containsExactly("hello");
    }

    @Test
    public void createFromAlreadyFailedFuture() {
        CompletionStage<String> cs = new CompletableFuture<>();
        ((CompletableFuture<String>) cs).completeExceptionally(new Exception("Expected"));

        try {
            ReactiveStreams.fromCompletionStageNullable(cs).findFirst().run().toCompletableFuture().join();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("Expected");
        }
    }

    @Test
    public void createFromFutureGoingToBeCompleted() {
        CompletableFuture<String> cf = new CompletableFuture<>();
        CompletionStage<Optional<String>> stage = ReactiveStreams.fromCompletionStageNullable(cf).findFirst().run();

        AtomicBoolean done = new AtomicBoolean();
        stage.whenComplete((res, err) -> {
            assertThat(err).isNull();
            assertThat(res).contains("Hello");
            done.set(true);
        });

        new Thread(() -> cf.complete("Hello")).start();
        await().untilAtomic(done, is(true));
    }

    @Test
    public void createFromFutureGoingToBeFailed() {
        CompletableFuture<String> cf = new CompletableFuture<>();
        CompletionStage<Optional<String>> stage = ReactiveStreams.fromCompletionStageNullable(cf).findFirst().run();

        AtomicBoolean done = new AtomicBoolean();
        stage.whenComplete((res, err) -> {
            assertThat(err).isNotNull().hasMessageContaining("Expected");
            assertThat(res).isNull();
            done.set(true);
        });

        new Thread(() -> cf.completeExceptionally(new Exception("Expected"))).start();
        await().untilAtomic(done, is(true));
    }

    @Test
    public void createFromFutureCompletedWithNull() {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        CompletionStage<Optional<Void>> stage = ReactiveStreams.fromCompletionStageNullable(cf).findFirst().run();

        AtomicBoolean done = new AtomicBoolean();
        stage.whenComplete((res, err) -> {
            assertThat(err).isNull();
            assertThat(res).isEmpty();
            done.set(true);
        });

        new Thread(() -> cf.complete(null)).start();
        await().untilAtomic(done, is(true));
    }

    @Test
    public void createWithoutStage() {
        assertThrows(NullPointerException.class, () -> factory.create(null, null));
    }

    @Test
    public void createWithNullResult() {
        assertThrows(NullPointerException.class, () -> factory.create(null, () -> null).get());
    }

}
