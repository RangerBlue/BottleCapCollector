package com.km.BottleCapCollector.actuator;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Endpoint(id = "capNotes", enableByDefault = true)
public class BottleCapEndpointNotes {
    private List<String> notes = new ArrayList<>();

    @ReadOperation
    public List<String> notes() {
        return notes;
    }

    @WriteOperation
    public List<String> addNote(String text) {
        notes.add(text);
        return notes;
    }

    @DeleteOperation
    public List<String> deleteNote(int index) {
        if (index < notes.size()) {
            notes.remove(index);
        }
        return notes;
    }
}
