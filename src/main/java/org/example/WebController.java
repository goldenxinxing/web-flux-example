package org.example;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("web")
public class WebController {

    @Autowired
    private WebService service;

    @GetMapping("t")
    public String t() {
        System.out.printf("group:%s, thread:%s%n", Thread.currentThread().getThreadGroup(), Thread.currentThread());
        return "success";
    }

    @GetMapping("test")
    public Mono<ResponseEntity<String>> test(String name) {
        return Mono.<ResponseEntity<String>>create(responseEntityMonoSink -> {
            responseEntityMonoSink.onDispose(() -> System.out.println("disposed"));
            responseEntityMonoSink.onCancel(() -> System.out.println("canceled"));
            if (name.equals("ok"))
                responseEntityMonoSink.success(ResponseEntity.ok("ok la"));
            else
                responseEntityMonoSink.error(new RuntimeException("error"));
        });
    }
    // 1.how to wrap with unify response struct; swagger show??
    // 2.only replace web container to netty
    @GetMapping(value = "test2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> test2(String name) {
        return service.getStrList(name);
    }

    /**
     * ResponseEntity<Mono<T>> or ResponseEntity<Flux<T>> make the response status and headers known immediately while the body is provided asynchronously at a later point.
     * Use Mono if the body consists of 0..1 values or Flux if it can produce multiple values.
     * @param name
     * @return
     */
    @GetMapping("test3")
    public ResponseEntity<Mono<String>> test3(String name) {
        return ResponseEntity.ok(
                Mono.just(String.format("ok la %s... ", "single")));
    }

    @GetMapping(value = "test32", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> test32(String name) {
        // netty下，这个是流式的输出到client，但mock32不是，因为它需要json化，那就得等（解决方案就是content-type = ndjson/stream-json）
        // 可能需要event-stream，因为stream-json只能是object，待验证(是的)
        return ResponseEntity.ok(
                service.getStrList(name)
        );
    }

    @Async
    @GetMapping(value = "test321", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public CompletableFuture<ResponseEntity<Flux<ResponseMessage<String>>>> test321(String name) {
        // servlet async下可以，也就是只要内部想要流式的数据被flux包裹即可，不用像mock32那样
        return CompletableFuture.completedFuture(ResponseEntity.ok(
                service.getResList(name)
        ));
    }

    @GetMapping(value = "test322", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public ResponseEntity<Flux<ResponseMessage<String>>> test322(String name) {
        // servlet async下可以，也就是只要内部想要流式的数据被flux包裹即可，不用像mock32那样
        return ResponseEntity.ok(
                service.getResList(name)
        );
    }

    @Async
    @GetMapping(value = "test33", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public CompletableFuture<ResponseEntity<List<String>>> test33(String name) {
        var list = new ArrayList<String>();
        for (int i = 0; i < 100; i++) {
            list.add(String.format("ok la %s... ", i));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return CompletableFuture.completedFuture(ResponseEntity.ok(list));
    }

    /**
     * Mono<ResponseEntity<T>>
     * @param name
     * @return
     */
    @GetMapping("test4")
    public Mono<ResponseEntity<String>> test4(String name) {
        return Mono.create(responseEntityMonoSink -> {
                    responseEntityMonoSink.onDispose(() -> System.out.println("disposed"));
                    responseEntityMonoSink.onCancel(() -> System.out.println("canceled"));
                    if (name.equals("ok")) {
                        responseEntityMonoSink.success(ResponseEntity.ok(String.format("ok la %s... ", "mono a single entity")));
                    } else
                        responseEntityMonoSink.error(new RuntimeException("error"));
                });
    }

    /**
     * Mono<ResponseEntity<Mono<T>>> or Mono<ResponseEntity<Flux<T>>> are yet another possible, albeit less common alternative.
     * They provide the response status and headers asynchronously first and then the response body, also asynchronously, second.
     * @param name
     * @return
     */
    @GetMapping("test5")
    public Mono<ResponseEntity<Mono<String>>> test5(String name) {
        return Mono.just(ResponseEntity.ok(
                    Mono.just(String.format("ok la %s... ", "mono a single entity's mono str:Mono<ResponseEntity<Mono<T>>>")))
        );
    }

    @GetMapping("test52")
    public Mono<ResponseEntity<Flux<String>>> test52(String name) {
        return Mono.create(responseEntityMonoSink -> {
                    responseEntityMonoSink.onDispose(() -> System.out.println("disposed"));
                    responseEntityMonoSink.onCancel(() -> System.out.println("canceled"));
                    if (name.equals("ok")) {
                            responseEntityMonoSink.success(
                                    ResponseEntity.ok(service.getStrList(name))
                            );
                    } else {
                        responseEntityMonoSink.error(new RuntimeException("error"));
                    }
                });
    }

    @GetMapping("mock")
    public Mono<ResponseEntity<ResponseMessage<String>>> mock(String name) {
        return Mono.create(responseEntityMonoSink -> {
            responseEntityMonoSink.onDispose(() -> System.out.println("disposed"));
            responseEntityMonoSink.onCancel(() -> System.out.println("canceled"));
            if (name.equals("ok"))
                responseEntityMonoSink.success(
                        ResponseEntity.ok(ResponseMessage.<String>builder()
                                .code("ok")
                                .data("hi")
                                .build()
                        )
                );
            else
                responseEntityMonoSink.error(new RuntimeException("error"));
        });
    }


    /**
     * ResponseEntity<Mono<T>> or ResponseEntity<Flux<T>> make the response status and headers known immediately while the body is provided asynchronously at a later point.
     * Use Mono if the body consists of 0..1 values or Flux if it can produce multiple values.
     * @param name
     * @return
     */
    @GetMapping("mock3")
    public ResponseEntity<Mono<ResponseMessage<String>>> mock3(String name) {
        // TODO ResponseMessage<Mono>???????
        return ResponseEntity.ok(
                Mono.create(stringFluxSink -> {
                    stringFluxSink.onDispose(() -> System.out.println("disposed"));
                    stringFluxSink.onCancel(() -> System.out.println("canceled"));
                    if (name.equals("ok")) {
                        stringFluxSink.success(
                                ResponseMessage.<String>builder()
                                        .code("success")
                                        .data(String.format("ok la %s... ", "single"))
                                        .build()
                        );
                    } else
                        stringFluxSink.error(new RuntimeException("error"));
                })
        );
    }

    @Async
    @GetMapping(value = "mock32", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public CompletableFuture<ResponseEntity<Flux<ResponseMessage<String>>>> mock32(String name) {
        // ResponseEntity<ResponseMessage<Flux<String>>> 不成
        // TODO ResponseEntity<Flux<ResponseMessage<String>>> 不符合预期，且client未按流式接收数据，仍是结束后才开始接收
        return CompletableFuture.completedFuture(ResponseEntity.ok(
                service.getResList(name)
        ));
    }

    /**
     * Mono<ResponseEntity<T>>
     * @param name
     * @return
     */
    @GetMapping("mock4")
    public Mono<ResponseEntity<ResponseMessage<String>>> mock4(String name) {
        // 没什么意义
        return Mono.create(responseEntityMonoSink -> {
            responseEntityMonoSink.onDispose(() -> System.out.println("disposed"));
            responseEntityMonoSink.onCancel(() -> System.out.println("canceled"));
            if (name.equals("ok")) {
                responseEntityMonoSink.success(ResponseEntity.ok(
                        ResponseMessage.<String>builder()
                                .code("success")
                                .data(String.format("ok la %s... ", "mono a single entity"))
                                .build()
                ));
            } else
                responseEntityMonoSink.error(new RuntimeException("error"));
        });
    }

    /**
     * Mono<ResponseEntity<Mono<T>>> or Mono<ResponseEntity<Flux<T>>> are yet another possible, albeit less common alternative.
     * They provide the response status and headers asynchronously first and then the response body, also asynchronously, second.
     * @param name
     * @return
     */
    @GetMapping("mock5")
    public Mono<ResponseEntity<Mono<ResponseMessage<String>>>> mock5(String name) {
        // 没什么意义
        return Mono.create(responseEntityMonoSink -> {
            responseEntityMonoSink.onDispose(() -> System.out.println("disposed"));
            responseEntityMonoSink.onCancel(() -> System.out.println("canceled"));
            if (name.equals("ok")) {
                responseEntityMonoSink.success(
                        ResponseEntity.ok(Mono.create(s -> s.success(
                                ResponseMessage.<String>builder()
                                        .code("success")
                                        .data(String.format("ok la %s... ", "mono a single entity's mono str:Mono<ResponseEntity<Mono<T>>>"))
                                        .build()
                        )))
                );
            } else {
                responseEntityMonoSink.error(new RuntimeException("error"));
            }
        });
    }

    @GetMapping(value = "mock52", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Mono<ResponseEntity<Flux<ResponseMessage<String>>>> mock52(String name) {
        // Mono<ResponseEntity<ResponseMessage<Flux<String>>>> 不成，仅能到ResponseEntity这层，因为只有这个的resultHandler
        // TODO 是一个类似于jsonl的格式
        return Mono.create(responseEntityMonoSink -> {
            responseEntityMonoSink.onDispose(() -> System.out.println("disposed"));
            responseEntityMonoSink.onCancel(() -> System.out.println("canceled"));
            if (name.equals("ok")) {
                responseEntityMonoSink.success(
                        ResponseEntity.ok(
                                    service.getResList(name)
                        ));
            } else {
                responseEntityMonoSink.error(new RuntimeException("error"));
            }
        });
    }

    @Data
    @Builder
    static class ResponseMessage<T> {
        String code;
        T data;
    }

}
