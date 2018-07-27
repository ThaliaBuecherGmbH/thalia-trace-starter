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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import de.thalia.boot.tracing.Span;
import lombok.Getter;

/**
 * Definiert ein Objekt, dass die Tracing-Daten für einen Datenbankaufruf enthält.
 *
 * @author Hendrik Busch
 * @since 21.02.18
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class QuerySpan extends Span {

    /**
     * Die Gesamtanzahl der SQL-Quries, die im Rahmen der Datenbankaktion ausgeführt wurden. Überlicherweise ist das genau eine,
     * außer bei Batch-Statements.
     */
    private final int anzahlQueries;

    /**
     * Der (Bean-)Name der Datasource, mit der die Aktion ausgeführt wurde.
     */
    private final String datasourceName;

    /**
     * Erzeugt eine neue Instanz und konfiguriert sie mit den übergebenen Werten.
     * 
     * @param name
     *            siehe Feldbeschreibung
     * @param startTime
     *            siehe Feldbeschreibung
     * @param duration
     *            siehe Feldbeschreibung
     * @param anzahlQueries
     *            siehe Feldbeschreibung
     * @param datasourceName
     *            siehe Feldbeschreibung
     */
    public QuerySpan(final String name, final long startTime, final long duration, final int anzahlQueries,
            final String datasourceName) {
        super(name, startTime, duration);
        this.anzahlQueries = anzahlQueries;
        this.datasourceName = datasourceName;
    }
}
