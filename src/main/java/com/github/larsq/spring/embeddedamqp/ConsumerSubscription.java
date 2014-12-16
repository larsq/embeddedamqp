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

package com.github.larsq.spring.embeddedamqp;

import com.rabbitmq.client.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concrete implementation of a subscription with the {@link Consumer} as
 * callback interface.
 *
 * @author Lars Eriksson (larsq.eriksson@gmail.com)
 */
class ConsumerSubscription extends AbstractSubscription<Consumer> {
	private final AtomicInteger counter = new AtomicInteger();
	private final static Logger LOG = LoggerFactory.getLogger(ConsumerSubscription.class.getPackage().getName());

	ConsumerSubscription(Consumer instance) {
		this(new ChannelWrapper(), "", instance);
	}

	ConsumerSubscription(ChannelWrapper owner, String tag, Consumer instance) {
		super(owner, tag, instance);
	}

	/**
	 * @param message the message to be delivered to the consumer
	 * @throws IOException
	 */
	@Override
	void onMessage(Message message) throws IOException {
		counter.incrementAndGet();

		LOG.debug("message delivered to : {} {} {}", owner(), tag(), instance());
		instance().handleDelivery(tag(), message.getEnvelope(), message.getBasicProperties(), message.getPayload());
	}
}
