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
package de.thalia.boot.tracing.database;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import de.thalia.boot.tracing.Span;
import de.thalia.boot.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

/**
 * Mit diesem Listener werden die Aktionen auf der verbundenen {@link javax.sql.DataSource} aufgezeichnet.
 *
 * @author Hendrik Busch
 * @since 21.02.18
 */
@RequiredArgsConstructor
public class TracingQueryExecutionListener implements QueryExecutionListener {

    /**
     * Der Tracer, in dem die Tracing-Informationen aggregiert werden.
     */
    private final Tracer tracer;

    /**
     * Die Startzeit der Datenbankoperation.
     */
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    /**
     * Hält die Startzeit der Datenbankoperation fest.
     * 
     * @param executionInfo
     *            Informationen zur Ausführung der Datenbankoperation
     * @param list
     *            Liste von auszuführenden Queries
     */
    @Override
    public void beforeQuery(final ExecutionInfo executionInfo, final List<QueryInfo> list) {
        startTime.set(System.currentTimeMillis());
    }

    /**
     * Ermittelt nach Abschluss der Datenbankoperation die nötigen Kennzahlen und erzeugt daraus ein {@link QuerySpan}-Objekt, das
     * anschliessend im {@link #tracer} aggregiert wird.
     * 
     * @param executionInfo
     *            Informationen zur Ausführung der Datenbankoperation
     * @param list
     *            Liste von auszuführenden Queries
     */
    @Override
    public void afterQuery(final ExecutionInfo executionInfo, final List<QueryInfo> list) {
        final QuerySpan span = new QuerySpan(executionInfo.getDataSourceName(), startTime.get(), executionInfo.getElapsedTime(),
                list.size(), executionInfo.getDataSourceName());

        final Optional<List<Span>> requestSpans = tracer.getCollectedSpansForCurrentRequest();
        final QuerySpan mergeSpan = findeKandidatenFuerMerge(requestSpans.orElse(Collections.emptyList()),
                executionInfo.getDataSourceName());

        if (null != mergeSpan) {
            final QuerySpan mergedSpan = new QuerySpan(executionInfo.getDataSourceName(), mergeSpan.getStartTime(),
                    mergeSpan.getDuration() + span.getDuration(), mergeSpan.getAnzahlQueries() + span.getAnzahlQueries(),
                    mergeSpan.getDatasourceName());
            tracer.replaceLatestSpan(mergedSpan);
        } else {
            tracer.addToLog(span);
        }
    }

    /**
     * Prüft ob das letzte verfügbare Element in den übergebenen Spans ein QuerySpan der aktuellen Datenquelle ist. Falls ja, wird
     * es zurückgegeben und mit den neu aufgezeichneten Daten zusammengeführt.
     * 
     * @param spans
     *            die Spans, deren letztes Element überprüft werden soll
     * @param dataSourceName
     *            der Name der Datenquelle, auf deren Übereinstimmung geprüft werden soll
     * @return falls verfügbar, das passende {@link QuerySpan}-Element, ansonste {@code null}
     */
    static QuerySpan findeKandidatenFuerMerge(final List<Span> spans, final String dataSourceName) {
        if (null == spans || spans.isEmpty()) {
            return null;
        }

        final Span letzterSpan = spans.get(spans.size() - 1);
        return letzterSpan instanceof QuerySpan && dataSourceName.equals(((QuerySpan) letzterSpan).getDatasourceName())
                ? (QuerySpan) letzterSpan
                : null;
    }
}