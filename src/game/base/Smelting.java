/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.user.* ;
import src.util.* ;



public class Smelting extends Plan implements EconomyConstants {
  
  
  
  /**  Fields, constructors and save/load methods-
    */
  private static boolean verbose = false ;
  
  final ExcavationSite shaft ;
  final Venue smeltsAt ;
  final Service mined ;
  final Item sample ;
  
  
  Smelting(Actor actor, Venue smelter, ExcavationSite shaft, Service mined) {
    super(actor, smelter) ;
    this.shaft = shaft ;
    this.smeltsAt = smelter ;
    this.mined = mined ;
    sample = Item.asMatch(SAMPLES, mined) ;
  }
  
  
  public Smelting(Session s) throws Exception {
    super(s) ;
    shaft = (ExcavationSite) s.loadObject() ;
    smeltsAt = (Venue) s.loadObject() ;
    mined = (Service) s.loadObject() ;
    sample = Item.asMatch(SAMPLES, mined) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(shaft) ;
    s.saveObject(smeltsAt) ;
    s.saveObject(mined) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    float amount = 0 ;
    if (smeltsAt != shaft) {
      if (Plan.competition(Smelting.class, smeltsAt, actor) > 0) return 0 ;
      final Smelter smelter = (Smelter) smeltsAt ;
      amount += smelter.stocks.amountOf(smelter.type) ;
      amount += actor.gear.amountOf(smelter.type) ;
      amount += smeltsAt.stocks.amountOf(SAMPLES) ;
      return Visit.clamp(
        (amount / 5) * Action.ROUTINE, 0, Action.URGENT
      ) ;
    }
    else {
      amount += smeltsAt.stocks.amountOf(SAMPLES) ;
      return Visit.clamp(
        (amount / 10) * Action.ROUTINE, 0, Action.URGENT
      ) ;
    }
  }
  
  
  protected Behaviour getNextStep() {
    final Smelter smelter = (shaft == smeltsAt) ? null : (Smelter) smeltsAt ;
    //if (smelter != null && smelter.type != mined) continue ;
    
    if (smelter != null && smelter.allCooled()) {
      final Delivery d = new Delivery(mined, smeltsAt, shaft) ;
      if (smeltsAt.stocks.amountOf(mined) > 0) return d ;
    }
    
    final Item match = Item.asMatch(SAMPLES, mined) ;
    final float atS = smeltsAt.stocks.amountOf(match) ;
    if (atS > 0 && shaft.personnel.onShift(actor)) {
      final Action smelting = new Action(
        actor, smeltsAt,
        this, "actionSmeltOre",
        Action.BUILD, "Smelting "+mined
      ) ;
      return smelting ;
    }
    
    if (smelter != null && shaft.stocks.amountOf(match) > 0) {
      final Batch <Item> matches = shaft.stocks.matches(match) ;
      final Delivery d = new Delivery(matches, shaft, smeltsAt) ;
      return d ;
    }
    
    if (actor.gear.amountOf(match) > 0) {
      final Batch <Item> matches = shaft.stocks.matches(match) ;
      final Delivery d = new Delivery(matches, shaft, smeltsAt) ;
      return d ;
    }
    return null ;
  }
  
  
  
  public boolean actionSmeltOre(Actor actor, Venue venue) {
    for (Item sample : venue.stocks.matches(SAMPLES)) {
      final Service type = (Service) sample.refers ;
      
      //
      //  Firstly, check to ensure that your skills are up to snuff-
      float success = 0 ;
      if (actor.traits.test(HARD_LABOUR, MODERATE_DC, 1)) success++ ;
      if (! actor.traits.test(GEOPHYSICS, ROUTINE_DC, 1)) success-- ;
      if (actor.traits.test(GEOPHYSICS, DIFFICULT_DC, 1)) success++ ;
      
      //
      //  Smelting at the smelter proper confers a substantial efficiency
      //  bonus, but can also be done at the shaft itself.
      if (success > 0) {
        success *= (2 + venue.structure.upgradeBonus(type)) / 2f ;
        if (venue instanceof Smelter) {
          final float amount = Math.min(1, sample.amount) ;
          venue.stocks.removeItem(Item.withAmount(sample, amount)) ;
          ((Smelter) venue).fillMolds(amount) ;
        }
        else {
          final float amount = Math.min(1, sample.amount) ;
          venue.stocks.bumpItem(type, success * amount / 2) ;
          venue.stocks.removeItem(Item.withAmount(sample, amount)) ;
        }
        
        return true ;
      }
      return false ;
    }
    return false ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Smelting ore at "+smeltsAt) ;
  }
}








