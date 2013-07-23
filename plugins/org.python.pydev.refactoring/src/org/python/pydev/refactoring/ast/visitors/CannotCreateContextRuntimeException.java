package org.python.pydev.refactoring.ast.visitors;

public class CannotCreateContextRuntimeException extends RuntimeException {

    private String msg;

    public CannotCreateContextRuntimeException(Exception e) {
        super(e);
        this.msg = e.getMessage();
    }

    @Override
    public String toString() {
        return msg;
    }

    @Override
    public String getMessage() {
        return msg;
    }

}
