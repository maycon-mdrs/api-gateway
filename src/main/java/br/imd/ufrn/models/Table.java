package br.imd.ufrn.models;

import br.imd.ufrn.exceptions.InvalidTableException;
import lombok.Getter;

@Getter
public class Table {

    private final String id;
    private final int capacity;
    private final String area;
    private final boolean active;

    public Table(String id, int capacity, String area, boolean active) {
        if (id == null || id.isBlank()) {
            throw new InvalidTableException("identificador é obrigatório");
        }
        if (capacity <= 0) {
            throw new InvalidTableException("capacidade deve ser maior que zero");
        }
        if (area == null || area.isBlank()) {
            throw new InvalidTableException("área é obrigatória");
        }

        this.id = id;
        this.capacity = capacity;
        this.area = area;
        this.active = active;
    }

    public boolean canAccommodate(int numberOfPeople) {
        return active && numberOfPeople > 0 && numberOfPeople <= capacity;
    }

    @Override
    public String toString() {
        return "Table{id='" + id + "', capacity=" + capacity
                + ", area='" + area + "', active=" + active + "}";
    }
}
