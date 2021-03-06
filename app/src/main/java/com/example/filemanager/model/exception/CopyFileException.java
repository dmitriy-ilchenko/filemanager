package com.example.filemanager.model.exception;

public class CopyFileException extends RuntimeException {
    public CopyFileException(Throwable cause) {
        super(cause);
    }

    public CopyFileException(String message, Throwable cause) {
        super("Unable to copy file: " + message, cause);
    }
}
