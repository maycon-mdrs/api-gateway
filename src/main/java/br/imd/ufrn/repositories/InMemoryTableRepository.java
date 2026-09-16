package br.imd.ufrn.repositories;

import br.imd.ufrn.models.Table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryTableRepository {

    private final Map<String, Table> tables = new LinkedHashMap<>();

    public void save(Table table) {
        tables.put(table.getId(), table);
    }

    public Optional<Table> findById(String id) {
        return Optional.ofNullable(tables.get(id));
    }

    public List<Table> findAll() {
        return new ArrayList<>(tables.values());
    }
}
