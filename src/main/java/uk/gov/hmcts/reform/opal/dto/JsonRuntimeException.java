package uk.gov.hmcts.reform.opal.dto;

import com.fasterxml.jackson.core.JsonProcessingException;

public class JsonRuntimeException extends RuntimeException {
    public JsonRuntimeException(JsonProcessingException e) {
        super(e);
    }
}
