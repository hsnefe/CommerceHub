package com.commercehub.messaging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = RabbitAutoConfiguration.class)
@Import(RabbitMqTopologyConfig.class)
public class CommonMessagingAutoConfiguration {
}
