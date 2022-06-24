package io.github.iamaldren.exceptions;

import lombok.Getter;

@Getter
public class MetricHandlerException extends Exception {

    private String errorCode;
    private String errorMessage;

    public MetricHandlerException(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

}
