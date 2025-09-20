package co.com.pragma.model.exceptions;

import co.com.pragma.model.constants.Errors;

public class InvalidPathVariableException extends CustomException {

    public InvalidPathVariableException() {
        super(Errors.INVALID_PATH_VARIABLE, Errors.INVALID_PATH_VARIABLE_CODE);
    }
}
