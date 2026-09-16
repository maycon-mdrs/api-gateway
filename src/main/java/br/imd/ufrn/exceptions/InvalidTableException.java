package br.imd.ufrn.exceptions;

public class InvalidTableException extends CamaroesBaseException {

    private final String reason;

    public InvalidTableException(String reason) {
        this.reason = reason;
    }

    @Override
    public String getFriendlyMessage() {
        return "Mesa inválida: " + reason + ".";
    }

    @Override
    public String getLogMessage() {
        return "InvalidTableException: " + reason + ".";
    }
}
