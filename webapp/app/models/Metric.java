package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import play.data.validation.Constraints;
import play.db.ebean.Model;

/**
 * Provides data for one Metric. One Metric is a layer covering the whole
 * network. Includes all information about this layer. Not the concrete values
 * (see MetricValues).
 * 
 * @author Werner Hoffmann
 * @version 0.3
 */
@Entity
@Table(
        name = "METRIC")
public class Metric extends Model {
    /**
     * 
     */
    private static final long serialVersionUID = -1401232548924996000L;

    @Id
    @Column(
            unique = true,
            nullable = false)
    private String name;

    private String unit;

    @Constraints.Required
    private int defaultValue;

    /**
     * @return the unit
     */
    public final String getUnit() {
        return unit;
    }

    /**
     * @return the defaultValue
     */
    public final int getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param newName
     *            the name to set
     */
    public final void setName(final String newName) {
        this.name = newName;
    }

    /**
     * @param newUnit
     *            the unit to set
     */
    public final void setUnit(final String newUnit) {
        unit = newUnit;
    }

    /**
     * @param newDefaultValue
     *            the defaultValue to set
     */
    public final void setDefaultValue(final int newDefaultValue) {
        this.defaultValue = newDefaultValue;
    }

    public final String getName() {
        return name;
    }

    public Metric(final String theName, final String theUnit,
            final int theDefaultValue) {
        this.setName(theName);
        this.setUnit(theUnit);
        this.setDefaultValue(theDefaultValue);
    }

    private static Finder<String, Metric> find = new Finder<String, Metric>(
            String.class, Metric.class);

    public static Finder<String, Metric> getFind() {
        return find;
    }
}
