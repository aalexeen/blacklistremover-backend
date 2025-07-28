package org.bhmc.blacklistremover.error;

import static org.bhmc.blacklistremover.error.ErrorType.BAD_REQUEST;

public class IllegalRequestDataException extends AppException {
    public IllegalRequestDataException(String msg) {
        super(msg, BAD_REQUEST);
    }
}