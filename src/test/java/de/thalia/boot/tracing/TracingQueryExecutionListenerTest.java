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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import de.thalia.boot.tracing.database.QuerySpan;
import de.thalia.boot.tracing.database.TracingQueryExecutionListener;
import de.thalia.boot.tracing.rest.HTTPSpan;

/**
 * Testklasse für den {@link TracingQueryExecutionListener}. Das eigentliche Tracing wird in {@link DatabaseTracingTest} getestet.
 *
 * @author Hendrik Busch
 * @since 02.03.18
 */
public class TracingQueryExecutionListenerTest {

    @Test
    public void findeKandidatenFuerMerge() {
        final List<Span> spanList = new ArrayList<>(10);
        assertThat(TracingQueryExecutionListener.findeKandidatenFuerMerge(spanList, "testDataSource")).isNull();

        spanList.add(new HTTPSpan("name", 10, 20, HttpMethod.GET, URI.create("http://www.example.com"), HttpStatus.OK, null));
        assertThat(TracingQueryExecutionListener.findeKandidatenFuerMerge(spanList, "testDataSource")).isNull();

        final QuerySpan querySpan1 = new QuerySpan("testDataSource", 10, 20, 2, "testDataSource");
        spanList.add(querySpan1);
        assertThat(TracingQueryExecutionListener.findeKandidatenFuerMerge(spanList, "testDataSource")).isSameAs(querySpan1);

        spanList.add(new QuerySpan("anotherDataSource", 10, 20, 2, "anotherDataSource"));
        assertThat(TracingQueryExecutionListener.findeKandidatenFuerMerge(spanList, "testDataSource")).isNull();

        final QuerySpan querySpan2 = new QuerySpan("testDataSource", 50, 55, 15, "testDataSource");
        spanList.add(querySpan2);
        assertThat(TracingQueryExecutionListener.findeKandidatenFuerMerge(spanList, "testDataSource")).isSameAs(querySpan2);
    }
}