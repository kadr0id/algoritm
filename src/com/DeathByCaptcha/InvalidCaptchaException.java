package com.DeathByCaptcha;

/**
 * @author Sergey Kolchin <ksa242@gmail.com>
 */
public class InvalidCaptchaException extends Exception {

    public InvalidCaptchaException(String message) {
        super(message);
    }
}
