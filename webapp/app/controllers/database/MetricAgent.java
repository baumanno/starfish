package controllers.database;

import java.util.List;

import models.Metric;
import play.Logger;
import play.mvc.Controller;

/**
 * @author Werner Hoffmann
 * @author Oliver Baumann
 * 
 * @version 0.3 Fully-functional CreateReadDelete-methods
 * @version 0.4 improved CRUD; refactoring
 */
public class MetricAgent extends Controller {

    /**
     * Saves metric to database
     * 
     * Every metric has a unique name, a unit and a default value. This default
     * kicks in if no feed is assigned to the metric.
     * 
     * @param name
     *            the name the metric should have; this is the primary-key in
     *            the DB
     * @param unit
     *            the unit the information has (additional Information for a
     *            metric)
     * @param defaultValue
     *            the DefaultValue, when no Feed is given
     * @return true on successful creation, else false
     * 
     */
    public static boolean createMetric(final String name, final String unit,
            final int defaultValue) {
        Logger.debug("New Metric created: " + name + " " + unit + " "
                + defaultValue);

        /*
         Create the metric with all the parameters
         createMetric() also saves to DB.
         */

        Metric m = new Metric(name, unit, defaultValue);
        m.save();

        return true;
    }

    /**
     * Delete Metric.
     * 
     * @param name
     *            the metric to delete
     * @return true on successful delete
     */
    public static boolean deleteMetric(final String name) {
        Metric f = Metric.getFind().where().eq("name", name).findUnique();

        try {
            f.delete();
            Logger.debug("Deleted metric: " + name);
            return true;
        } catch (Exception e) {
            Logger.error("Error deleting Metric. ", e);
            return false;
        }
    }

    /**
     * Get list of all Metrics.
     * 
     * @return List of all Metrics in the database
     */
    public static List<Metric> getAllMetrics() {
        List<Metric> metrics = Metric.getFind().all();

        return metrics;
    }
}
