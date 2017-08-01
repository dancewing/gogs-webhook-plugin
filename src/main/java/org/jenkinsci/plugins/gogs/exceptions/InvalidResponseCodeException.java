package org.jenkinsci.plugins.gogs.exceptions;

import org.jenkinsci.plugins.gogs.Messages;

public class InvalidResponseCodeException extends NotificationException {

    public InvalidResponseCodeException(int responseCode) {
        super(Messages.InvalidResponseCode(responseCode));
    }
}
