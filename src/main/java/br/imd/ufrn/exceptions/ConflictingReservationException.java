package br.imd.ufrn.exceptions;

import java.time.LocalDateTime;

public class ConflictingReservationException extends CamaroesBaseException {

    private final String tableId;
    private final LocalDateTime start;
    private final LocalDateTime end;

    public ConflictingReservationException(
            String tableId, LocalDateTime start, LocalDateTime end) {
        this.tableId = tableId;
        this.start = start;
        this.end = end;
    }

    @Override
    public String getFriendlyMessage() {
        return "A mesa " + tableId + " já está reservada no período informado.";
    }

    @Override
    public String getLogMessage() {
        return "ConflictingReservationException: mesa " + tableId
                + ", período " + start + " até " + end + ".";
    }
}
