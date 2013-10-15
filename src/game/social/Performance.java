


package src.game.social ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.base.* ;
import src.user.* ;
import src.util.* ;




public class Performance extends Plan implements Abilities {
  
  
  /**  Data fields, setup and save/load functions-
    */
  final static String PERFORM_NAMES[] = {
    "Red Planet Blues, by Khal Segin & Tolev Zaller",
    "It's Full Of Stars, by D. B. Unterhaussen",
    "Take The Sky From Me, by Wedon the Elder",
    "Men Are From Asra Novi, by The Ryod Sisters",
    "Ode To A Hrexxen Gorn, by Ultimex 1450",
    "Geodesic Dome Science Rap, by Sarles Matson",
    "Stuck In The Lagrange Point With You, by Eniud Yi",
    "Untranslatable Feelings, by Strain Variant Beta-7J",
    "A Credit For Your Engram, by Tobul Masri Mark IV",
    "Where Everyone Knows Your Scent Signature, by The Imperatrix",
    "101111-00879938-11AA9191-101, by Lucinda O",
    "Pi Is The Loneliest Number, by Marec 'Irrational' Bel",
    "Zakharov And MG Go Go Go, by Natalya Morgan-Skye",
    "Procyon Nerve-Wipe Hymn, Traditional",
    "ALL HAIL THE MESMERFROG, by ALL HAIL THE MESMERFROG",
  } ;
  final static String EFFECT_DESC[] = {
    "Poor",
    "Fair",
    "Good",
    "Excellent",
    "Rapturous",
  } ;
  
  
  final Venue venue ;
  final Skill employed ;
  int nameID ;
  
  private float performValue = 2 ;
  private float beginTime = -1 ;
  
  
  public Performance(Actor actor, Venue venue, Skill employed) {
    super(actor, venue) ;
    this.venue = venue ;
    this.employed = employed ;
    nameID = Rand.index(PERFORM_NAMES.length) ;
  }
  
  
  public Performance(Session s) throws Exception {
    super(s) ;
    venue = (Venue) s.loadObject() ;
    employed = (Skill) ALL_TRAIT_TYPES[s.loadInt()] ;
    nameID = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(venue) ;
    s.saveInt(employed.traitID) ;
    s.saveInt(nameID) ;
  }
  
  
  public float performValue() {
    return performValue ;
  }
  
  
  public String performDesc() {
    return PERFORM_NAMES[nameID] ;
  }
  
  
  public String qualityDesc() {
    String desc = EFFECT_DESC[Visit.clamp((int) (performValue / 2), 5)] ;
    return desc+" reception." ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    if (expired()) return 0 ;
    return ROUTINE ;
  }
  
  
  private boolean expired() {
    if (beginTime == -1) return false ;
    return actor.world().currentTime() - beginTime > 10 ;
  }
  
  
  protected Behaviour getNextStep() {
    if (beginTime == -1) beginTime = actor.world().currentTime() ;
    if (expired()) return null ;
    final Action perform = new Action(
      actor, venue,
      this, "actionPerform",
      Action.TALK, "Performing"
    ) ;
    return perform ;
  }
  
  
  public boolean actionPerform(Actor actor, Venue venue) {
    final boolean success = actor.traits.test(employed, 10, 1) ;
    if (success) performValue++ ;
    else if (Rand.yes()) performValue-- ;
    performValue = Visit.clamp(performValue, 0, 10) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public void describeBehaviour(Description d) {
    d.append("Performing "+employed.name+" at ") ;
    d.append(venue) ;
  }
}







