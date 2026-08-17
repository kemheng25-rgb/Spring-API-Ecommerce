package com.example.ecommerce.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptorFactoryBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_DLQ = "notification.queue.dlq";

    /**
     * Dead-lettered via the default exchange (routing key = queue name) - notification.queue
     * isn't bound to a custom exchange, so this is the simplest way to route rejected messages
     * here without introducing one.
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
            .build();
    }

    /** Where a notification lands after exhausting retries - for manual inspection/alerting, not auto-processed. */
    @Bean
    public Queue notificationDeadLetterQueue() {
        return new Queue(NOTIFICATION_DLQ, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    /**
     * Overrides Spring Boot's default listener container factory. Without this, a throwing
     * @RabbitListener nacks-with-requeue by default - the same poison message gets redelivered
     * and rethrown forever. This retries in-process a few times with backoff, then rejects
     * without requeueing via RejectAndDontRequeueRecoverer, which (combined with
     * notificationQueue's dead-letter arguments above) routes the message to
     * notification.queue.dlq instead of looping or being silently dropped.
     * Bean name must be exactly "rabbitListenerContainerFactory" - that's the default
     * @RabbitListener looks up when no containerFactory is specified.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) throws Exception {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setContainerCustomizer(container -> container.setAdviceChain(retryInterceptor()));
        return factory;
    }

    private org.aopalliance.aop.Advice retryInterceptor() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10_000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        StatelessRetryOperationsInterceptorFactoryBean factoryBean = new StatelessRetryOperationsInterceptorFactoryBean();
        factoryBean.setRetryOperations(retryTemplate);
        factoryBean.setMessageRecoverer(new RejectAndDontRequeueRecoverer());
        try {
            return factoryBean.getObject();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build the RabbitMQ retry interceptor", ex);
        }
    }
}
