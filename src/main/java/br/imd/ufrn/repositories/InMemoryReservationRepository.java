package br.imd.ufrn.repositories;

import br.imd.ufrn.models.Reservation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryReservationRepository {

    private final Map<String, Reservation> reservations = new LinkedHashMap<>();

    public void save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
    }

    public Optional<Reservation> findById(String id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public List<Reservation> findAll() {
        return new ArrayList<>(reservations.values());
    }
}
