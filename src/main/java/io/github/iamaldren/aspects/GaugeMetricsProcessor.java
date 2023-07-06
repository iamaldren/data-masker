package io.github.iamaldren.aspects;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class GaugeMetricsProcessor {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(methodGauge)")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint, Gauge methodGauge) throws Throwable {

        return null;
    }

}
