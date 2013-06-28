


package src.game.planet ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.user.* ;



public class Lair extends Venue {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  Species species ;
  
  
  public Lair(int size, int high, int entranceFace, Base base) {
    super(size, high, entranceFace, base) ;
  }
  
  
  public Lair(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Behavioural assignments (all null in this case.)
    */
  public Behaviour jobFor(Actor actor) { return null ; }
  protected Vocation[] careers() { return new Vocation[0] ; }
  protected Item.Type[] goods() { return new Item.Type[0] ; }

  
  //
  //  TODO:  The 'onGrowth' method for a lair needs to do some damage to it.
  //  TODO:  For that, you will need to update the VenueStocks class.
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return species.name+" lair" ;
  }

  public Composite portrait(BaseUI UI) {
    return null ;
  }

  public String helpInfo() {
    return null ;
  }
  
  public String buildCategory() {
    return BuildingsTab.TYPE_HIDDEN ;
  }
}



