package helpers;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import play.Logger;

/**
 * Provides authentication methods for HTTPS-feeds.
 * 
 * @author baumanno
 * @version 0.1
 * 
 */

public class HttpsAuthenticator extends Authenticator {
    private String user;
    private String pass;

    public final void setUser(final String newUser) {
        this.user = newUser;
    }

    public final void setPass(final String newPass) {
        this.pass = newPass;
    }

    /**
     * Sets up the Authenticator-object with user/password-credentials.
     * 
     * @param u
     *            the username
     * @param p
     *            the password
     */

    public HttpsAuthenticator(final String u, final String p) {
        setUser(u);
        setPass(p);
    }

    /**
     * Sets up the password-authenticator.
     * 
     * This only returns the password-authenticator if the request-protocol is
     * HTTPS. If it is standard HTTP, no authentication is needed and null is
     * returned.
     */

    public final PasswordAuthentication getPasswordAuthentication() {
        Logger.debug("Requested URL is " + getRequestingURL());
        Logger.debug("Requested host is " + getRequestingHost());
        Logger.debug("Requested IP is " + getRequestingSite());
        Logger.debug("Request port is " + getRequestingPort());

        /*
         check whether the request is HTTPS
         if it is HTTP, we don't need an authenticator
         */

        if (getRequestingProtocol().equals("https")) {
            Logger.debug("Request protocol is " + getRequestingProtocol());
            Logger.warn("Attempting to authenticate as '" + user + "'");

            return (new PasswordAuthentication(user, pass.toCharArray()));
        } else {
            Logger.error("An error occurred setting up authentication;"
                    + "please check the following:");
            Logger.error("Is the URL correct?");
            Logger.error("Are username and password correct?");
            return null;
        }
    }
}
