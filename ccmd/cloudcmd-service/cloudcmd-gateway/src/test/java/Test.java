import java.util.concurrent.*;

import reactor.core.publisher.Mono;

public class Test {

    private static final ExecutorService executorService =
        new ThreadPoolExecutor(8, 100, 10L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(200));

    public static void main(String[] args) {
        CompletableFuture<Void> statusFuture = CompletableFuture.runAsync(() -> {
            System.out.println("1");
            throw new RuntimeException();
        }, executorService);
        CompletableFuture<Void> baseAbilitiesFuture = CompletableFuture.runAsync(() -> {
            System.out.println("2");
        }, executorService);

        Mono.fromCompletionStage(CompletableFuture.allOf(statusFuture, baseAbilitiesFuture)).then(Mono.defer(() -> {
            System.out.println("3");
            return Mono.empty();
        })).onErrorResume(e -> {
            e.printStackTrace();
            return Mono.empty();
        }).subscribe();
    }
}
