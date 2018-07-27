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

import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

/**
 * Ein HTTP Span ist ein Zeitschlitz, in welchen ein Remote-Aufruf statt gefunden hat.
 *
 * Das aufgerufene System kann optional ein Trace-Log als HTTP Header zurück liefern.
 * Dieses Log wird in den Span eingebunden. Auf diese Weise können wir ein verteiltes
 * Tracing bauen.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class HTTPSpan extends Span {

    private HttpMethod requestMethod;
    private URI requestURI;
    private HttpStatus responseStatus;
    private TraceLog nestedTraceLog;

    public HTTPSpan(String aName, long aStartTime, long aDuration, HttpMethod aRequestMethod, URI aRequestURI,
            HttpStatus aResponseStatus, TraceLog aNestedTraceLog) {
        super(aName, aStartTime, aDuration);
        requestMethod = aRequestMethod;
        requestURI = aRequestURI;
        responseStatus = aResponseStatus;
        nestedTraceLog = aNestedTraceLog;
    }
}
