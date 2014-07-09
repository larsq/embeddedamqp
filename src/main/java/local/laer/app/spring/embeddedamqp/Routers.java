/*
 * The MIT License
 *
 * Copyright 2014 Lars Eriksson (larsq.eriksson@gmail.com).
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
package local.laer.app.spring.embeddedamqp;

import com.rabbitmq.client.AMQP;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import local.laer.app.support.Predicates;
import local.laer.app.support.SuppressedThrowable;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Queue;

/**
 *
 * @author Lars Eriksson (larsq.eriksson@gmail.com) private static final ExceptionSuppliers exceptions = new ExceptionSuppliers();
    
   
 */
class Routers {
    private final static Logger LOG = LoggerFactory.getLogger(Routers.class.getPackage().getName());
    private final static Supplier<SuppressedThrowable<IOException>> IOException = ()->SuppressedThrowable.wrap(IOException.class);
    
    static class HeadersExchangeRouter extends AbstractExchangeRouter {

        private final static Logger LOG = LoggerFactory.getLogger(HeadersExchangeRouter.class.getPackage().getName());

        HeadersExchangeRouter(SimpleAmqpMessageContainer container) {
            super(container, ExchangeTypes.HEADERS);
        }

        protected boolean containsKeyValue(AMQP.BasicProperties properties, String key, Object value) {
            Field field = Arrays.stream(properties.getClass().getDeclaredFields())
                    .filter(f -> Objects.equals(f.getName(), key))
                    .findFirst()
                    .orElse(null);

            if (field != null) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                try {
                    return Objects.equals(field.get(properties), value);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }

            if (properties.getHeaders() == null) {
                return false;
            }

            return Objects.equals(properties.getHeaders().get(key), value);
        }

        protected Predicate<SimpleAmqpMessageContainer.Binding> matchesHeader(AMQP.BasicProperties properties) {
            return b -> b.getArguments().entrySet().stream()
                    .allMatch(e -> containsKeyValue(properties, e.getKey(), e.getValue()));
        }

        @Override
        public void route(ExchangeWrapper exchangeWrapper, String routingKey, Message message) throws IOException {
            final SuppressedThrowable<IOException> suppressed = IOException.get();
            
            container.bindings(exchangeWrapper).stream()
                    .filter(matchesHeader(message.getBasicProperties()))
                    .forEach(Unchecked.consumer(b -> container.route(routingKey, b, message), suppressed));
            
            suppressed.check();
        }

    }

    static class FanoutExchangeRouter extends AbstractExchangeRouter {

        private final static Logger LOG = LoggerFactory.getLogger(HeadersExchangeRouter.class.getPackage().getName());

        FanoutExchangeRouter(SimpleAmqpMessageContainer container) {
            super(container, ExchangeTypes.FANOUT);
        }

        @Override
        public void route(ExchangeWrapper exchangeWrapper, String routingKey, Message message) throws IOException {
            LOG.debug("routing fanout {}", exchangeWrapper.name);
            
            final SuppressedThrowable<IOException> suppressed = IOException.get();
 
            container.bindings(exchangeWrapper).stream()
                    .forEach(Unchecked.consumer(b -> container.route(routingKey, b, message), suppressed));
        
            suppressed.check();
        }

    }

    static class DirectExchangeRouter extends AbstractExchangeRouter {

        private final static Logger LOG = LoggerFactory.getLogger(HeadersExchangeRouter.class.getPackage().getName());

        public DirectExchangeRouter(SimpleAmqpMessageContainer container) {
            super(container, ExchangeTypes.DIRECT);
        }

        @Override
        public void route(ExchangeWrapper exchangeWrapper, String routingKey, Message message) throws IOException {
            final SuppressedThrowable<IOException> suppressed = IOException.get();

            container.bindings(exchangeWrapper).stream()
                    .filter(b -> Objects.equals(b.getRoutingKey(), routingKey))
                    .forEach(Unchecked.consumer(b -> container.route(routingKey, b, message), suppressed));
        
            suppressed.check();
        }
    }

    static class TopicExchangeRouter extends AbstractExchangeRouter {
        private final static Logger LOG = LoggerFactory.getLogger(HeadersExchangeRouter.class.getPackage().getName());

        TopicExchangeRouter(SimpleAmqpMessageContainer container) {
            super(container, ExchangeTypes.TOPIC);
        }

        private Pattern translatePattern(String queuePattern) throws IOException {
            final String startsWithPoundQueueuPattern = "^#+";
            final String escapeDotQueuePattern = "[.](?!\\])";
            final String esacpeDotRegExpPattern = "[.]";
            final String poundQueuePattern = "\\.?#+";  //any dot before the pound must be removed
            final String starQueuePattern = "[*]+";
            final String starRegexpPattern = "[^.]+";
            final String poundRegexpPattern = "([.][^.]+)*";
            final String startsWithPoundRegexpPattern = "[^.]+" + poundRegexpPattern;

            String regexpPattern = queuePattern
                    .replaceAll(starQueuePattern, starRegexpPattern)
                    .replaceAll(startsWithPoundQueueuPattern, startsWithPoundRegexpPattern)
                    .replaceAll(poundQueuePattern, poundRegexpPattern)
                    .replaceAll(escapeDotQueuePattern, esacpeDotRegExpPattern);

            LOG.debug("effective pattern {} : {}", queuePattern, regexpPattern);

            try {
                Pattern pattern = Pattern.compile(regexpPattern);
                return pattern;
            } catch (PatternSyntaxException e) {
                throw new IOException("topic pattern is not valid", e);
            }
        }

        @Override
        public void route(ExchangeWrapper exchangeWrapper, String routingKey, Message message) throws IOException {
            final SuppressedThrowable<IOException> suppressed = IOException.get();
 
            Pattern p = translatePattern(routingKey);

            Set<String> availableQueues = container.availableQueues();
            
            availableQueues.stream()
                    .filter(Predicates.String.matches(p))
                    .forEach(Unchecked.consumer(str -> container.route(new Queue(str), message), suppressed));
        }
    }
}
