package benchmark.microbenchmark;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

public class FluxExperiments {

    public static final boolean FLUX_AUTOCANCEL = false;
    public static final int BACKPRESSURE_QUEUE_SIZE = 10;

    public static void main(String[] args) {
        Flux<String> flux = Flux.generate(
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3 * state);
                    if (state == 10) sink.complete();
                    return state + 1;
                });
        flux.subscribe(System.out::println);

        Sinks.Many<String> stringSink = Sinks.many().multicast().onBackpressureBuffer(1, FLUX_AUTOCANCEL);


        System.out.println("Subscribers: " + stringSink.currentSubscriberCount());
        int subCount = 1;
        StringSubscriber s1 = new StringSubscriber("S" + subCount++);
        StringSubscriber s2 = new StringSubscriber("S" + subCount++);
        StringSubscriber s3 = new StringSubscriber("S" + subCount++);
        StringSubscriber s4 = new StringSubscriber("S" + subCount++);

        stringSink.asFlux()
                .filter(str -> str.endsWith("4"))
                .subscribe(s1);
        stringSink.asFlux().subscribe(s2);
        stringSink.asFlux().subscribe(s3);
        stringSink.asFlux().subscribe(s4);

        System.out.println("Subscribers: " + stringSink.currentSubscriberCount());
        int msgCounter = 1;

        Sinks.EmitFailureHandler failureHandler = new Sinks.EmitFailureHandler() {
            @Override
            public boolean onEmitFailure(SignalType signalType, Sinks.EmitResult emitResult) {
                System.err.println(signalType.toString());
                System.err.println(emitResult);
                return false;
            }
        };
        for (int i = 0; i < 4; i++) {
            String msg = "message " + msgCounter++;
            stringSink.emitNext(msg, (signalType, emitResult) -> {
                //System.err.println(signalType);
                System.err.println("Message could not be sent: " + msg);
                return false;
            });
        }

        for (int i = 0; i < 10; i++) {
            s1.requestNext();
            s2.requestNext();
            s3.requestNext();
            s4.requestNext();
        }

    }

    public static class StringSubscriber implements Subscriber<String> {

        String subscriberName;
        Subscription subscription;

        public StringSubscriber(String subscriberName) {
            this.subscriberName = subscriberName;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            System.out.println(subscriberName + " subscribed. ");
            this.subscription = subscription;
            this.subscription.request(1);
        }

        @Override
        public void onNext(String s) {
            System.out.println(subscriberName + ": " + s);
        }

        @Override
        public void onError(Throwable throwable) {
            System.err.println(subscriberName + ": " + throwable.getMessage());
        }

        @Override
        public void onComplete() {
            System.err.println(subscriberName + ": complete.");
        }

        public void requestNext() {
            subscription.request(1);
        }
    }
}
