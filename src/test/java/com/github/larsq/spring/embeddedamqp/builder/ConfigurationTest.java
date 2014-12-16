package com.github.larsq.spring.embeddedamqp.builder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

public class ConfigurationTest {
	private final Resource resourceLoader = new ClassPathResource("rabbit_localhost.json");
	private Configuration resourceUnderTest;

	@Mock
	private RabbitAdmin rabbitAdmin;

	public ConfigurationTest() {
		MockitoAnnotations.initMocks(this);
	}

	@Before
	public void readConfiguration() throws IOException {
		resourceUnderTest = Configuration.parse(resourceLoader.getInputStream());
	}

	@Test
	public void testParse_checkNoException() {
		resourceUnderTest.accept(rabbitAdmin);
	}
}