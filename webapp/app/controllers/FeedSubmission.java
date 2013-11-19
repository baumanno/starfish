package controllers;

import controllers.service.Communicator;
import play.*;
import play.mvc.*;
import play.data.*;

import models.Feed;
import views.html.*;
import views.html.feed.*;

import models.*;

public class FeedSubmission extends Controller {
    
    /**
     * Defines a form wrapping the Feed class.
     */ 
    final static Form<Feed> feedSubmissionForm = form(Feed.class);
  
    /**
     * Display a blank form.
     */ 
    public static Result blank() {
        return ok(form.render(feedSubmissionForm));
    }
  
    
  
    /**
     * Handle the form submission.
     */
    public static Result submit() {
        Form<Feed> filledForm = feedSubmissionForm.bindFromRequest();
        
        if(filledForm.hasErrors()) {
            return badRequest(form.render(filledForm));
        } else {
            Feed created = filledForm.get();
            if(Communicator.MyLogic == null){
    			new Communicator();
    			}
            boolean check = Communicator.MyLogic.newFeed(created);
    		if(check){
    			return ok(summary.render(created));
    		}else{
    			return badRequest(summary.render(created));
    		}
            
        }
    }
  
}