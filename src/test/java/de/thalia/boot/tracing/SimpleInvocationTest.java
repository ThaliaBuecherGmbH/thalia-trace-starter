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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

@RunWith(SpringRunner.class)
@WebMvcTest(value = SimpleInvocationTest.SimpleResource.class)
@ContextConfiguration(classes = {TraceConfig.class, SimpleInvocationTest.Config.class })
public class SimpleInvocationTest {

    @Configuration
    public static class Config {

        @Bean
        public RestTemplateBuilder restTemplateBuilder(RestTemplateCustomizer... customizer) {
            return new RestTemplateBuilder(customizer);
        }
    }

    @Controller
    public static class SimpleResource {

        private final RestTemplate restTemplate;

        public SimpleResource(RestTemplateBuilder builder) {
            restTemplate = builder.build();
        }

        @GetMapping(value = "/api/dosomething")
        public ResponseEntity<String> doSomething() throws InterruptedException {
            Thread.sleep(100);
            return ResponseEntity.ok().build();
        }

        @GetMapping(value = "/api/dosomethinghystrix")
        public ResponseEntity<String> doSomethingHystrix() throws InterruptedException, ExecutionException {
            new HystrixCommand<String>(HystrixCommandGroupKey.Factory.asKey("Example")) {
                @Override
                protected String run() throws Exception {
                    Thread.sleep(100);
                    return "Value";
                }
            }.queue().get();

            return ResponseEntity.ok().build();
        }

        @GetMapping(value = "/api/dosomethingrest")
        public ResponseEntity<String> doSomethingrest() throws InterruptedException, URISyntaxException {
            restTemplate.getForObject(new URI("http://localhost"), String.class);
            return ResponseEntity.ok().build();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SimpleResource simpleResource;

    private MockRestServiceServer restServiceServer;

    @Before
    public void setUp() {
        restServiceServer = MockRestServiceServer.createServer(simpleResource.restTemplate);
    }

    @Test
    public void testWithoutFeatureToggle() throws Exception {
        mvc.perform(get("/api/dosomething"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(header().doesNotExist("thaliatrace"))
            .andExpect(header().doesNotExist("sever-timing")).andReturn();
    }

    @Test
    public void testWithFeatureToggle() throws Exception {

        MvcResult result = mvc.perform(get("/api/dosomething").header("THALIATRACE","true"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().exists("thaliatrace"))
                .andReturn();

        assertEquals(1, result.getResponse().getHeaders("server-timing").size());

        TraceLog log = TraceLog.fromJSON(result.getResponse().getHeader("thaliatrace"));
        assertEquals("test", log.getApplicationName());
        assertNotNull(log.getHostName());
        assertTrue(log.getDuration() >= 100);
        assertNull(log.getSpans());
    }

    @Test
    public void testHystrix() throws Exception {

        MvcResult result = mvc.perform(get("/api/dosomethinghystrix").header("THALIATRACE","true"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().exists("thaliatrace"))
                .andReturn();

        assertEquals(2, result.getResponse().getHeaders("server-timing").size());

        TraceLog log = TraceLog.fromJSON(result.getResponse().getHeader("thaliatrace"));
        assertEquals("test", log.getApplicationName());
        assertNotNull(log.getHostName());

        List<Span> spans = log.getSpans();
        assertEquals(1, spans.size());
        Span singleSpan = spans.get(0);

        assertEquals("Example#SimpleInvocationTest$SimpleResource$1", singleSpan.getName());
        assertTrue(singleSpan.getDuration() >= 0);
        assertTrue(singleSpan.getStartTime() >= log.getStartTime());
    }

    @Test
    public void testRest() throws Exception {

        restServiceServer.expect(MockRestRequestMatchers.requestTo("http://localhost"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess("ok", MediaType.TEXT_PLAIN));

        MvcResult result = mvc.perform(get("/api/dosomethingrest").header("THALIATRACE","true"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().exists("thaliatrace"))
                .andReturn();

        restServiceServer.verify();

        assertEquals(2, result.getResponse().getHeaders("server-timing").size());

        TraceLog log = TraceLog.fromJSON(result.getResponse().getHeader("thaliatrace"));
        assertEquals("test", log.getApplicationName());
        assertNotNull(log.getHostName());

        List<Span> spans = log.getSpans();
        assertEquals(1, spans.size());
        Span singleSpan = spans.get(0);

        assertEquals("SimpleInvocationTest$SimpleResource-RestTemplate", singleSpan.getName());
        assertTrue(singleSpan.getDuration() >= 0);
        assertTrue(singleSpan.getStartTime() >= log.getStartTime());
    }
}