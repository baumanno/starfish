package controllers;

import models.Trc;
import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import controllers.service.Communicator;
import views.html.*;
import views.html.trc.*;


public class TracerouteSubmission extends Controller {
    
    /**
     * Defines a form wrapping the Metric class.
     */ 
    final static Form<Trc> trcSubmitForm = form(Trc.class);
  
    /**
     * Display a blank form.
     */ 
    public static Result blank() {
        return ok(form.render(trcSubmitForm));
    }
  
    
  
    /**
     * Handle the form submission.
     */
    public static Result submit() {
        Form<Trc> filledForm = trcSubmitForm.bindFromRequest();
        
            Trc created = filledForm.get();
            if(Communicator.MyLogic == null){
    			new Communicator();
    			}
            Communicator.MyLogic.traceroute(created);

            return ok(summary.render(created));
    }
}