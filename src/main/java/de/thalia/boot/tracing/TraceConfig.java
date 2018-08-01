/*
 * (c) Copyright 2018 Thalia Bücher GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.thalia.boot.tracing;

import javax.annotation.PostConstruct;

import de.thalia.boot.tracing.hystrix.HystrixRequestContextFilter;
import de.thalia.boot.tracing.rest.TraceRestTemplateCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.strategy.HystrixPlugins;

import de.thalia.boot.tracing.database.DatasourceWrappingBeanPostProcessor;
import lombok.AllArgsConstructor;

import java.net.InetAddress;
import java.net.UnknownHostException;

@ConditionalOnWebApplication
@Configuration
public class TraceConfig {

    @Bean
    public Tracer tracer() {
        return new Tracer();
    }

    @Bean
    public TraceRestTemplateCustomizer traceRestTemplateCustomizer(final Tracer aTracer) {
        return new TraceRestTemplateCustomizer(aTracer);
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricExporter metricsExporter() {
        return new DefaultMetricExporter();
    }

    @Bean
    public TraceOutputFilter traceOutputFilter(@Value("${spring.application.name}") final String applicationName, final Tracer aTracer,
            final MetricExporter aExporter) throws UnknownHostException {
        return new TraceOutputFilter(aTracer, aExporter, applicationName, InetAddress.getLocalHost().getHostName());
    }

    @ConditionalOnClass(Hystrix.class)
    @AllArgsConstructor
    @Configuration
    static class TracingHystrixConfiguration {

        private final AutowireCapableBeanFactory beanFactory;

        @Bean
        public HystrixRequestContextFilter hystrixRequestContextFilter() {
            return new HystrixRequestContextFilter();
        }

        @PostConstruct
        public void afterPropertiesSet() {
            // Das ist an dieser Stelle etwas ungewöhnlich, aber!
            // Das Hystrix Plugin läd den CommandExecutionHook via ServiceLoader API
            // Hier gibt es eine Spring DI. Der Hook muss aber trotzdem auf den Tracer
            // zugreifen. Das passiert dann hier manuell
            beanFactory.autowireBean(HystrixPlugins.getInstance().getCommandExecutionHook());
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "tracing.database.enabled", havingValue = "true")
    static class DatasourceTracingInitializer {

        @Bean
        public DatasourceWrappingBeanPostProcessor erzeugeDatasourceWrappingBeanPostProcessor(final Tracer tracer) {
            return new DatasourceWrappingBeanPostProcessor(tracer);
        }
    }
}
