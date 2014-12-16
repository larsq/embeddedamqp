package com.github.larsq.spring.embeddedamqp.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 */
public class Configuration implements Consumer<RabbitAdmin> {
	private ObjectNode root;
	private final static Logger logger = LoggerFactory.getLogger(Configuration.class.getPackage().getName());

	public Configuration(ObjectNode root) {
		this.root = root;
	}

	public static Configuration parse(InputStream inputStream) throws IOException {
		return new Configuration(readConfiguration(inputStream));
	}

	public static ObjectNode readConfiguration(InputStream in) throws IOException {
		JsonNode node = new ObjectMapper().readTree(in);

		if(!node.isObject()) {
			throw new IllegalStateException("Node is not a compound one");
		}

		return (ObjectNode) node;
	}

	private <T> T typedValue(ObjectNode object, String field, T defaultValue, Class<T> expectedType) {
		T value = Optional.ofNullable(object.get(field))
				.filter(expectedType::isInstance)
				.map(expectedType::cast)
				.orElse(defaultValue);

		return value;
	}

	protected Map<String,Object> convertToMap(ObjectNode objectNode) {
		Map<String, Object> map = new LinkedHashMap<>();


		Seq.seq(objectNode.fields()).forEach(e->{
			String key = e.getKey();
			String value = e.getValue().textValue();

			map.put(key,value);
		});

		return map;
	}

	protected boolean booleanField(ObjectNode node, String name, boolean defaultValue) {
		return Optional.ofNullable(node.get(name))
				.map(n->n.asBoolean(defaultValue))
				.orElse(defaultValue);
	}

	protected String stringField(ObjectNode node, String name) {
		return Optional.ofNullable(node.get(name))
				.map(n->n.asText())
				.orElse(null);
	}

	protected Queue parseQueue(ObjectNode object) {
		Queue queue = new Queue(
				stringField(object,"name"),
				booleanField(object,"durable", false),
				booleanField(object,"exclusive", false),
				booleanField(object,"auto_delete", false),
				convertToMap((ObjectNode) object.get("arguments")));

		logger.debug("creating queue: "+queue);

		return queue;
	}

	protected Exchange parseExchange(ObjectNode object) {
		String type = stringField(object, "type");

		Exchange exchange = null;

		switch(type) {
			case "fanout": exchange = new FanoutExchange(
					stringField(object,"name"),
					booleanField(object,"durable", false),
					booleanField(object,"auto_delete", false),
					convertToMap((ObjectNode) object.get("arguments")));
				break;


			case "headers" : exchange = new HeadersExchange(
					stringField(object,"name"),
					booleanField(object,"durable", false),
					booleanField(object,"auto_delete", false),
					convertToMap((ObjectNode) object.get("arguments")));
				break;

			default: throw new UnsupportedOperationException("type is currently not implemented:"+type);
		}

		logger.debug("creating exchange:"+exchange);

		return exchange;
	}

	protected Binding.DestinationType destinationType(String destinationType) {
		return Binding.DestinationType.valueOf(destinationType.toUpperCase());
	}

	protected Binding parseBinding(ObjectNode object) {

		Binding.DestinationType type = destinationType(stringField(object, "destination_type"));

		Binding binding = new Binding(
				stringField(object, "destination"),
				type,
				stringField(object, "source"),
				Strings.emptyToNull(stringField(object, "routing_key")),
				convertToMap((ObjectNode) object.get("arguments")));

		logger.debug("creating binding: " + binding);

		return binding;
	}


	protected List<ObjectNode> convertArray(ArrayNode array) {
		return Seq.seq(array.elements()).map(ObjectNode.class::cast).collect(Collectors.toList());
	}

	@Override
	public void accept(RabbitAdmin admin) {
		AdminContext context = new AdminContext(admin);

		context.defineQueues(convertArray(typedValue(root, "queues", null, ArrayNode.class)));
		context.defineExchange(convertArray(typedValue(root, "exchanges", null, ArrayNode.class)));
		context.defineBindings(convertArray(typedValue(root, "bindings", null, ArrayNode.class)));
	}

	protected class AdminContext  {
		private final RabbitAdmin rabbitAdmin;

		public AdminContext(RabbitAdmin rabbitAdmin) {
			this.rabbitAdmin = rabbitAdmin;
		}

		protected void defineQueues(List<ObjectNode> queues) {
			if (queues == null) {
				return;
			}

			queues.stream().map(Configuration.this::parseQueue).forEach(rabbitAdmin::declareQueue);
		}

		protected void defineExchange(List<ObjectNode> exchanges) {
			if (exchanges == null) {
				return;
			}

			exchanges.stream().map(Configuration.this::parseExchange).forEach(rabbitAdmin::declareExchange);
		}

		protected void defineBindings(List<ObjectNode> bindings) {
			if (bindings == null) {
				return;
			}

			bindings.stream().map(Configuration.this::parseBinding).forEach(rabbitAdmin::declareBinding);
		}
	}
}
