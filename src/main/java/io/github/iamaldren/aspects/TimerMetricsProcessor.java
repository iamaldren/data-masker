package io.github.iamaldren.aspects;

import io.github.iamaldren.annotations.Time;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class TimerMetricsProcessor {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(methodTime)")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint, Time methodTime) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        final boolean stopWhenCompleted = CompletionStage.class.isAssignableFrom(method.getReturnType());

        if(methodTime.longTask()) {
            return executeLongTimer(joinPoint, methodTime, stopWhenCompleted);
        }

        return executeTimer(joinPoint, methodTime, stopWhenCompleted);
    }

    private Object executeTimer(ProceedingJoinPoint joinPoint, Time methodTime, boolean stopWhenCompleted) throws Throwable {
        Timer.Sample timer = Timer.start(meterRegistry);

        if(stopWhenCompleted) {
            return ((CompletionStage<?>) joinPoint.proceed())
                    .whenComplete((result, throwable) -> stopTimer(timer, methodTime));
        }

        try {
            return joinPoint.proceed();
        } finally {
            stopTimer(timer, methodTime);
        }
    }

    private void stopTimer(Timer.Sample timer, Time methodTime) {
        long duration = stopSampleTimer(timer, methodTime);
        log.debug("Total execution time for {} is {}ms", methodTime.name(), TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS));
    }

    private long stopSampleTimer(Timer.Sample timer, Time methodTime) {
        try {
            Timer.Builder builder = Timer
                    .builder(methodTime.name())
                    .tags(methodTime.tags());

            if(methodTime.publishPercentiles()) {
                builder.publishPercentileHistogram(methodTime.publishPercentiles());
                builder.publishPercentiles(methodTime.percentiles());
            }

            return timer.stop(builder.register(meterRegistry));
        } catch (Exception e) {
            log.warn("Error stopping timer", e);
        }

        return 0;
    }

    private Object executeLongTimer(ProceedingJoinPoint joinPoint, Time methodTime, boolean stopWhenCompleted) throws Throwable {
        Optional<LongTaskTimer.Sample> longTaskTimer = buildLongTaskTimer(methodTime).map(LongTaskTimer::start);

        if(stopWhenCompleted) {
            return ((CompletionStage<?>) joinPoint.proceed())
                    .whenComplete((result, throwable) -> longTaskTimer.ifPresent(sample -> {
                        stopLongTaskTimer(sample, methodTime);
                    }));
        }

        try {
            return joinPoint.proceed();
        } finally {
            longTaskTimer.ifPresent(sample -> {
                stopLongTaskTimer(sample, methodTime);
            });
        }
    }

    private void stopLongTaskTimer(LongTaskTimer.Sample sample, Time methodTime) {
        try {
            sample.stop();
            log.debug("Total execution time for {} is {}ms", methodTime.name(), sample.duration(TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            log.warn("Error stopping long task timer", e);
        }
    }

    private Optional<LongTaskTimer> buildLongTaskTimer(Time methodTime) {
        try {
            return Optional.of(LongTaskTimer
                    .builder(methodTime.name())
                    .tags(methodTime.tags())
                    .register(meterRegistry));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
