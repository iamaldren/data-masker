package io.github.iamaldren.aspects;

import io.github.iamaldren.annotations.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TimerMetricsProcessor {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(methodTimer)")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint, Timer methodTimer) throws Throwable {
        io.micrometer.core.instrument.Timer timer = meterRegistry.timer(methodTimer.name());

        Instant start = Instant.now();

        Object proceed = joinPoint.proceed();

        Instant end = Instant.now();

        timer.record(Duration.between(start, end));

        String name = joinPoint.getSignature().getName();
        log.debug("Total execution time for {} is {}ms", name, timer.totalTime(TimeUnit.MILLISECONDS));

        return proceed;
    }

}
