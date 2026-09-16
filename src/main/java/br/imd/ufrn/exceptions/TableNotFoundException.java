package br.imd.ufrn.exceptions;

public class TableNotFoundException extends CamaroesBaseException {

    private final String tableId;

    public TableNotFoundException(String tableId) {
        this.tableId = tableId;
    }

    @Override
    public String getFriendlyMessage() {
        return "A mesa " + tableId + " não foi encontrada.";
    }

    @Override
    public String getLogMessage() {
        return "TableNotFoundException: mesa " + tableId + " inexistente.";
    }
}
