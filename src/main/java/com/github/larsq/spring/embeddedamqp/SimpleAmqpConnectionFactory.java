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
package com.github.larsq.spring.embeddedamqp;

import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.BlockedListener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ExceptionHandler;
import com.rabbitmq.client.FlowListener;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.Method;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.AMQImpl;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.github.larsq.support.SuppressedThrowable;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AnonymousQueue;

/**
 *
 * @author lars
 */
public class SimpleAmqpConnectionFactory extends ConnectionFactory {
    private final static Logger LOG = LoggerFactory.getLogger(SimpleAmqpConnectionFactory.class.getPackage().getName());
    private final static Supplier<SuppressedThrowable<IOException>> IOException = () -> SuppressedThrowable.wrap(IOException.class);

    /**
     * Support class to define event listeners.
     * @param <T> the listener type
     */
    private static class EventListenerList<T> {
        private final List<T> listeners = new ArrayList<>();

        public EventListenerList() {
        }

        public void add(T listener) {
            if (listener != null) {
                listeners.add(listener);
            }
        }

        public boolean remove(T listener) {
            return listeners.remove(listener);
        }

        public void clear() {
            this.listeners.clear();
        }

        public void forEach(Consumer<T> action) {
            listeners.forEach(action);
        }
    }

    
    private final SimpleAmqpMessageContainer container;

    public SimpleAmqpConnectionFactory() {
        this(new SimpleAmqpMessageContainer());
    }

    public SimpleAmqpConnectionFactory(SimpleAmqpMessageContainer container) {
        this.container = container;
    }

    protected static class ChannelImpl implements Channel {
        private final SimpleAmqpConnectionFactory.ConnectionImpl connection;
        private final int channelNumber;

        private Integer closeCode;
        private String closeMessage;
        private boolean flowBlocked = false;
        private com.rabbitmq.client.Consumer defaultConsumer;

        private final EventListenerList<ReturnListener> returnListeners = new EventListenerList<>();
        private final EventListenerList<ConfirmListener> confirmListeners = new EventListenerList<>();
        private final EventListenerList<FlowListener> flowListeners = new EventListenerList<>();
        private final EventListenerList<ShutdownListener> shutdownListeners = new EventListenerList<>();

        /**
         * Used to simulated connection blockings
         *
         * @param flowBlocked of the flow is set as "blocked"
         */
        public void setFlowBlocked(boolean flowBlocked) {
            this.flowBlocked = flowBlocked;
            //is not expected that any exception are raised
            flowListeners.forEach(Unchecked.consumer(l -> l.handleFlow(!flowBlocked)));
        }

        ChannelImpl(int channelNumber, SimpleAmqpConnectionFactory.ConnectionImpl connection) {
            this.channelNumber = channelNumber;
            this.connection = connection;
        }

        @Override
        public int getChannelNumber() {
            return channelNumber;
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public void close() throws IOException {
            close(AMQP.REPLY_SUCCESS, "OK");
        }

        @Override
        public void close(int closeCode, String closeMessage) throws IOException {
            this.closeCode = closeCode;
            this.closeMessage = closeMessage;
            
            container().unsubscribeAll(this);
        }

        @Override
        public boolean flowBlocked() {
            return flowBlocked;
        }

        @Override
        public void abort() throws IOException {
            close();
        }

        @Override
        public void abort(int closeCode, String closeMessage) throws IOException {
            close(closeCode, closeMessage);
        }

        @Override
        public void addReturnListener(ReturnListener listener) {
            returnListeners.add(listener);
        }

        @Override
        public boolean removeReturnListener(ReturnListener listener) {
            return returnListeners.remove(listener);
        }

        @Override
        public void clearReturnListeners() {
            returnListeners.clear();
        }

        @Override
        public void addFlowListener(FlowListener listener) {
            flowListeners.add(listener);
        }

        @Override
        public boolean removeFlowListener(FlowListener listener) {
            return flowListeners.remove(listener);
        }

        @Override
        public void clearFlowListeners() {
            flowListeners.clear();
        }

        @Override
        public void addConfirmListener(ConfirmListener listener) {
            confirmListeners.add(listener);
        }

        @Override
        public boolean removeConfirmListener(ConfirmListener listener) {
            return confirmListeners.remove(listener);
        }

        @Override
        public void clearConfirmListeners() {
            confirmListeners.clear();
        }

        @Override
        public com.rabbitmq.client.Consumer getDefaultConsumer() {
            return defaultConsumer;
        }

        @Override
        public void setDefaultConsumer(com.rabbitmq.client.Consumer consumer) {
            this.defaultConsumer = consumer;
        }

        @Override
        /**
         * Not yet supported
         */
        public void basicQos(int prefetchSize, int prefetchCount, boolean global) throws IOException {
        }

        @Override
        /**
         * Not yet supported
         */
        public void basicQos(int prefetchCount, boolean global) throws IOException {

        }

        @Override
        /**
         * Not yet supported
         */
        public void basicQos(int prefetchCount) throws IOException {

        }

        protected SimpleAmqpMessageContainer container() {
            return connection.factory.container;
        }

        @Override
        public void basicPublish(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body) throws IOException {
            basicPublish(exchange, routingKey, false, false, props, body);
        }

        @Override
        public void basicPublish(String exchange, String routingKey, boolean mandatory, AMQP.BasicProperties props, byte[] body) throws IOException {
            basicPublish(exchange, routingKey, mandatory, false, props, body);
        }

        @Override
        public void basicPublish(String exchange, String routingKey, boolean mandatory, boolean immediate, AMQP.BasicProperties props, byte[] body) throws IOException {
            LOG.debug("publishing message: {} {}", exchange, routingKey);

            container().publish(this, exchange, routingKey, mandatory, immediate, props, body);
        }

        @Override
        public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, String type) throws IOException {
            return exchangeDeclare(exchange, type, true, true, false, null);
        }

        @Override
        public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, String type, boolean durable) throws IOException {
            return exchangeDeclare(exchange, type, durable, true, false, null);
        }

        @Override
        public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, String type, boolean durable, boolean autoDelete, Map<String, Object> arguments) throws IOException {
            return exchangeDeclare(exchange, type, durable, autoDelete, false, arguments);
        }

        @Override
        public AMQP.Exchange.DeclareOk exchangeDeclare(String exchange, String type, boolean durable, boolean autoDelete, boolean internal, Map<String, Object> arguments) throws IOException {
            container().declareExchange(exchange, type);
            return new AMQImpl.Exchange.DeclareOk();
        }

        @Override
        public AMQP.Exchange.DeclareOk exchangeDeclarePassive(String name) throws IOException {
            ExchangeWrapper wrapper = container().exchange(name).orElseThrow(ExceptionSuppliers.Exception.exchangeNotFound(name));
            return new AMQImpl.Exchange.DeclareOk();
        }

        @Override
        public AMQP.Exchange.DeleteOk exchangeDelete(String exchange, boolean ifUnused) throws IOException {
            container().exchangeDelete(exchange);
            return new AMQImpl.Exchange.DeleteOk();
        }

        @Override
        public AMQP.Exchange.DeleteOk exchangeDelete(String exchange) throws IOException {
            return exchangeDelete(exchange, true);
        }

        @Override
        public AMQP.Exchange.BindOk exchangeBind(String destination, String source, String routingKey) throws IOException {
            return exchangeBind(destination, source, routingKey, null);
        }

        @Override
        public AMQP.Exchange.BindOk exchangeBind(String destination, String source, String routingKey, Map<String, Object> arguments) throws IOException {
            container().exchangeBind(destination, source, routingKey, arguments);
            return new AMQImpl.Exchange.BindOk();
        }

        @Override
        public AMQP.Exchange.UnbindOk exchangeUnbind(String destination, String source, String routingKey) throws IOException {
            return exchangeUnbind(destination, source, routingKey, null);
        }

        @Override
        public AMQP.Exchange.UnbindOk exchangeUnbind(String destination, String source, String routingKey, Map<String, Object> arguments) throws IOException {
            container().exchangeUnbind(destination, source, routingKey, arguments);

            return new AMQImpl.Exchange.UnbindOk();
        }

        @Override
        public AMQP.Queue.DeclareOk queueDeclare() throws IOException {
            AnonymousQueue anonymousQueue = new AnonymousQueue();
            return queueDeclare(anonymousQueue.getName(), false, false, true, null);
        }

        @Override
        public AMQP.Queue.DeclareOk queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments) throws IOException {
            QueueInfo info = container().queueDeclare(new org.springframework.amqp.core.Queue(queue, durable, exclusive, autoDelete, arguments));

            return new AMQImpl.Queue.DeclareOk(info.getQueue(), info.getMessageCount(), info.getConsumerCount());
        }

        @Override
        public AMQP.Queue.DeclareOk queueDeclarePassive(String queue) throws IOException {
            Optional<QueueInfo> optionalInfo = container().requestQueue(queue);

            QueueInfo info = optionalInfo.orElseThrow(ExceptionSuppliers.Exception.queueNotFound(queue));

            return new AMQImpl.Queue.DeclareOk(info.getQueue(), info.getMessageCount(), info.getConsumerCount());
        }

        @Override
        public AMQP.Queue.DeleteOk queueDelete(String queue) throws IOException {
            return queueDelete(queue, true, true);
        }

        @Override
        public AMQP.Queue.DeleteOk queueDelete(String queue, boolean ifUnused, boolean ifEmpty) throws IOException {
            QueueInfo info = container().queueDelete(queue);

            return new AMQImpl.Queue.DeleteOk(info.getMessageCount());
        }

        @Override
        public AMQP.Queue.BindOk queueBind(String queue, String exchange, String routingKey) throws IOException {
            return queueBind(queue, exchange, routingKey, null);
        }

        @Override
        public AMQP.Queue.BindOk queueBind(String queue, String exchange, String routingKey, Map<String, Object> arguments) throws IOException {
            container().queueBind(queue, exchange, routingKey, arguments);
            return new AMQImpl.Queue.BindOk();
        }

        @Override
        public AMQP.Queue.UnbindOk queueUnbind(String queue, String exchange, String routingKey) throws IOException {
            return queueUnbind(queue, exchange, routingKey, null);
        }

        @Override
        public AMQP.Queue.UnbindOk queueUnbind(String queue, String exchange, String routingKey, Map<String, Object> arguments) throws IOException {
            container().queueUnbind(routingKey, queue, routingKey, arguments);
            return new AMQImpl.Queue.UnbindOk();
        }

        @Override
        public AMQP.Queue.PurgeOk queuePurge(String queue) throws IOException {
            Optional<QueueInfo> info = container().requestQueue(queue);

            info.ifPresent(q -> q.purge());

            return new AMQImpl.Queue.PurgeOk(info.map(q -> q.getMessageCount()).orElse(0));
        }

        @Override
        public GetResponse basicGet(String queue, boolean autoAck) throws IOException {
            SuppressedThrowable<IOException> suppressed = IOException.get();
            LOG.debug("get message: {}", queue);

            Optional<QueueInfo> info = container().requestQueue(queue);

            Optional<GetResponse> response = info.flatMap(q -> q.receive());

            if (!response.isPresent()) {
                return null;
            }

            if (autoAck) {
                long deliveryTag = response.get().getEnvelope().getDeliveryTag();
                confirmListeners.forEach(Unchecked.consumer(l -> l.handleAck(deliveryTag, false), suppressed));

                suppressed.check();
            }

            return response.get();
        }

        @Override
        public void basicAck(long deliveryTag, boolean multiple) throws IOException {
            SuppressedThrowable<IOException> suppressed = IOException.get();

            confirmListeners.forEach(Unchecked.consumer(l -> l.handleAck(deliveryTag, false), suppressed));

            suppressed.check();
        }

        @Override
        public void basicNack(long deliveryTag, boolean multiple, boolean requeue) throws IOException {
            SuppressedThrowable<IOException> suppressed = IOException.get();

            confirmListeners.forEach(Unchecked.consumer(l -> l.handleNack(deliveryTag, multiple), suppressed));

            suppressed.check();
        }

        @Override
        /**
         * Not supported
         */
        public void basicReject(long deliveryTag, boolean requeue) throws IOException {

        }

        @Override
        public String basicConsume(String queue, com.rabbitmq.client.Consumer callback) throws IOException {
            return basicConsume(queue, true, null, false, false, null, callback);
        }

        @Override
        public String basicConsume(String queue, boolean autoAck, com.rabbitmq.client.Consumer callback) throws IOException {
            return basicConsume(queue, autoAck, null, false, false, null, callback);
        }

        @Override
        public String basicConsume(String queue, boolean autoAck, Map<String, Object> arguments, com.rabbitmq.client.Consumer callback) throws IOException {
            return basicConsume(queue, autoAck, null, false, false, arguments, callback);
        }

        @Override
        public String basicConsume(String queue, boolean autoAck, String consumerTag, com.rabbitmq.client.Consumer callback) throws IOException {
            return basicConsume(queue, autoAck, consumerTag, false, false, null, callback);
        }

        @Override
        public String basicConsume(String queue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive, Map<String, Object> arguments, com.rabbitmq.client.Consumer callback) throws IOException {
            String tag = container().subscribe(this, consumerTag, queue, callback);

            LOG.debug("register consumer: {} {}", tag, queue);

            return tag;
        }

        @Override
        public void basicCancel(String consumerTag) throws IOException {
            container().unsubscribe(consumerTag);
        }

        @Override
        /**
         * Not supported
         */
        public AMQP.Basic.RecoverOk basicRecover() throws IOException {
            return new AMQImpl.Basic.RecoverOk();
        }

        @Override
        /**
         * Not supported
         */
        public AMQP.Basic.RecoverOk basicRecover(boolean requeue) throws IOException {
            return new AMQImpl.Basic.RecoverOk();
        }

        @Override
        /**
         * Not supported
         */
        public void basicRecoverAsync(boolean requeue) throws IOException {
        }

        @Override
        /**
         * Not supported
         */
        public AMQP.Tx.SelectOk txSelect() throws IOException {
            return new AMQImpl.Tx.SelectOk();
        }

        @Override
        /**
         * Not supported
         */

        public AMQP.Tx.CommitOk txCommit() throws IOException {
            return new AMQImpl.Tx.CommitOk();
        }

        @Override
        /**
         * Not supported
         */

        public AMQP.Tx.RollbackOk txRollback() throws IOException {
            return new AMQImpl.Tx.RollbackOk();
        }

        @Override
        public AMQP.Confirm.SelectOk confirmSelect() throws IOException {
            return new AMQImpl.Confirm.SelectOk();
        }

        @Override
        public long getNextPublishSeqNo() {
            return container().nextSequenceNumber(this);
        }

        @Override
        /**
         * Not supported
         */

        public boolean waitForConfirms() throws InterruptedException {
            return true;
        }

        @Override
        /**
         * Not supported
         */

        public boolean waitForConfirms(long timeout) throws InterruptedException, TimeoutException {
            return true;
        }

        @Override
        /**
         * Not supported
         */

        public void waitForConfirmsOrDie() throws IOException, InterruptedException {

        }

        @Override
        /**
         * Not supported
         */

        public void waitForConfirmsOrDie(long timeout) throws IOException, InterruptedException, TimeoutException {
        }

        @Override
        /**
         * Not supported
         */
        public void asyncRpc(Method method) throws IOException {
            throw new UnsupportedOperationException("not yet supported");
        }

        @Override
        /**
         * Not supported
         */
        public Command rpc(Method method) throws IOException {
            throw new UnsupportedOperationException("not yet supported");
        }

        @Override
        public void addShutdownListener(ShutdownListener listener) {
            shutdownListeners.add(listener);
        }

        @Override
        public void removeShutdownListener(ShutdownListener listener) {
            shutdownListeners.remove(listener);
        }

        @Override
        public ShutdownSignalException getCloseReason() {
            return new ShutdownSignalException(false, true, null, closeMessage);
        }

        @Override
        public void notifyListeners() {
        }

        @Override
        public boolean isOpen() {
            return closeCode == null;
        }

        @Override
        public String toString() {
            return String.format("%s{channelNumber=%s}", getClass().getSimpleName(), getChannelNumber());
        }

    }

    protected static class ConnectionImpl implements Connection {
        private final static int NOT_YET_CLOSED = -1;

        protected final Address connectedTo;
        protected final SimpleAmqpConnectionFactory factory;
        private int closeCode = NOT_YET_CLOSED;
        private String closeMessage;
        private final Map<String, List<com.rabbitmq.client.Consumer>> consumers = new HashMap<>();
        private final EventListenerList<BlockedListener> blockedListeners = new EventListenerList<>();
        private final EventListenerList<ShutdownListener> shutdownListeners = new EventListenerList<>();

        protected ConnectionImpl(Address connectedTo, SimpleAmqpConnectionFactory factory) {
            this.connectedTo = connectedTo;
            this.factory = factory;
        }

        @Override
        public InetAddress getAddress() {
            try {
                return InetAddress.getByName(connectedTo.getHost());
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public int getPort() {
            return connectedTo.getPort();
        }

        @Override
        public int getChannelMax() {
            return factory.getRequestedChannelMax();
        }

        @Override
        public int getFrameMax() {
            return factory.getRequestedFrameMax();
        }

        @Override
        public int getHeartbeat() {
            return factory.getRequestedHeartbeat();
        }

        @Override
        public Map<String, Object> getClientProperties() {
            return factory.getClientProperties();
        }

        @Override
        public Map<String, Object> getServerProperties() {
            //get empty map
            return ImmutableMap.of();
        }

        @Override
        public Channel createChannel() throws IOException {
            return createChannel(0);
        }

        @Override
        public Channel createChannel(int channelNumber) throws IOException {
            return new ChannelImpl(channelNumber, this);
        }

        @Override
        public void close() throws IOException {
            close(AMQP.REPLY_SUCCESS, "OK");
        }

        @Override
        public void close(int closeCode, String closeMessage) throws IOException {
            this.closeCode = closeCode;
            this.closeMessage = closeMessage;
        }

        @Override
        public void close(int timeout) throws IOException {
            close(AMQP.REPLY_SUCCESS, "OK");
        }

        @Override
        public void close(int closeCode, String closeMessage, int timeout) throws IOException {
            close(closeCode, closeMessage);
        }

        @Override
        public void abort() {
            abort(AMQP.REPLY_SUCCESS, "OK");
        }

        @Override
        public void abort(int closeCode, String closeMessage) {
            this.closeCode = closeCode;
            this.closeMessage = closeMessage;
        }

        @Override
        public void abort(int timeout) {
            abort();
        }

        @Override
        public void abort(int closeCode, String closeMessage, int timeout) {
            abort(closeCode, closeMessage);
        }

        @Override
        public void addBlockedListener(BlockedListener listener) {
            blockedListeners.add(listener);
        }

        @Override
        public boolean removeBlockedListener(BlockedListener listener) {
            return blockedListeners.remove(listener);
        }

        @Override
        public void clearBlockedListeners() {
            blockedListeners.clear();
        }

        @Override
        public ExceptionHandler getExceptionHandler() {
            return factory.getExceptionHandler();
        }

        @Override
        public boolean isOpen() {
            return closeCode == NOT_YET_CLOSED;
        }

        @Override
        public void addShutdownListener(ShutdownListener listener) {
            shutdownListeners.add(listener);
        }

        @Override
        public void removeShutdownListener(ShutdownListener listener) {
            shutdownListeners.remove(listener);
        }

        @Override
        public ShutdownSignalException getCloseReason() {
            return new ShutdownSignalException(false, true, null, null);
        }

        @Override
        public void notifyListeners() {
        }

    }

    @Override
    public Connection newConnection(ExecutorService executor) throws IOException {
        return newConnection();
    }

    @Override
    public Connection newConnection() throws IOException {
        return new ConnectionImpl(new Address(getHost(), getPort()), this); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Connection newConnection(ExecutorService executor, Address[] addrs) throws IOException {
        return new ConnectionImpl(addrs[0], this);
    }

    @Override
    public Connection newConnection(Address[] addrs) throws IOException {
        return new ConnectionImpl(addrs[0], this); //To change body of generated methods, choose Tools | Templates.
    }

}
