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

package local.laer.app.spring.embeddedampq.support;

import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;
import org.springframework.messaging.Message;

/**
 *
 * @author Lars Eriksson (larsq.eriksson@gmail.com)
 */
public class AmqpMessageMatchers {
    private AmqpMessageMatchers() {}
    
    public static <T> Matcher<Message<?>> receivedExchange(Matcher<T> expected) {
        return hasHeader("amqp_receivedExchange", is(expected));
    }
    
    public static <T> Matcher<Message<?>> withRoutingKey(T expectedValue) {
        return hasHeader("amqp_receivedRoutingKey", is(expectedValue));
    }
    
    public static Matcher<Message<?>> withNoRoutingKey() {
        return not(hasHeader("amqp_receivedRoutingKey", notNullValue()));
    }
}
