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

import com.github.larsq.spring.embeddedamqp.support.AmqpMessageMatchers;
import com.google.common.base.Charsets;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;

/**
 * The purpose with the text is to test if the mockup works in an Integration
 * flow
 *
 * @author lars
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("dev")
@ContextConfiguration(value = "classpath*:test-context.xml")
public class ConnectionFactoryTest {
	private final static Logger LOG = LoggerFactory.getLogger(ConnectionFactoryTest.class.getPackage().getName());

	@Autowired
	private ApplicationContext context;

	@Resource(name = "template")
	private AmqpTemplate template;

	@Autowired
	@Qualifier("request")
	private PublishSubscribeChannel requestChannel;

	private Message constructMessage() {
		return MessageBuilder
				.withBody("{}".getBytes(Charsets.UTF_8))
				.andProperties(new MessageProperties())
				.build();
	}

	private Matcher<org.springframework.messaging.Message<?>> receivedFrom(String exchange, String routingKey) {
		Matcher<org.springframework.messaging.Message<?>> routingKeyTest;
		if (routingKey == null) {
			routingKeyTest = AmqpMessageMatchers.withNoRoutingKey();
		} else {
			routingKeyTest = AmqpMessageMatchers.withRoutingKey(routingKey);
		}

		return Matchers.allOf(AmqpMessageMatchers.receivedExchange(is(exchange)), routingKeyTest);
	}

	@Test
	public void testDirect() {
		Message messageToSend = constructMessage();
		MockTestListener mockTestListener = new MockTestListener();

		requestChannel.subscribe(mockTestListener);

		template.send("amqp.direct", "foo", messageToSend);

		Awaitility.waitAtMost(Duration.TWO_SECONDS)
				.until(() -> mockTestListener.getMessages(), Matchers.hasItem(receivedFrom("amqp.direct", "foo")));

		Awaitility.waitAtMost(Duration.TWO_HUNDRED_MILLISECONDS)
				.until(() -> mockTestListener.size(), is(1));
	}

	@Test
	public void testSendFanout() {
		Message messageToSend = constructMessage();
		MockTestListener mockTestListener = new MockTestListener();

		requestChannel.subscribe(mockTestListener);

		template.send("amqp.fanout", null, messageToSend);

		Awaitility.waitAtMost(Duration.TWO_SECONDS)
				.until(() -> mockTestListener.getMessages(), Matchers.hasItem(receivedFrom("amqp.fanout", null)));

		Awaitility.waitAtMost(Duration.TWO_HUNDRED_MILLISECONDS)
				.until(() -> mockTestListener.size(), is(2));
	}

	@Test
	public void testSetup() {
	}


	public static class MockTestListener implements MessageHandler {
		private final List<org.springframework.messaging.Message<?>> messages = new ArrayList<>();

		@Override
		public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
			messages.add(message);

			LOG.debug("message received: {} [{}]", message.getHeaders().getId(), messages.size());
		}

		public int size() {
			return messages.size();
		}

		public List<org.springframework.messaging.Message<?>> getMessages() {
			return messages;
		}

		public void reset() {
			messages.clear();
		}
	}
}
