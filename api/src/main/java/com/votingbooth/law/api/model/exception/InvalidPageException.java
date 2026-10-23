package com.votingbooth.law.api.model.exception;

public class InvalidPageException extends RuntimeException {

    public InvalidPageException(int page, int size, int maxSize) {
        super("Page " + page + " of size " + size + " is invalid: page must be 0 or more, size between 1 and " + maxSize + ".");
    }

}
