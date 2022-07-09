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

    @Around("@within(io.github.iamaldren.annotations.Time)")
    public Object timeClassLevel(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Executing class level time metric process");

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> clazz = method.getDeclaringClass();
        final boolean stopWhenCompleted = CompletionStage.class.isAssignableFrom(method.getReturnType());

        Time methodTime = clazz.getAnnotation(Time.class);

        if(methodTime.longTask()) {
            return executeLongTimer(joinPoint, methodTime, stopWhenCompleted);
        }

        return executeTimer(joinPoint, methodTime, stopWhenCompleted);
    }

    @Around("execution (@io.github.iamaldren.annotations.Time * *.*(..))")
    public Object timeTypeLevel(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Executing type level time metric process");

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> clazz = method.getDeclaringClass();
        final boolean stopWhenCompleted = CompletionStage.class.isAssignableFrom(method.getReturnType());

        Time methodTime = clazz.getAnnotation(Time.class);

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
        log(duration, methodTime);
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
            long duration = sample.stop();
            log(duration, methodTime);
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

    private void log(long duration, Time methodTime) {
        if(methodTime.enableCustomLogging()) {
            log.info(methodTime.loggingMessage(), methodTime.timerUnit().convert(duration, TimeUnit.NANOSECONDS));
        } else {
            log.info("Total execution time for {} is {}ms", methodTime.name(), TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS));
        }
    }

}
