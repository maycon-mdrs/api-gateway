package br.imd.ufrn.exceptions;

public class ReservationAlreadyCancelledException extends CamaroesBaseException {

    private final String reservationId;

    public ReservationAlreadyCancelledException(String reservationId) {
        this.reservationId = reservationId;
    }

    @Override
    public String getFriendlyMessage() {
        return "A reserva " + reservationId + " já está cancelada.";
    }

    @Override
    public String getLogMessage() {
        return "ReservationAlreadyCancelledException: reserva " + reservationId + " já cancelada.";
    }
}
