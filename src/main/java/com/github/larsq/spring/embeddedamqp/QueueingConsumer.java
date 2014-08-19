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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Consumer that stores the messages in a queue until its full capacity is reached.
 *
 * @author Lars Eriksson (larsq.eriksson@gmail.com)
 */
public class QueueingConsumer implements Consumer {
	private final static Logger LOG = LoggerFactory.getLogger(QueueingConsumer.class.getPackage().getName());
	private final BlockingQueue<Message> store;
	private boolean enabled = true;

	public QueueingConsumer(int capacity, boolean isEnabled) {
		this.store = new LinkedBlockingQueue<>(capacity);
		this.enabled = isEnabled;
	}

	BlockingQueue<Message> getStore() {
		return store;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			LOG.debug("set new status: enabled is now {}", enabled);
		}

		this.enabled = enabled;
	}

	@Override
	public void handleConsumeOk(String consumerTag) {
		this.enabled = true;
	}

	@Override
	public void handleCancelOk(String consumerTag) {
		this.enabled = false;
	}

	@Override
	public void handleCancel(String consumerTag) throws IOException {
		this.enabled = false;
	}

	@Override
	public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
	}

	@Override
	public void handleRecoverOk(String consumerTag) {
	}

	@Override
	public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
		if (!enabled) {
			LOG.warn("consumer is not enabled: ignoring message");
			return;
		}

		Message message = new Message(envelope, body, properties);
		boolean added = store.add(message);

		if (!added) {
			throw new IOException("cannot store message", new IllegalStateException());
		}
	}

	public void purge() {
		store.clear();
	}

	public int size() {
		return store.size();
	}
}
