

package src.game.social ;
import src.game.actors.* ;
import src.game.building.Boardable ;
import src.game.common.* ;


public interface Employment extends Session.Saveable, Boardable {
  
  
  Behaviour jobFor(Actor actor) ;
  void setWorker(Actor actor, boolean is) ;
  void setApplicant(Application app, boolean is) ;
  int numOpenings(Background b) ;
  //
  //  TODO:  Have Employers return a Personnel class in the same way that
  //  Installations return a Structure class.
  
  
}