package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.flow.adapters.AdaptersToFlow;

public class MultiCreateFromPublisherTest {

    @Test
    public void testThatPublisherCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().publisher(null));
    }

    @Test
    public void testWithFailedPublisher() {
        AssertSubscriber<String> subscriber = Multi.createFrom().<String> publisher(
                AdaptersToFlow.publisher(Flowable.error(new IOException("boom")))).subscribe()
                .withSubscriber(AssertSubscriber.create());
        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithEmptyPublisher() {
        AssertSubscriber<String> subscriber = Multi.createFrom().<String> publisher(AdaptersToFlow.publisher(Flowable.empty()))
                .subscribe()
                .withSubscriber(AssertSubscriber.create());
        subscriber.assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithRegularPublisher() {
        AtomicLong requests = new AtomicLong();
        AtomicInteger count = new AtomicInteger();
        Flowable<Integer> flowable = Flowable.defer(() -> {
            count.incrementAndGet();
            return Flowable.just(1, 2, 3, 4);
        }).doOnRequest(requests::addAndGet);

        Multi<Integer> multi = Multi.createFrom().publisher(AdaptersToFlow.publisher(flowable));

        multi.subscribe().withSubscriber(AssertSubscriber.create()).assertHasNotReceivedAnyItem()
                .request(2)
                .assertItems(1, 2)
                .run(() -> assertThat(requests).hasValue(2))
                .request(1)
                .assertItems(1, 2, 3)
                .request(1)
                .assertItems(1, 2, 3, 4)
                .run(() -> assertThat(requests).hasValue(4))
                .assertCompleted();

        assertThat(count).hasValue(1);

        multi.subscribe().withSubscriber(AssertSubscriber.create()).assertHasNotReceivedAnyItem()
                .request(2)
                .assertItems(1, 2)
                .request(1)
                .assertItems(1, 2, 3)
                .request(1)
                .assertItems(1, 2, 3, 4)
                .run(() -> assertThat(requests).hasValue(8))
                .assertCompleted();

        assertThat(count).hasValue(2);

    }

    @Test
    public void testWithRegularSafePublisher() {
        AtomicLong requests = new AtomicLong();
        AtomicInteger count = new AtomicInteger();
        Flowable<Integer> flowable = Flowable.defer(() -> {
            count.incrementAndGet();
            return Flowable.just(1, 2, 3, 4);
        }).doOnRequest(requests::addAndGet);

        Multi<Integer> multi = Multi.createFrom().safePublisher(AdaptersToFlow.publisher(flowable));

        multi.subscribe().withSubscriber(AssertSubscriber.create()).assertHasNotReceivedAnyItem()
                .request(2)
                .assertItems(1, 2)
                .run(() -> assertThat(requests).hasValue(2))
                .request(1)
                .assertItems(1, 2, 3)
                .request(1)
                .assertItems(1, 2, 3, 4)
                .run(() -> assertThat(requests).hasValue(4))
                .assertCompleted();

        assertThat(count).hasValue(1);

        multi.subscribe().withSubscriber(AssertSubscriber.create()).assertHasNotReceivedAnyItem()
                .request(2)
                .assertItems(1, 2)
                .request(1)
                .assertItems(1, 2, 3)
                .request(1)
                .assertItems(1, 2, 3, 4)
                .run(() -> assertThat(requests).hasValue(8))
                .assertCompleted();

        assertThat(count).hasValue(2);

    }

    @Test
    public void testThatCancellingTheMultiCancelThePublisher() {
        AtomicBoolean cancellation = new AtomicBoolean();
        Flowable<Integer> flowable = Flowable.just(1, 2, 3, 4).doOnCancel(() -> cancellation.set(true));

        Multi<Integer> multi = Multi.createFrom().publisher(AdaptersToFlow.publisher(flowable));

        multi.subscribe().withSubscriber(AssertSubscriber.create()).assertHasNotReceivedAnyItem()
                .request(2)
                .assertItems(1, 2)
                .run(() -> assertThat(cancellation).isFalse())
                .request(1)
                .assertItems(1, 2, 3)
                .cancel()
                .request(1)
                .assertItems(1, 2, 3)
                .assertNotTerminated();

        assertThat(cancellation).isTrue();
    }
}
