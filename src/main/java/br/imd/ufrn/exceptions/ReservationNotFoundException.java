package br.imd.ufrn.exceptions;

public class ReservationNotFoundException extends CamaroesBaseException {

    private final String reservationId;

    public ReservationNotFoundException(String reservationId) {
        this.reservationId = reservationId;
    }

    @Override
    public String getFriendlyMessage() {
        return "A reserva " + reservationId + " não foi encontrada.";
    }

    @Override
    public String getLogMessage() {
        return "ReservationNotFoundException: reserva " + reservationId + " inexistente.";
    }
}
