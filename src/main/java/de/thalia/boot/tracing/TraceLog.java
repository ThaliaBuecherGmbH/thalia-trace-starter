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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Das Tracelog ist die Sammlung aller Zeitschlitze, welche bei einem Aufruf des
 * Systems ermittelt wurden. Zusätzlich werden in das Tracelog noch Informationen
 * über die aufgerufene Applikation und deren Version eingebunden.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TraceLog {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static TraceLog fromJSON(String aJSON) throws IOException {
        return MAPPER.readValue(aJSON, TraceLog.class);
    }

    private String applicationName;
    private String hostName;
    private long startTime;
    private long duration;
    private List<Span> spans;

    public TraceLog(String aApplicationName, String aHostname, long aStartTime, long aDuration,
            List<Span> aSpans) {
        applicationName = aApplicationName;
        hostName = aHostname;
        startTime = aStartTime;
        duration = aDuration;
        spans = aSpans;
    }

    public String toJSON() throws JsonProcessingException {
        return MAPPER.writeValueAsString(this);
    }
}
