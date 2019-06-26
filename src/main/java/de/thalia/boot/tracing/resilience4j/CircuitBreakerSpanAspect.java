package de.thalia.boot.tracing.resilience4j;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;

import de.thalia.boot.tracing.Tracer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Slf4j
@RequiredArgsConstructor
public class CircuitBreakerSpanAspect implements Ordered {

    private final Tracer tracer;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Around(value = "@annotation(io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker)")
    public Object handle(final ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        final long startTime = System.currentTimeMillis();
        log.debug("Started at {}", startTime);
        final Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        final String spanName = method.getAnnotation(CircuitBreaker.class).name();
        try {
            return proceedingJoinPoint.proceed();
        } finally {
            final long duration = System.currentTimeMillis() - startTime;
            log.debug("Finished, duration = {}", duration);
            if (tracer != null) {
                tracer.addToLog(new CircuitBreakerSpan(spanName, startTime, duration));
            }
        }
    }
}
