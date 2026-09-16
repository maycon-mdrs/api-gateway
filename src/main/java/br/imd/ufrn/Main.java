package br.imd.ufrn;

import br.imd.ufrn.exceptions.CamaroesBaseException;
import br.imd.ufrn.models.Table;
import br.imd.ufrn.models.Reservation;
import br.imd.ufrn.models.ServerRole;
import br.imd.ufrn.services.ReservationService;
import br.imd.ufrn.services.TableService;

import java.time.LocalDateTime;
import java.util.List;


public class Main {

    public static void main(String[] args) {
        System.out.println("[api-gateway] Camarões");
        
        TableService tableService = new TableService();
        ReservationService leader = new ReservationService(tableService, ServerRole.LEADING);
        ReservationService follower = new ReservationService(tableService, ServerRole.FOLLOWING);

        System.out.println("\nMesas ativas:");
        for (Table table : tableService.findActive()) {
            System.out.println("  " + table);
        }

        LocalDateTime start = LocalDateTime.of(2026, 9, 20, 19, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 20, 21, 0);

        System.out.println("\nDisponíveis para 4 pessoas em " + start + ":");
        List<Table> availableTables = leader.findAvailableTables(start, end, 4);
        for (Table table : availableTables) {
            System.out.println("  " + table.getId() + " (" + table.getArea() + ")");
        }

        Reservation reservation = leader.create("Ana", "M03", start, end, 4);
        System.out.println("\nReserva criada pelo LEADER: " + reservation);

        System.out.println("Consulta: " + leader.findById(reservation.getId()).orElse(null));

        demonstrateError("mesa inexistente", () ->
                leader.create("Bruno", "M99", start, end, 2));

        demonstrateError("capacidade excedida", () ->
                leader.create("Carla", "M01", start.plusDays(1), end.plusDays(1), 10));

        demonstrateError("intervalo sobreposto", () ->
                leader.create("Diego", "M03", start.plusHours(1), end.plusHours(1), 3));

        demonstrateError("FOLLOWER não cria", () ->
                follower.create("Elena", "M04", start, end, 2));

        demonstrateError("FOLLOWER não cancela", () ->
                follower.cancel(reservation.getId()));

        Reservation cancelled = leader.cancel(reservation.getId());
        System.out.println("\nReserva cancelada pelo LEADER: " + cancelled);

        System.out.println("\nApós cancelamento, M03 disponível novamente?");
        boolean available = leader.findAvailableTables(start, end, 4).stream()
                .anyMatch(m -> m.getId().equals("M03"));
        System.out.println("  " + available);
    }

    private static void demonstrateError(String scenario, Runnable action) {
        try {
            action.run();
            System.out.println("FALHOU (" + scenario + "): esperado erro");
        } catch (CamaroesBaseException e) {
            System.out.println("OK (" + scenario + "): " + e.getFriendlyMessage());
        }
    }
}
