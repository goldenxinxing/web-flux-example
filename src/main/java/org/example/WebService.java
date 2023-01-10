package org.example;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class WebService {

    public Flux<String> getStrList(String name) {
        return Flux.create(stringFluxSink -> {
            stringFluxSink.onDispose(() -> System.out.println("disposed"));
            stringFluxSink.onCancel(() -> System.out.println("canceled"));
            if (name.equals("ok")) {
                pubsub(stringFluxSink::next, (v) -> stringFluxSink.complete());
                // stringFluxSink.complete();
            } else
                stringFluxSink.error(new RuntimeException("error"));
        });
    }
    private void pubsub(Consumer<String> consumer, Consumer<Void> f) {
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 100; i++) {
                System.out.printf("group:%s, thread:%s, index:%s%n", Thread.currentThread().getThreadGroup(), Thread.currentThread(), i);
                consumer.accept(String.format("ok la %s... ", i));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            f.accept(null);
        });
    }

    /**
     * 此写法不太对，混合了flux机制和async开启异步线程池的方式，如果使用了main，那就是这真的sync了
     * @param name
     * @return
     */
    @Async
    public CompletableFuture<Flux<String>> getStrListAsync(String name) {
        return CompletableFuture.completedFuture(Flux.create(stringFluxSink -> {
            stringFluxSink.onDispose(() -> System.out.println("disposed"));
            stringFluxSink.onCancel(() -> System.out.println("canceled"));
            if (name.equals("ok")) {
                for (int i = 0; i < 100; i++) {
                    System.out.printf("group:%s, thread:%s, index:%s%n", Thread.currentThread().getThreadGroup(), Thread.currentThread(), i);
                    stringFluxSink.next(String.format("ok la %s... ", i));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        stringFluxSink.error(new RuntimeException(e));
                    }
                }
                stringFluxSink.complete();
            } else
                stringFluxSink.error(new RuntimeException("error"));
        }));
    }


    public Flux<WebController.ResponseMessage<String>> getResList(String name) {
        return Flux.create(stringFluxSink -> {
            stringFluxSink.onDispose(() -> System.out.println("disposed"));
            stringFluxSink.onCancel(() -> System.out.println("canceled"));
            if (name.equals("ok")) {
                for (int i = 0; i < 100; i++) {
                    System.out.printf("group:%s, thread:%s, index:%s%n", Thread.currentThread().getThreadGroup(), Thread.currentThread(), i);
                    stringFluxSink.next(WebController.ResponseMessage.<String>builder().code("success").data(String.format("ok la %s... ", i)).build());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        stringFluxSink.error(new RuntimeException(e));
                    }
                }
                stringFluxSink.complete();
            } else
                stringFluxSink.error(new RuntimeException("error"));
        });
    }
}
