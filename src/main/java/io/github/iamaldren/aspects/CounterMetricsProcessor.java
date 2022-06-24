package io.github.iamaldren.aspects;

import io.github.iamaldren.annotations.Count;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class CounterMetricsProcessor {

    public final String DEFAULT_EXCEPTION_TAG_VALUE = "none";
    public final String RESULT_TAG_FAILURE_VALUE = "failure";
    public final String RESULT_TAG_SUCCESS_VALUE = "success";
    public final String DEFAULT_ERROR_CODE_TAG_VALUE = "none";

    private static final String RESULT_TAG = "result";
    private static final String EXCEPTION_TAG = "exception";
    private static final String ERROR_CODE_TAG = "error.code";

    private final MeterRegistry meterRegistry;

    @Around("@annotation(methodCount)")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint, Count methodCount) throws Throwable {
        final Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        final boolean stopWhenCompleted = CompletionStage.class.isAssignableFrom(method.getReturnType());

        if (stopWhenCompleted) {
            try {
                return ((CompletionStage<?>) joinPoint.proceed())
                        .whenComplete((result, throwable) -> recordCompletionResult(methodCount, throwable));
            } catch (Throwable e) {
                String errorCode = DEFAULT_ERROR_CODE_TAG_VALUE;
                try {
                    errorCode = (String) e.getClass().getDeclaredField("errorCode").get(e);
                } catch (NoSuchFieldException | IllegalAccessException e1) {
                    log.warn("No such errorCode field from class");
                    //ignore
                }
                record(methodCount, e.getClass().getSimpleName(), RESULT_TAG_FAILURE_VALUE, errorCode);
                throw e;
            }
        }

        try {
            Object result = joinPoint.proceed();
            if (!methodCount.countFailuresOnly()) {
                record(methodCount, DEFAULT_EXCEPTION_TAG_VALUE, RESULT_TAG_SUCCESS_VALUE, DEFAULT_ERROR_CODE_TAG_VALUE);
            }

            return result;
        } catch (Throwable e) {
            String errorCode = DEFAULT_ERROR_CODE_TAG_VALUE;
            try {
                errorCode = (String) e.getClass().getDeclaredField("errorCode").get(e);
            } catch (NoSuchFieldException | IllegalAccessException e1) {
                log.warn("No such errorCode field from class");
            }
            record(methodCount, e.getClass().getSimpleName(), RESULT_TAG_FAILURE_VALUE, errorCode);
            throw e;
        }
    }

    private void recordCompletionResult(Count methodCount, Throwable throwable) {

        if (throwable != null) {
            String errorCode = DEFAULT_ERROR_CODE_TAG_VALUE;
            try {
                errorCode = (String) throwable.getClass().getDeclaredField("errorCode").get(throwable);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                log.warn("No such errorCode field from class");
                //ignore
            }

            String exceptionTagValue = throwable.getCause() == null ? throwable.getClass().getSimpleName()
                    : throwable.getCause().getClass().getSimpleName();
            record(methodCount, exceptionTagValue, RESULT_TAG_FAILURE_VALUE, errorCode);
        }
        else if (!methodCount.countFailuresOnly()) {
            record(methodCount, DEFAULT_EXCEPTION_TAG_VALUE, RESULT_TAG_SUCCESS_VALUE, DEFAULT_ERROR_CODE_TAG_VALUE);
        }

    }

    private void record(Count methodCount, String exception, String result, String errorCode) {
        counter(methodCount)
                .tag(EXCEPTION_TAG, exception)
                .tag(RESULT_TAG, result)
                .tag(ERROR_CODE_TAG, errorCode)
                .tags(methodCount.tags())
                .register(meterRegistry).increment();
    }

    private Counter.Builder counter(Count methodCount) {
        return Counter.builder(methodCount.name());
    }

}
