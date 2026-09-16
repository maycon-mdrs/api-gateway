package br.imd.ufrn.exceptions;

public class InactiveTableException extends CamaroesBaseException {

    private final String tableId;

    public InactiveTableException(String tableId) {
        this.tableId = tableId;
    }

    @Override
    public String getFriendlyMessage() {
        return "A mesa " + tableId + " está inativa.";
    }

    @Override
    public String getLogMessage() {
        return "InactiveTableException: mesa " + tableId + " inativa.";
    }
}
