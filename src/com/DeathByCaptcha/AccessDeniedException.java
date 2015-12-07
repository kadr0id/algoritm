package com.DeathByCaptcha;

/**
 * @author Sergey Kolchin <ksa242@gmail.com>
 */
public class AccessDeniedException extends Exception {

    public AccessDeniedException(String message) {
        super(message);
    }
}
