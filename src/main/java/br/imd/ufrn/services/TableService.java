package br.imd.ufrn.services;

import br.imd.ufrn.exceptions.TableNotFoundException;
import br.imd.ufrn.models.Table;
import br.imd.ufrn.repositories.InMemoryTableRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TableService {

    private final InMemoryTableRepository tableRepository;

    public TableService() {
        this(new InMemoryTableRepository());
    }

    public TableService(InMemoryTableRepository tableRepository) {
        this.tableRepository = tableRepository;
        registerDefaultTables();
    }

    public List<Table> findActive() {
        return new ArrayList<>(tableRepository.findAll().stream()
                .filter(Table::isActive)
                .toList());
    }

    public Optional<Table> findById(String id) {
        return tableRepository.findById(id);
    }

    public Table findByIdOrThrow(String id) {
        return findById(id)
                .orElseThrow(() -> new TableNotFoundException(id));
    }

    public boolean canAccommodate(String tableId, int numberOfPeople) {
        return findById(tableId)
                .map(table -> table.canAccommodate(numberOfPeople))
                .orElse(false);
    }

    private void registerDefaultTables() {
        tableRepository.save(new Table("M01", 2, "Varanda", true));
        tableRepository.save(new Table("M02", 2, "Varanda", true));
        tableRepository.save(new Table("M03", 4, "Salão", true));
        tableRepository.save(new Table("M04", 4, "Salão", true));
        tableRepository.save(new Table("M05", 6, "Salão", true));
        tableRepository.save(new Table("M06", 8, "VIP", true));
        tableRepository.save(new Table("M07", 4, "Terraço", true));
        tableRepository.save(new Table("M08", 2, "Terraço", false));
    }
}
