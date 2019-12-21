/*
 * (c) Copyright 2019 Thalia Bücher GmbH
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
package org.thalia.boot.tracing.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import de.thalia.boot.tracing.rest.TraceRestTemplateCustomizer;

/**
 * Tests für {@link TraceRestTemplateCustomizer}
 */
public class TraceRestTemplateCustomizerTest {

    @Test
    public void customize_arrayOutOfBound() {
        final TraceRestTemplateCustomizer templateCustomizer = new TraceRestTemplateCustomizer(null);
        final RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getInterceptors()).thenReturn(new ArrayList<>());

        templateCustomizer.customize(restTemplate);
    }

}