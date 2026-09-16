package br.imd.ufrn.exceptions;

public abstract class CamaroesBaseException extends RuntimeException {

    protected CamaroesBaseException() {
        super();
    }

    protected CamaroesBaseException(Throwable cause) {
        super(cause);
    }

    public abstract String getFriendlyMessage();

    public abstract String getLogMessage();
}
