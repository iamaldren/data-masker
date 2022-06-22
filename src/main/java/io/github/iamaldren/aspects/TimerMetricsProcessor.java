package io.github.iamaldren.aspects;

import io.github.iamaldren.annotations.Timer;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TimerMetricsProcessor {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(methodTimer)")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint, Timer methodTimer) throws Throwable {
        if(methodTimer.longTask()) {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            final boolean stopWhenComplete = CompletionStage.class.isAssignableFrom(method.getReturnType());
            return executeLongTimer(joinPoint, methodTimer, stopWhenComplete);
        }

        return executeTimer(joinPoint, methodTimer);
    }

    private Object executeTimer(ProceedingJoinPoint joinPoint, Timer methodTimer) throws Throwable {
        Instant start = Instant.now();

        Object proceed = joinPoint.proceed();

        Instant end = Instant.now();

        io.micrometer.core.instrument.Timer.Builder builder = io.micrometer.core.instrument.Timer
                .builder(methodTimer.name())
                .tags(methodTimer.tags());

        if(methodTimer.publishPercentiles()) {
            builder.publishPercentileHistogram(methodTimer.publishPercentiles());
            builder.publishPercentiles(methodTimer.percentiles());
        }

        io.micrometer.core.instrument.Timer timer = builder.register(meterRegistry);
        timer.record(Duration.between(start, end));
        log.debug("Total execution time for {} is {}ms", methodTimer.name(), timer.totalTime(TimeUnit.MILLISECONDS));

        return proceed;
    }

    private Object executeLongTimer(ProceedingJoinPoint joinPoint, Timer methodTimer, boolean stopWhenComplete) throws Throwable {
        Instant start = Instant.now();

        Object proceed = joinPoint.proceed();

        Instant end = Instant.now();

        Optional<LongTaskTimer> longTaskTimer = buildLongTaskTimer(joinPoint, methodTimer);

        if(longTaskTimer.isPresent()) {
            longTaskTimer.get().start();
        }

        return null;
    }

    private void startLongTaskTimer(LongTaskTimer timer) {
        try {
            timer.start();
        } catch (Exception e) {
            log.warn("Error stopping long task timer", e);
        }
    }

    private void stopLongTaskTimer(LongTaskTimer.Sample sample) {
        try {
            sample.stop();
        } catch (Exception e) {
            log.warn("Error stopping long task timer", e);
        }
    }

    private Optional<LongTaskTimer> buildLongTaskTimer(ProceedingJoinPoint joinPoint, Timer methodTimer) {
        try {
            return Optional.of(LongTaskTimer
                    .builder(methodTimer.name())
                    .tags(methodTimer.tags())
                    .register(meterRegistry));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
