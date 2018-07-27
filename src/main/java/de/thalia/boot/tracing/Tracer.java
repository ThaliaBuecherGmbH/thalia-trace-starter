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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Der Tracer registriert alle Spans, welche bei der Verarbeitung eines Requests ermittelt wurden.
 */
@SuppressWarnings("unchecked")
public class Tracer {

    public static final String TRACE_ATTRIBUTE_NAME = Tracer.class.getName() + ".TRACELOG";
    public static final String TRACE_HEADER_NAME = "THALIATRACE";
    private static final String TRACE_TOGGLE_ATTRIBUTE_NAME = Tracer.class.getName() + ".TRACELOGTOGGLE";

    /**
     * Fügt den übergebenen Span in die Liste der Spans für den aktuellen Request ein, sofern ein Request bestimmbar ist.
     * 
     * @param aSpan
     *            der einzufügende Span
     */
    public synchronized void addToLog(final Span aSpan) {
        final Optional<List<Span>> aktuelleSpans = getCollectedSpansForCurrentRequest();
        aktuelleSpans.ifPresent(spanList -> spanList.add(aSpan));
    }

    public synchronized void addToLog(final HttpServletRequest aRequest, final Span aSpan) {
        List<Span> theSpans = (List<Span>) aRequest.getAttribute(TRACE_ATTRIBUTE_NAME);
        if (null == theSpans) {
            theSpans = new ArrayList<>();
            aRequest.setAttribute(TRACE_ATTRIBUTE_NAME, theSpans);
        }
        theSpans.add(aSpan);
    }

    public List<Span> collectedSpansFor(final ServletRequest aRequest) {
        return (List<Span>) aRequest.getAttribute(Tracer.TRACE_ATTRIBUTE_NAME);
    }

    /**
     * Ersetzt den letzten Span in der Liste mit dem übergebenen.
     * 
     * @param replacement
     *            der Ersatz für den letzten Span
     */
    public void replaceLatestSpan(final Span replacement) {
        final Optional<List<Span>> aktuelleSpans = getCollectedSpansForCurrentRequest();
        aktuelleSpans.ifPresent(spanList -> {
            if (!spanList.isEmpty()) {
                spanList.remove(spanList.size() - 1);
                spanList.add(replacement);
            }
        });
    }

    /**
     * Holt die Spans des aktuellen Requests. Dabei werden nur die Attribute des aktuellen Requests über den
     * {@link RequestContextHolder} bezogen und aus diesen die Spans geholt. Falls bislang keine Spans existieren, wird eine neue
     * Liste mit Spans angelegt und in den Request gepackt. Sind die Attribute des Requests nicht verfügbar, wird ein leeres
     * Optional zurückgegeben.
     * 
     * @return die Liste mit Spans oder ein leeres Optional, falls kein aktiver Request verfügbar war.
     */
    public Optional<List<Span>> getCollectedSpansForCurrentRequest() {
        final RequestAttributes theCurrentRequest = RequestContextHolder.getRequestAttributes();
        if (null != theCurrentRequest) {
            List<Span> theSpans = (List<Span>) theCurrentRequest.getAttribute(TRACE_ATTRIBUTE_NAME,
                    RequestAttributes.SCOPE_REQUEST);
            if (null == theSpans) {
                theSpans = new ArrayList<>();
                theCurrentRequest.setAttribute(TRACE_ATTRIBUTE_NAME, theSpans, RequestAttributes.SCOPE_REQUEST);
            }
            return Optional.of(theSpans);
        }
        return Optional.empty();
    }

    public boolean registerFeatureToggleStatusFrom(final HttpServletRequest aRequest) {
        if (null != aRequest.getHeader(TRACE_HEADER_NAME)) {
            aRequest.setAttribute(TRACE_TOGGLE_ATTRIBUTE_NAME, "true");
            return true;
        }
        return false;
    }

    public void propagateFeatureToggle(final HttpRequest aRequest) {
        final RequestAttributes theCurrentRequest = RequestContextHolder.getRequestAttributes();
        if (null != theCurrentRequest) {
            if (null != theCurrentRequest.getAttribute(TRACE_TOGGLE_ATTRIBUTE_NAME, RequestAttributes.SCOPE_REQUEST)) {
                aRequest.getHeaders().add(TRACE_HEADER_NAME, "true");
            }
        }
    }
}