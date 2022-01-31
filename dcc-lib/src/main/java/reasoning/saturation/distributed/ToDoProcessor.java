package reasoning.saturation.distributed;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import util.ConsoleUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class ToDoProcessor implements Processor<Object, Object> {

    private Logger log = ConsoleUtils.getLogger();

    private BlockingQueue<Object> toDo = new LinkedBlockingQueue<>();
    private Subscriber<? super Object> subscriber;

    private Subscription messagesToSendSub = new ToDoSubscription();
    private Subscription receivedMessagesSub;

    public ToDoProcessor() {
    }

    @Override
    public void subscribe(Subscriber<? super Object> subscriber) {
        this.subscriber = subscriber;
        this.subscriber.onSubscribe(messagesToSendSub);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.receivedMessagesSub = subscription;
    }

    @Override
    public void onNext(Object o) {
        this.subscriber.onNext(o);
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println(throwable.getMessage());
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        log.info("To-Do processor completed.");
    }

    public void addElementsToToDo(Object msg) {
        this.toDo.add(msg);
    }

    public class ToDoSubscription implements Subscription {

        @Override
        public void request(long requestedElements) {
            for (; !toDo.isEmpty() && requestedElements > 0; requestedElements--) {
                subscriber.onNext(toDo.poll());
            }
            receivedMessagesSub.request(requestedElements);
        }

        @Override
        public void cancel() {
            subscriber = null;
        }
    }


}
