package br.imd.ufrn.exceptions;

public class InvalidReservationException extends CamaroesBaseException {

    private final String reason;

    public InvalidReservationException(String reason) {
        this.reason = reason;
    }

    @Override
    public String getFriendlyMessage() {
        return "Reserva inválida: " + reason + ".";
    }

    @Override
    public String getLogMessage() {
        return "InvalidReservationException: " + reason + ".";
    }
}
