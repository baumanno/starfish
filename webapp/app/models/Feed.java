package models;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import play.data.validation.Constraints;
import play.db.ebean.Model;

/**
 * Provides data modelling necessary for using feeds. Implements required
 * DB-fields and the corresponding getters/setters.
 * 
 * @author baumanno
 * @version 0.1
 * @version 0.2 extend the model to support user/password
 */
@Entity
@Table(name="FEED")
public class Feed extends Model {

    /**
     * 
     */
    private static final long serialVersionUID = 5612873871049582346L;
    
    @Id
    @Column(unique=true,
            nullable=false) 
    private String name;

    public final String getName() {
        return name;
    }

    public final void setName(final String newName) {
        this.name = newName;
    }

    @Constraints.Required
    @Column(
            columnDefinition = "TEXT")
    private String url;

    public final String getUrl() {
        return url;
    }

    public final void setUrl(final String newUrl) {
        this.url = newUrl;
    }

    private String user;

    public final String getUser() {
        return user;
    }

    public final void setUser(final String newUser) {
        this.user = newUser;
    }

    private String pass;

    public final String getPass() {
        return pass;
    }

    public final void setPass(final String newPass) {
        this.pass = newPass;
    }

    private String xPath;

    public final String getXPath() {
        return xPath;
    }

    public final void setXPath(final String newXPath) {
        this.xPath = newXPath;
    }

    /**
     * The cached datavalues, retrieved via XPath.
     */
    private int cachedValue;

    public final int getCachedValue() {
        return cachedValue;
    }

    public final void setCachedValue(final int newCachedValue) {
        this.cachedValue = newCachedValue;
    }

    /**
     * The time of the last refresh.
     */
    @Column(
            nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp lastRefreshAt;

    public final Timestamp getLastRefreshAt() {
        return lastRefreshAt;
    }

    public final void setLastRefreshAt(final Timestamp newTime) {
        this.lastRefreshAt = newTime;
    }

    /**
     * A helper to assist in querying the database.
     * 
     * This variable acts as a helper on which database-queries can be
     * performed. It is accessed via a getter-method.
     */
    private static Finder<String, Feed> find = new Finder<String, Feed>(
            String.class, Feed.class);

    public static Finder<String, Feed> getFind() {
        return find;
    }

    /**
     * Writes a feed to the database.
     * 
     * In v0.1, no pattern-checking for well-formed urls is in place yet.
     * 
     * @param name
     *            the name of the feed as it should appear in the database
     * @param url
     *            the url the feed is at
     * @param user
     *            username for HTTPS
     * @param pass
     *            password corresponding to user
     * @param xPath
     *            the xPath
     * @param value
     *            the value retrieved via XPath
     * 
     */
    public Feed(final String name, final String url,
            final String user, final String pass, final String xPath,
            final int value) {

        this.setName(name);
        this.setUrl(url);
        this.setUser(user);
        this.setPass(pass);
        this.setXPath(xPath);
        this.setCachedValue(value);
        this.setLastRefreshAt(new Timestamp(System.currentTimeMillis()));                
    }
}
