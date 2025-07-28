package org.bhmc.blacklistremover.error;

import static org.bhmc.blacklistremover.error.ErrorType.DATA_CONFLICT;

public class DataConflictException extends AppException {
    public DataConflictException(String msg) {
        super(msg, DATA_CONFLICT);
    }
}