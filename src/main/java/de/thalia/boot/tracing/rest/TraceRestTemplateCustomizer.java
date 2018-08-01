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
package de.thalia.boot.tracing.rest;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import de.thalia.boot.tracing.TraceLog;
import de.thalia.boot.tracing.Tracer;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Dieser Customizer registriert die Trace-Mechanik in den Rest-Templates.
 */
@Slf4j
public class TraceRestTemplateCustomizer implements RestTemplateCustomizer {

    private static final String SPAN_SUFFIX = "-RestTemplate";

    private static class TraceInterceptor implements ClientHttpRequestInterceptor {

        private final String beanName;
        private final Tracer tracer;

        public TraceInterceptor(String aBeanName, Tracer aTracer) {
            beanName = aBeanName;
            tracer = aTracer;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest aRequest, byte[] aBytes, ClientHttpRequestExecution aExecution)
                throws IOException {
            long theStartTime = System.currentTimeMillis();
            HttpMethod theRequestMethod = aRequest.getMethod();
            URI theURI = aRequest.getURI();
            HttpStatus theResponseStatus = null;
            TraceLog theTraceLog = null;
            try {
                // Das Feature-Toggle wird weiter propagiert
                tracer.propagateFeatureToggle(aRequest);

                // Und weiter mit dem Aufruf
                ClientHttpResponse theResponse = aExecution.execute(aRequest, aBytes);
                theResponseStatus = theResponse.getStatusCode();
                String theTraceLogAsStr = theResponse.getHeaders().getFirst(Tracer.TRACE_ATTRIBUTE_NAME);
                if (!StringUtils.isEmpty(theTraceLogAsStr)) {
                    try {
                        theTraceLog = TraceLog.fromJSON(theTraceLogAsStr);
                    } catch (Exception e) {
                        log.warn(
                                "Konnte {} nicht als Tracelog deserialisieren. Die Verarbeitung kann jedoch fortgesetzt werden.",
                                theTraceLogAsStr, e);
                    }
                }
                return theResponse;
            } finally {
                long theDuration = System.currentTimeMillis() - theStartTime;

                tracer.addToLog(new HTTPSpan(beanName, theStartTime, theDuration,
                        theRequestMethod, theURI, theResponseStatus, theTraceLog));
            }
        }
    }

    private final Tracer tracer;

    public TraceRestTemplateCustomizer(Tracer aTracer) {
        tracer = aTracer;
    }

    @Override
    public void customize(RestTemplate aRestTemplate) {
        // Dieser Fall tritt auf, wenn ein Rest-Template direkt von
        // einem RestTemplateBuilder bezogen wird, das Template an sich aber
        // nicht via Spring gemanaged wird. In diesem Fall ist die Bean anonym, weshalb
        // wir den Bean-Namen auch nicht aus dem Spring Kontext ermitteln können.

        // Wir versuchen deshalb, den Namen aus dem Stack-Trace zu erraten
        String theGuessedName = "unknown";
        StackTraceElement[] theCurrentTrace = Thread.currentThread().getStackTrace();
        for (int i=0; i<= theCurrentTrace.length; i++) {
            StackTraceElement theElement = theCurrentTrace[i];
            String theDefiningClass = theElement.getClassName();
            int p = theDefiningClass.lastIndexOf(".");
            String thePackageName = theDefiningClass.substring(0, p);
            String theClassName = theDefiningClass.substring(p + 1);
            if (thePackageName.startsWith("de.") && (!theDefiningClass.equals(TraceRestTemplateCustomizer.class.getName()))) {
                theGuessedName = theClassName;
                break;
            }
        }
        aRestTemplate.getInterceptors().add(new TraceInterceptor(theGuessedName + SPAN_SUFFIX, tracer));
    }

    @EventListener
    public void applicationReady(ApplicationReadyEvent aEvent) {
        ApplicationContext theContext = aEvent.getApplicationContext();
        for (Map.Entry<String, RestTemplate> theEntry : BeanFactoryUtils.beansOfTypeIncludingAncestors(theContext, RestTemplate.class).entrySet()) {
            RestTemplate theTemplate = theEntry.getValue();
            boolean hasInterceptors = false;
            for (ClientHttpRequestInterceptor theInterceptor : theTemplate.getInterceptors()) {
                if (theInterceptor instanceof TraceInterceptor) {
                    hasInterceptors = true;
                }
            }
            if (!hasInterceptors) {
                theTemplate.getInterceptors().add(new TraceInterceptor(theEntry.getKey() + SPAN_SUFFIX, tracer));
            }
        }
    }
}