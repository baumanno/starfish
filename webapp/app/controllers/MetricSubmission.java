package controllers;

import models.Metric;
import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import controllers.service.Communicator;
import views.html.*;
import views.html.metric.*;


public class MetricSubmission extends Controller {
    
    /**
     * Defines a form wrapping the Metric class.
     */ 
    final static Form<Metric> metricSubmitForm = form(Metric.class);
  
    /**
     * Display a blank form.
     */ 
    public static Result blank() {
        return ok(form.render(metricSubmitForm));
    }
  
    
  
    /**
     * Handle the form submission.
     */
    public static Result submit() {
        Form<Metric> filledForm = metricSubmitForm.bindFromRequest();
        
        if(filledForm.hasErrors()) {
            return badRequest(form.render(filledForm));
        } else {
            Metric created = filledForm.get();
            Logger.debug(created.getName()+created.getUnit()+created.getDefaultValue());
            if(Communicator.MyLogic == null){
    			new Communicator();
    			}
            boolean check = Communicator.MyLogic.newMetric(created);
    		if(check){
    			return ok(summary.render(created));
    		}else{
    			return badRequest(summary.render(created));
    		}
    		
        }
        
        
    }
  
}