package org.bhmc.blacklistremover.error;

import static org.bhmc.blacklistremover.error.ErrorType.NOT_FOUND;

public class NotFoundException extends AppException {
    public NotFoundException(String msg) {
        super(msg, NOT_FOUND);
    }
}