

package src.game.tactical ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.building.* ;



public class Application extends Plan {
	
	
	//
	//  Priority is based on that offered by the bounty.  It fades the longer the
	//  actor has to wait (by 5 per day, let's say.)
	
	//  Go to the rendez-vous point for a particular mission, and wait there until
	//  You get dismissed or approved.  Also, don't apply for missions that have
	//  a lot of applicants (each one more than 5 reduces priority by 1.)
	
	//  Okay.  Attraction to combat.  Attraction to exploration.
	//  Attraction to money.  Just get that much right.
	
	Mission mission ;
	
	
	public Application(Actor actor, Mission mission) {
		super(actor, mission) ;
	}
	
	
	public Application(Session s) throws Exception {
		super(s) ;
	}
	
	
	
	
	
	
	
	
	protected Behaviour getNextStep() {
		return null ;
	}
}






