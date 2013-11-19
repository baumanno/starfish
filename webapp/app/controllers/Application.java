package controllers;



import controllers.service.Communicator;

import play.Logger;
import play.mvc.*;
import views.html.*;

/**
 * @author Werner Hoffmann
 * 
 * @version 0.1
 *          Initalization of the programm
 * 
 */
public class Application extends Controller {
    public static Communicator Communicator;
	
    /**
     * Main Method
     * 
     * gets called by "GET /".
     * 
     */
  public static Result index() {
	Logger.debug("Play is up and running. Mainpage is called");
	if(Communicator == null){
	Communicator = new Communicator();
	}
    return ok(index.render());
  }
  
}