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

import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.util.ContentCachingResponseWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Der TraceOutputFilter dient als Bootstrap für die Tracing-Mechanik.
 *
 * Für jeden Request werden die Trace-Informationen gesammelt und via HTTP Header in Form eines JSON zurück gegeben.
 *
 * @author Mirko Sertic
 * @since 09.02.2018
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TraceOutputFilter implements Filter {

    private static final String SERVER_TIMING_HEADER = "Server-Timing";

    private final Tracer tracer;
    private final MetricExporter metricExporter;
    private final String applicationName;
    private final String hostName;

    public TraceOutputFilter(final Tracer aTracer,
            final MetricExporter aMetricExporter, String aApplicationName, String aHostname) {
        tracer = aTracer;
        metricExporter = aMetricExporter;
        applicationName = aApplicationName;
        hostName = aHostname;
    }

    @Override
    public void init(final FilterConfig aFilterConfig) {
        log.info("Trace-Filter initialisiert");
    }

    @Override
    public void doFilter(final ServletRequest aRequest, final ServletResponse aResponse, final FilterChain aChain)
            throws IOException, ServletException {
        final HttpServletRequest theRequest = (HttpServletRequest) aRequest;
        // Wir müssen nur REQUESTS tracen, und keine INCLUDES oder FORWARDS
        if (DispatcherType.REQUEST == aRequest.getDispatcherType()) {

            // Feature Toggle Output aktiv?
            final boolean theOutputAktiv = tracer.registerFeatureToggleStatusFrom(theRequest);

            // Voller Trace-Lauf
            final long theStart = System.currentTimeMillis();
            final HttpServletResponse theResponse = (HttpServletResponse) aResponse;
            final ContentCachingResponseWrapper theResponseWrapper = new ContentCachingResponseWrapper(theResponse);
            try {
                aChain.doFilter(theRequest, theResponseWrapper);
            } finally {

                final String theInvokedPattern = (String) aRequest
                        .getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern");

                final long theDuration = System.currentTimeMillis() - theStart;
                final TraceLog theLog = new TraceLog(applicationName,
                        hostName, theStart, theDuration, tracer.collectedSpansFor(aRequest));

                // Das Tracing ist immer aktiv.
                // Nur wenn jedoch das Toggle-Flag gesetzt ist, wird auch eine
                // Ausgabe via HTTP Response generiert
                if (theOutputAktiv) {
                    theResponseWrapper.addHeader(Tracer.TRACE_HEADER_NAME, theLog.toJSON());
                    // https://w3c.github.io/server-timing/#introduction
                    writeServerTimingHeader(theLog, theResponseWrapper);
                }

                if (theInvokedPattern != null) {
                    metricExporter.exportMetricsFor(theLog, theInvokedPattern);
                }

                theResponseWrapper.copyBodyToResponse();
            }
        } else {
            // Passiert z.B. bei einem Forward in der Handler-Chain
            // Forwards sind Teil eines REQUESTS und werden somit
            // indirekt mit überwacht- Hier müssen wir kein Wrapping usw.
            // betreiben
            aChain.doFilter(theRequest, aResponse);
        }
    }

    /**
     * Erzeugt einen {@code Server-Timing}-Header mit den Informationen zu den Laufzeiten der einzelnen Verarbeitungsschritte, der
     * z.B. von Google Chrome in den Dev-Tools graphisch dargestellt werden kann.
     * 
     * @param traceLog
     *            das Trace-Log des aktuellen Aufrufs
     * @param response
     *            die aktuelle Response
     */
    private static void writeServerTimingHeader(final TraceLog traceLog, final HttpServletResponse response) {
        response.addHeader(SERVER_TIMING_HEADER, "total;desc=\"Total\";dur=" + traceLog.getDuration());
        if (null != traceLog.getSpans()) {
            int index = 0;
            for (final Span span : traceLog.getSpans()) {
                response.addHeader(SERVER_TIMING_HEADER, "S" + index + ";desc=\"" + span.getName() + "\";dur=" + span.getDuration());
                index++;
            }
        }
    }

    @Override
    public void destroy() {
    }
}