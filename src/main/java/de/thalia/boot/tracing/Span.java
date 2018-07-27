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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

/**
 * Basisklasse für alle Zeitschlitze.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public abstract class Span {

    private String name;
    private long startTime;
    private long duration;

    public Span(String aName, long aStartTime, long aDuration) {
        name = aName;
        startTime = aStartTime;
        duration = aDuration;
    }
}
