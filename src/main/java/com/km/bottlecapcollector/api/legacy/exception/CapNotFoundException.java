package com.km.bottlecapcollector.api.legacy.exception;

import com.km.bottlecapcollector.api.handler.exception.AppResourceNotFoundException;

import java.text.MessageFormat;

public class CapNotFoundException extends AppResourceNotFoundException {
    public CapNotFoundException(Long id) {
        super(MessageFormat.format("Bottle cap with id: {0} not found", id));
    }
}
