package br.imd.ufrn.exceptions;

public class InsufficientTableCapacityException extends CamaroesBaseException {

    private final String tableId;
    private final int numberOfPeople;
    private final int capacity;

    public InsufficientTableCapacityException(
            String tableId, int numberOfPeople, int capacity) {
        this.tableId = tableId;
        this.numberOfPeople = numberOfPeople;
        this.capacity = capacity;
    }

    @Override
    public String getFriendlyMessage() {
        return "A mesa " + tableId + " comporta no máximo " + capacity
                + " pessoas, mas a reserva solicita " + numberOfPeople + ".";
    }

    @Override
    public String getLogMessage() {
        return "InsufficientTableCapacityException: mesa " + tableId
                + ", capacity " + capacity + ", solicitadas "
                + numberOfPeople + ".";
    }
}
