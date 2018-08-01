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

import de.thalia.boot.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;

import com.netflix.hystrix.HystrixInvokable;
import com.netflix.hystrix.HystrixInvokableInfo;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;

import lombok.extern.slf4j.Slf4j;

/**
 * Dieser {@link HystrixCommandExecutionHook} fügt eine {@link HystrixSpan} zum {@link Tracer} hinzu.
 */
@Slf4j
public class TracingHystrixCommandExecutionHook extends HystrixCommandExecutionHook {

    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Autowired
    private Tracer tracer;

    public TracingHystrixCommandExecutionHook() {
    }

    @Override
    public <T> void onExecutionStart(final HystrixInvokable<T> commandInstance) {
        startTime.set(System.currentTimeMillis());
    }

    @Override
    public <T> Exception onExecutionError(final HystrixInvokable<T> commandInstance, final Exception e) {
        addToTraceLog(commandInstance);
        return super.onExecutionError(commandInstance, e);
    }

    @Override
    public <T> void onExecutionSuccess(final HystrixInvokable<T> commandInstance) {
        addToTraceLog(commandInstance);
    }

    private <T> void addToTraceLog(final HystrixInvokable<T> commandInstance) {
        if (!HystrixRequestContext.isCurrentThreadInitialized()) {
            log.debug("HystrixRequestContext ist nicht initialisiert. Dies ist wahrscheinlich kein Aufruf aus einem WebRequest");
            return;
        }
        final String name;
        if (commandInstance instanceof HystrixInvokableInfo) {
            final HystrixInvokableInfo<?> info = (HystrixInvokableInfo<?>) commandInstance;
            name = info.getThreadPoolKey().name() + "#" + info.getCommandKey().name();
        } else {
            name = "Hystrix";
        }

        if (tracer != null) {
            tracer.addToLog(HystrixRequestHolder.getInstance().get(),
                    new HystrixSpan(name, startTime.get(), System.currentTimeMillis() - startTime.get()));
        }
    }
}