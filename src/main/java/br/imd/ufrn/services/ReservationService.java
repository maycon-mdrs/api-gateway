package br.imd.ufrn.services;

import br.imd.ufrn.exceptions.ConflictingReservationException;
import br.imd.ufrn.exceptions.InactiveTableException;
import br.imd.ufrn.exceptions.InsufficientTableCapacityException;
import br.imd.ufrn.exceptions.InvalidReservationException;
import br.imd.ufrn.exceptions.LeaderOnlyOperationException;
import br.imd.ufrn.exceptions.ReservationNotFoundException;
import br.imd.ufrn.models.Reservation;
import br.imd.ufrn.models.ReservationStatus;
import br.imd.ufrn.models.ServerRole;
import br.imd.ufrn.models.Table;
import br.imd.ufrn.repositories.InMemoryReservationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ReservationService {

    private final InMemoryReservationRepository reservationRepository;
    private final TableService tableService;
    private final ServerRole role;

    public ReservationService(TableService tableService, ServerRole role) {
        this(
                new InMemoryReservationRepository(),
                tableService,
                role);
    }

    public ReservationService(
            InMemoryReservationRepository reservationRepository,
            TableService tableService,
            ServerRole role) {
        if (reservationRepository == null) {
            throw new InvalidReservationException("repositório de reservas é obrigatório");
        }
        if (tableService == null) {
            throw new InvalidReservationException("serviço de mesas é obrigatório");
        }
        if (role == null) {
            throw new InvalidReservationException("papel do servidor é obrigatório");
        }
        this.reservationRepository = reservationRepository;
        this.tableService = tableService;
        this.role = role;
    }

    public synchronized Reservation create(
            String customer,
            String tableId,
            LocalDateTime start,
            LocalDateTime end,
            int numberOfPeople) {
        requireWriteAccess();

        Table table = tableService.findByIdOrThrow(tableId);

        if (!table.isActive()) {
            throw new InactiveTableException(tableId);
        }
        if (!table.canAccommodate(numberOfPeople)) {
            throw new InsufficientTableCapacityException(
                    tableId, numberOfPeople, table.getCapacity());
        }

        Reservation reservation = new Reservation(
                UUID.randomUUID().toString(),
                customer,
                tableId,
                start,
                end,
                numberOfPeople,
                ReservationStatus.ACTIVE);

        if (hasInternalConflict(tableId, start, end)) {
            throw new ConflictingReservationException(tableId, start, end);
        }

        reservationRepository.save(reservation);
        return reservation;
    }

    public synchronized Optional<Reservation> findById(String id) {
        return reservationRepository.findById(id);
    }

    public Reservation findByIdOrThrow(String id) {
        return findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public synchronized List<Reservation> findActive() {
        List<Reservation> activeReservations = new ArrayList<>();
        for (Reservation reservation : reservationRepository.findAll()) {
            if (reservation.isActive()) {
                activeReservations.add(reservation);
            }
        }
        return activeReservations;
    }

    public List<Table> findAvailableTables(
            LocalDateTime start,
            LocalDateTime end,
            int numberOfPeople) {
        validateAvailabilityRequest(start, end, numberOfPeople);

        List<Table> availableTables = new ArrayList<>();
        for (Table table : tableService.findActive()) {
            if (table.canAccommodate(numberOfPeople)
                    && !hasConflict(table.getId(), start, end)) {
                availableTables.add(table);
            }
        }
        return availableTables;
    }

    public synchronized Reservation cancel(String id) {
        requireWriteAccess();

        Reservation reservation = findByIdOrThrow(id);
        reservation.cancel();
        return reservation;
    }

    public synchronized boolean hasConflict(
            String tableId, LocalDateTime start, LocalDateTime end) {
        return hasInternalConflict(tableId, start, end);
    }

    public ServerRole getRole() {
        return role;
    }

    private boolean hasInternalConflict(
            String tableId,
            LocalDateTime start,
            LocalDateTime end) {
        for (Reservation reservation : reservationRepository.findAll()) {
            if (reservation.getTableId().equals(tableId)
                    && reservation.conflictsWith(start, end)) {
                return true;
            }
        }
        return false;
    }

    private void validateAvailabilityRequest(
            LocalDateTime start,
            LocalDateTime end,
            int numberOfPeople) {
        if (start == null || end == null) {
            throw new InvalidReservationException(
                    "horários para consulta de disponibilidade são obrigatórios");
        }
        if (!end.isAfter(start)) {
            throw new InvalidReservationException(
                    "horário de término deve ser posterior ao de início");
        }
        if (numberOfPeople <= 0) {
            throw new InvalidReservationException(
                    "quantidade de pessoas deve ser maior que zero");
        }
    }

    private void requireWriteAccess() {
        if (role != ServerRole.LEADING) {
            throw new LeaderOnlyOperationException();
        }
    }
}
