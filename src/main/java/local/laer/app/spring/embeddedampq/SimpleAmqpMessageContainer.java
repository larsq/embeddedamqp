/*
 * The MIT License
 *
 * Copyright 2014 lars.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package local.laer.app.spring.embeddedampq;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import static java.util.function.Predicate.isEqual;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import static local.laer.app.spring.embeddedampq.ExceptionSuppliers.Exception;
import local.laer.app.support.ClassStructureWalker;
import local.laer.app.support.Predicates;
import static local.laer.app.support.Predicates.Entry.key;
import static local.laer.app.support.Predicates.Object.instanceOf;
import static local.laer.app.support.Predicates.Object.untyped;
import static local.laer.app.support.Predicates.compose;
import local.laer.app.support.SuppressedThrowable;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;

/**
 *
 * @author lars
 */
public class SimpleAmqpMessageContainer {
    private final static String SYSTEM = "_$sys";

    private final static Logger LOG = LoggerFactory.getLogger(SimpleAmqpMessageContainer.class.getPackage().getName());

    /**
     * Internal representation of a binding. 
     */
    class Binding {
        private final org.springframework.amqp.core.Binding.DestinationType type;
        private final String source;
        private final String destination;
        private final String routingKey;
        private final Map<String, Object> arguments;

        public Binding(org.springframework.amqp.core.Binding.DestinationType type, String source, String destination, String routingKey, Map<String, Object> arguments) {
            this.type = type;
            this.source = source;
            this.destination = destination;
            this.routingKey = routingKey;
            this.arguments = arguments;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj == this) {
                return obj == this;
            }

            if (!getClass().equals(obj.getClass())) {
                return false;
            }

            if (hashCode() != obj.hashCode()) {
                return false;
            }

            Binding other = getClass().cast(obj);

            return Objects.equals(source, other.source)
                    && Objects.equals(destination, other.destination)
                    && Objects.equals(type, other.type)
                    && (routingKey == other.routingKey || Objects.equals(routingKey, other.routingKey))
                    && (arguments == other.arguments || Objects.equals(arguments, other.arguments));
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, destination, type, routingKey, arguments);
        }

        boolean isQueueDestination() {
            return Objects.equals(org.springframework.amqp.core.Binding.DestinationType.QUEUE, type);
        }

        boolean isExchangeDestination() {
            return Objects.equals(org.springframework.amqp.core.Binding.DestinationType.EXCHANGE, type);

        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public org.springframework.amqp.core.Binding.DestinationType getType() {
            return type;
        }

        public String getSource() {
            return source;
        }

        public String getDestination() {
            return destination;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        @Override
        public String toString() {
            return String.format("%s{s=%s, d=%s, key=%s, arguments=%s}", 
                    getClass().getSimpleName(),
                    source,
                    destination,
                    routingKey,
                    arguments
            );
        }
        
        
    }


    private final Set<? extends AbstractExchangeRouter> routers;
    private final SortedSet<ExchangeWrapper> exchanges;
    private final WeakHashMap<ExchangeWrapper, List<Binding>> bindings;
    private final ConcurrentHashMap<Queue, List<AbstractSubscription<Consumer>>> consumers;
    private final AtomicLong sequenceNumber = new AtomicLong(0L);
    private final ConcurrentHashMap<ChannelWrapper, Long> nextSequenceNumber;

    /**
     * Topic exchange could be done without having any explicit exchange name.
     */
    private final Routers.TopicExchangeRouter topicExchangeRouter = new Routers.TopicExchangeRouter(SimpleAmqpMessageContainer.this);

    public SimpleAmqpMessageContainer() {
        consumers = new ConcurrentHashMap<>();
        nextSequenceNumber = new ConcurrentHashMap<>();
        exchanges = new TreeSet<>();
        bindings = new WeakHashMap<>();
        routers = ImmutableSet.copyOf(SimpleAmqpMessageContainer.this.findRouters());
    }

    long nextSequenceNumber(SimpleAmqpConnectionFactory.ChannelImpl impl) {
        ChannelWrapper key = new ChannelWrapper(impl.getChannelNumber());
        return nextSequenceNumber.computeIfAbsent(key, __ -> sequenceNumber.getAndIncrement());
    }

    
    public void publish(SimpleAmqpConnectionFactory.ChannelImpl sender, String exchange, String routingKey, boolean mandatory, boolean immediate, AMQP.BasicProperties props, byte[] body) throws IOException {
        ChannelWrapper key = new ChannelWrapper(sender.getChannelNumber());

        Long deliveryTag = nextSequenceNumber.computeIfAbsent(key, __ -> sequenceNumber.getAndIncrement());
        nextSequenceNumber.compute(key, (k, v) -> sequenceNumber.getAndIncrement());

        Message message = new Message(new Envelope(deliveryTag, false, exchange, routingKey), body, props);
        Address addressToRoute = defineAddress(exchange, routingKey);

        route(addressToRoute, message);
    }

    public void exchangeBind(String destination, String source, String routingKey, Map<String, Object> arguments) throws IOException {
        Binding b = new Binding(org.springframework.amqp.core.Binding.DestinationType.EXCHANGE, source, destination, routingKey, arguments);

       LOG.debug("exchange binding defined: {}", b);

        
        ExchangeWrapper sourceEx = exchange(source).orElseThrow(Exception.exchangeNotFound(source));
        ExchangeWrapper destEx = exchange(destination).orElseThrow(Exception.exchangeNotFound(destination));

        bindings.computeIfAbsent(sourceEx, __ -> new ArrayList<>()).add(b);
    }

    public void queueBind(String destination, String source, String routingKey, Map<String, Object> arguments) throws IOException {
        Binding b = new Binding(org.springframework.amqp.core.Binding.DestinationType.QUEUE, source, destination, routingKey, arguments);

        LOG.debug("queue binding defined: {}", b);

        ExchangeWrapper sourceEx = exchange(source).orElseThrow(Exception.exchangeNotFound(source));
        Queue destQ = queue(destination).orElseThrow(Exception.queueNotFound(destination));

        bindings.computeIfAbsent(sourceEx, __ -> new ArrayList<>()).add(b);
    }

    public void exchangeUnbind(String destination, String source, String routingKey, Map<String, Object> arguments) {
        Binding b = new Binding(org.springframework.amqp.core.Binding.DestinationType.EXCHANGE, source, destination, routingKey, arguments);
        ExchangeWrapper sourceEx = exchange(source).orElse(null);

        if (sourceEx == null) {
            return;
        }

        bindings.get(sourceEx).remove(b);
    }

    public void queueUnbind(String destination, String source, String routingKey, Map<String, Object> arguments) {
        Binding b = new Binding(org.springframework.amqp.core.Binding.DestinationType.QUEUE, source, destination, routingKey, arguments);
        ExchangeWrapper sourceEx = exchange(source).orElse(null);

        if (sourceEx == null) {
            return;
        }

        bindings.get(sourceEx).remove(b);
    }

    public String subscribe(SimpleAmqpConnectionFactory.ChannelImpl impl, String consumerTag, String queue, Consumer callback) throws IOException {
        Queue q = queue(queue).orElseThrow(Exception.queueNotFound(queue));

        String tag = Optional.ofNullable(consumerTag).filter(isEqual("").negate()).orElse(UUID.randomUUID().toString());

        unsubscribe(consumerTag);

        List<AbstractSubscription<Consumer>> subscriptions = consumers.computeIfAbsent(q, __ -> new ArrayList<>());

        ConsumerSubscription subscription = new ConsumerSubscription(ChannelWrapper.wrap(impl), tag, callback);
        subscriptions.add(subscription);

        callback.handleConsumeOk(consumerTag);

        return tag;
    }

    public void unsubscribeAll(Channel channel) {
        List<AbstractSubscription<Consumer>> activeSubcriptions = subscriptions(ChannelWrapper.wrap(channel));
        
        activeSubcriptions.stream().map(AbstractSubscription::tag).forEach(this::unsubscribe);
    }
    
    public void unsubscribe(String consumerTag) {
        Optional<AbstractSubscription<Consumer>> optionalSubscription = consumer(consumerTag);

        if (!optionalSubscription.isPresent()) {
            return;
        }

        optionalSubscription.get().instance().handleCancelOk(consumerTag);
        consumers.values().stream().forEach(l -> l.removeIf(compose(isEqual(consumerTag), AbstractSubscription::tag)));
    }
    
    List<AbstractSubscription<Consumer>> subscriptions(ChannelWrapper wrapper) {
        return consumers.values().stream()
                .flatMap(l->l.stream())
                .filter(compose(isEqual(wrapper), AbstractSubscription::owner))
                .collect(Collectors.toList());
    }
    
    Optional<Consumer> subscription(String consumerTag) {
        return consumers.values().stream()
                .flatMap(l->l.stream())
                .filter(compose(isEqual(consumerTag), s->s.tag()))
                .findAny().map(s->s.instance());
    }

    public void purgeQueue(String queue) {
        messages(queue).ifPresent(q->q.clear());
    }

    public QueueInfo queueDeclare(Queue queue) {
        Optional<Queue> reference = queue(queue.getName());
        consumers.computeIfAbsent(reference.orElse(queue), __ -> new ArrayList<>());

        ConsumerSubscription defaultSubscription = new ConsumerSubscription(null, String.join(".", SYSTEM, queue.getName()), new QueueingConsumer(100, true));

        return new QueueInfo(this, queue.getName());
    }

    public void exchangeDelete(String exchange) {
        exchanges.removeIf(compose(isEqual(exchange), ExchangeWrapper::name));
    }

    public void declareExchange(String exchange, String type) throws IOException {
        LOG.debug("declare exchange {}: {}", exchange, type);

        Optional<ExchangeWrapper> wrapper = exchange(exchange);

        if (wrapper.isPresent()) {
            throw Exception.exchangeAlreadyExists(exchange).get();
        }

        AbstractExchangeRouter router = router(type).orElseThrow(Exception.routerNotFound(type));

        ExchangeWrapper defined = new ExchangeWrapper(exchange, router);
        exchanges.add(defined);
    }

    int countUnread(String queue) {
       return messages(queue).map(q->q.size()).orElse(0);
    }

    int countConsumers(String queue) {
       return queue(queue)
               .map(q->consumers.get(q))
               .filter(l->!l.isEmpty())
               .map(l->(l.size()-1)).orElse(0);
               
    }

    Optional<BlockingQueue<Message>> messages(String queue) {
        Optional<QueueingConsumer> optionalConsumer = 
                subscription(String.join(".", SYSTEM, queue)).filter(untyped(instanceOf(QueueingConsumer.class))).map(c->(QueueingConsumer)c);
        
        if(!optionalConsumer.isPresent()) {
            return Optional.empty();
        }
        
        return optionalConsumer.map(QueueingConsumer::getStore);
    }
    
    List<AbstractSubscription<Consumer>> subscriptions(String queue) {
        return consumers.entrySet().stream()
                .filter(key(compose(isEqual(queue), Queue::getName)))
                .findAny().map(e -> e.getValue()).orElse(ImmutableList.of());
    }

    
    
    

    /**
     * Support method.
     *
     * @param clz
     * @return
     */
    private Object invokeInnerClassConstructor(Class<?> clz) {
        try {
            Constructor<?> noArgConstructor = clz.getDeclaredConstructor(SimpleAmqpMessageContainer.class);
            if (!noArgConstructor.isAccessible()) {
                noArgConstructor.setAccessible(true);
            }

            return noArgConstructor.newInstance(this);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Return exchange given its name
     *
     * @param name the exchange name
     * @return the first instance that matches the name. It is assumed that the
     * exchange should be unique on its name
     */
    Optional<ExchangeWrapper> exchange(String name) {
        return exchanges.stream()
                .filter(Predicates.compose(Predicate.isEqual(name), ExchangeWrapper::name))
                .findFirst();
    }

    Optional<Queue> queue(String name) {
        return consumers.keySet().stream()
                .filter(Predicates.compose(Predicate.isEqual(name), Queue::getName))
                .findFirst();
    }

    /**
     * Return bindings given exchange
     *
     * @param wrapper the given exchange wrapper instance
     * @return the bindings or the empty list if no bindings are defined for
     * this wrapper.
     */
    List<Binding> bindings(ExchangeWrapper wrapper) {
        return Optional.ofNullable(bindings.get(wrapper)).orElse(ImmutableList.of());
    }

    /**
     * Find set of abstract exchange routers by traversing a comprising class.
     * This method is invoked to discover available router types.
     *
     * @return
     */
    Set<AbstractExchangeRouter> findRouters() {
        ClassStructureWalker walker = new ClassStructureWalker(Routers.class, false, true);
        Iterable<Class<?>> innerClasses = walker.traverseClassStructure(clz -> Sets.newHashSet(clz.getDeclaredClasses()));

        return StreamSupport.stream(innerClasses.spliterator(), false)
                .filter(AbstractExchangeRouter.class::isAssignableFrom)
                .filter(clz -> !Modifier.isAbstract(clz.getModifiers()))
                .map(clz -> (AbstractExchangeRouter) invokeInnerClassConstructor(clz))
                .collect(Collectors.toSet());
    }

    private Optional<? extends AbstractExchangeRouter> router(String kind) {
        if (routers == null) {
            return null;
        }

        return routers.stream().filter(r -> Objects.equals(kind, r.type)).findFirst();
    }

    Optional<AbstractSubscription<Consumer>> consumer(String consumerTag) {
        return consumers.values().stream()
                .flatMap(l -> l.stream())
                .filter(compose(isEqual(consumerTag), AbstractSubscription::tag))
                .findAny();
    }

    void route(Address address, Message message) throws IOException {
        Preconditions.checkNotNull(address, "Address is not specified");

        LOG.debug("route message {} to address {}", message.getEnvelope(), address);

        /**
         * When address type is TOPIC, these
         */
        if (Objects.equals(address.getExchangeType(), ExchangeTypes.TOPIC)) {
            topicExchangeRouter.route(null, address.getRoutingKey(), message);
            return;
        }

        ExchangeWrapper exchangeWrapper = exchange(address.getExchangeName())
                .orElseThrow(Exception.exchangeNotFound(address.getExchangeName()));

        exchangeWrapper.router.route(exchangeWrapper, address.getRoutingKey(), message);
    }

    void route(String routingKey, Binding binding, Message message) throws IOException {
        if (binding.isExchangeDestination()) {
            ExchangeWrapper exchange = exchange(binding.destination)
                    .orElseThrow(Exception.exchangeNotFound(binding.destination));

            exchange.router.route(exchange, routingKey, message);
        }

        if (binding.isQueueDestination()) {
            Queue queue = queue(binding.destination).orElseThrow(Exception.queueNotFound(binding.destination));
            route(queue, message);
        }
    }

    void route(Queue queue, Message message) throws IOException {
        LOG.debug("route message {} to queue {}", message.getEnvelope(), queue);

        SuppressedThrowable<IOException> suppressed = SuppressedThrowable.wrap(IOException.class);

        Queue key = queue(queue.getName())
                .orElseThrow(Exception.queueNotFound(queue.getName()));

        List<AbstractSubscription<Consumer>> list = consumers.get(queue);
        
          Optional<QueueingConsumer> queueingConsumer = 
                subscription(String.join(".", SYSTEM, queue.getName())).filter(untyped(instanceOf(QueueingConsumer.class))).map(c->(QueueingConsumer)c);
        
          queueingConsumer.ifPresent(c->c.setEnabled(list.size()>1));
          
        
        list.forEach(Unchecked.consumer(s -> s.onMessage(message), suppressed));

        suppressed.check();
    }

    protected Address defineAddress(String exchangeName, String routingKey) {
        if (exchangeName != null && routingKey != null) {
            return new Address(ExchangeTypes.DIRECT, exchangeName, routingKey);
        }

        if (exchangeName == null) {
            return new Address(ExchangeTypes.TOPIC, null, routingKey);
        }

        if (routingKey == null) {
            return new Address(ExchangeTypes.FANOUT, exchangeName, null);
        }

        //both are null
        throw new NullPointerException("both exchange and routing key cannot be null");
    }

    Set<String> availableQueues() {
        return consumers.keySet().stream()
                .map(Queue::getName)
                .collect(Collectors.toSet());
    }

    Optional<QueueInfo> requestQueue(String queueName) throws IOException {
        return queue(queueName).map(q -> new QueueInfo(this, queueName));
    }

    QueueInfo queueDelete(String queueName) throws IOException {
        SuppressedThrowable<IOException> suppressed = SuppressedThrowable.wrap(IOException.class);

        Optional<Queue> queue = queue(queueName);

        QueueInfo info = new QueueInfo(this, queueName);

        if (queue.isPresent()) {
            List<AbstractSubscription<Consumer>> subscriptions = consumers.remove(queue.get());
            subscriptions.forEach(Unchecked.consumer(s -> s.instance().handleCancel(s.tag()), suppressed));
            suppressed.check();
        }

        return info;
    }

}
