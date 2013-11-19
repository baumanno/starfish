package controllers;

import models.LinkToMetric;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import controllers.service.Communicator;

import views.html.*;
import views.html.linkmetric.*;

public class LinkMetricSubmission extends Controller {

    /**
     * Defines a form wrapping the Metric class.
     */
    final static Form<LinkToMetric> linkMetricSubmitForm = form(LinkToMetric.class);

    /**
     * Display a blank form.
     */
    public static Result blank() {
        return ok(form.render(linkMetricSubmitForm));
    }

    /**
     * Handle the form submission.
     */
    public static Result submit() {
        Form<LinkToMetric> filledForm = linkMetricSubmitForm.bindFromRequest();

        if (filledForm.hasErrors()) {
            return badRequest(form.render(filledForm));
        } else {
            LinkToMetric created = filledForm.get();
            if (Communicator.MyLogic == null) {
                new Communicator();
            }
            boolean check = Communicator.MyLogic.addRelLinkMetricFeed(created);
            if (check) {			
                return ok(summary.render(created));
            } else {
                return badRequest(summary.render(created));
            }

        }

    }

}