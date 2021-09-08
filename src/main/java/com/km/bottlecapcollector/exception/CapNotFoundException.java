package com.km.bottlecapcollector.exception;

import java.text.MessageFormat;

public class CapNotFoundException extends  RuntimeException{
    public CapNotFoundException(Long id) {
        super(MessageFormat.format("Bottle cap with id: {0} not found", id));
    }
}
