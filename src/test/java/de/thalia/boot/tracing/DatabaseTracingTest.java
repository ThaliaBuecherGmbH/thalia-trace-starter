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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.thalia.boot.tracing.database.DatasourceWrappingBeanPostProcessor;
import de.thalia.boot.tracing.database.QuerySpan;
import net.ttddyy.dsproxy.support.ProxyDataSource;

/**
 * Testklasse für das Datenbank-Tracing. Sie überprüft, ob das Wrappen der von Spring verwalteten DataSources funktioniert und ob
 * die Spans korrekt erzeugt werden.
 *
 * @author Hendrik Busch
 * @since 02.03.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class DatabaseTracingTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Tracer tracer;

    @Test
    public void testDatabaseTracing() {
        when(tracer.getCollectedSpansForCurrentRequest()).thenReturn(Optional.empty());

        assertThat(dataSource).isInstanceOf(ProxyDataSource.class);
        final JdbcTemplate template = new JdbcTemplate(dataSource);
        final List<Map<String, Object>> result = template.queryForList("SELECT * FROM serien");
        assertThat(result).hasSize(4);

        final ArgumentCaptor<Span> spanCaptor = ArgumentCaptor.forClass(Span.class);
        verify(tracer).addToLog(spanCaptor.capture());
        assertThat(spanCaptor.getAllValues()).hasSize(1);

        final Span span1 = spanCaptor.getValue();
        assertThat(span1).isInstanceOf(QuerySpan.class);
        final QuerySpan querySpan1 = (QuerySpan) span1;
        assertThat(querySpan1.getNumberQueries()).isEqualTo(1);
        assertThat(querySpan1.getDuration()).isGreaterThan(0);

        reset(tracer);
        when(tracer.getCollectedSpansForCurrentRequest()).thenReturn(Optional.of(Collections.singletonList(querySpan1)));

        template.execute("DELETE FROM serien WHERE jahr < 1990");
        verify(tracer).replaceLatestSpan(spanCaptor.capture());
        assertThat(spanCaptor.getAllValues()).hasSize(2);

        final Span span2 = spanCaptor.getValue();
        assertThat(span2).isInstanceOf(QuerySpan.class);
        final QuerySpan querySpan2 = (QuerySpan) span2;

        assertThat(querySpan2.getNumberQueries()).isEqualTo(2);
        assertThat(querySpan2.getDuration()).isGreaterThan(querySpan1.getDuration());
        assertThat(querySpan2.getStartTime()).isEqualTo(querySpan1.getStartTime());
    }

    @Configuration
    static class DatabaseConfiguration {

        @Bean
        public DataSource starteInMemoryHSQL() {
            final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            //@formatter:off
            return builder
                    .setType(EmbeddedDatabaseType.HSQL)
                    .setName("hsqltest")
                    .addScript("/database-tracing-setup.sql")
                    .build();
            //@formatter:on
        }

        @Bean
        public Tracer tracer() {
            return Mockito.mock(Tracer.class);
        }

        @Bean
        public DatasourceWrappingBeanPostProcessor erzeugeDatasourceWrappingBeanPostProcessor(final Tracer tracer) {
            return new DatasourceWrappingBeanPostProcessor(tracer);
        }
    }
}