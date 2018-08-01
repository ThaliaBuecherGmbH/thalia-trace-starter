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
package de.thalia.boot.tracing.hystrix;

import javax.servlet.http.HttpServletRequest;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableDefault;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Hält den ursprünglichen {@link HttpServletRequest} in einer
 * {@link com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable}. Dadurch kann aus Hystrix heraus auf den Request
 * zugegriffen werden.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HystrixRequestHolder {

    private final HystrixRequestVariableDefault<HttpServletRequest> variable = new HystrixRequestVariableDefault<>();

    @Getter
    private static final HystrixRequestHolder instance = new HystrixRequestHolder();

    /**
     * Setzt den aktuellen Request. Delegiert zum Setter der
     * {@link com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable}
     * 
     * @param request
     *            Der zu speichernde Request
     */
    public void set(final HttpServletRequest request) {
        variable.set(request);
    }

    /**
     * Liefert den aktuellen Request aus der {@link com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable}
     * 
     * @return Der ursprüngliche Request
     */
    public HttpServletRequest get() {
        return variable.get();
    }
}
