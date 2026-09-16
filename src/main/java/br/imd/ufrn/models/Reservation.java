package br.imd.ufrn.models;

import br.imd.ufrn.exceptions.InvalidReservationException;
import br.imd.ufrn.exceptions.ReservationAlreadyCancelledException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class Reservation {

    private final String id;
    private final String customer;
    private final String tableId;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final int numberOfPeople;
    private ReservationStatus status;

    public Reservation(
            String id,
            String customer,
            String tableId,
            LocalDateTime start,
            LocalDateTime end,
            int numberOfPeople,
            ReservationStatus status) {
        if (id == null || id.isBlank()) {
            throw new InvalidReservationException("identificador é obrigatório");
        }
        if (customer == null || customer.isBlank()) {
            throw new InvalidReservationException("cliente é obrigatório");
        }
        if (tableId == null || tableId.isBlank()) {
            throw new InvalidReservationException("mesa é obrigatória");
        }
        if (start == null || end == null) {
            throw new InvalidReservationException("horários são obrigatórios");
        }
        if (!end.isAfter(start)) {
            throw new InvalidReservationException("horário de término deve ser posterior ao de início");
        }
        if (numberOfPeople <= 0) {
            throw new InvalidReservationException("quantidade de pessoas deve ser maior que zero");
        }
        if (status == null) {
            throw new InvalidReservationException("status é obrigatório");
        }

        this.id = id;
        this.customer = customer;
        this.tableId = tableId;
        this.start = start;
        this.end = end;
        this.numberOfPeople = numberOfPeople;
        this.status = status;
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    public boolean conflictsWith(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return isActive()
                && otherStart.isBefore(end)
                && otherEnd.isAfter(start);
    }

    public void cancel() {
        if (status != ReservationStatus.ACTIVE) {
            throw new ReservationAlreadyCancelledException(id);
        }
        this.status = ReservationStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reservation)) {
            return false;
        }
        Reservation reservation = (Reservation) o;
        return Objects.equals(id, reservation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reservation{id='" + id + "', customer='" + customer + "', tableId='" + tableId
                + "', start=" + start + ", end=" + end
                + ", numberOfPeople=" + numberOfPeople
                + ", status=" + status + "}";
    }
}
