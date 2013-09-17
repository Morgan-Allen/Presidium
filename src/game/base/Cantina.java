


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;





public class Cantina extends Venue implements BuildConstants {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static Model MODEL = ImageModel.asIsometricModel(
    Cantina.class, "media/Buildings/merchant/cantina.gif", 4, 3
  ) ;
  final static String VENUE_NAMES[] = {
    "The Hive From Home",
    "The Inverse Square",
    "Uncle Fnargex-3Zs",
    "Feynmann's Fortune",
    "The Heavenly Body",
    "The Plug And Play",
    "The Zeroth Point",
    "Lensmans' Folly",
    "The Purple Haze",
  } ;
  
  
  private int nameID = -1 ;//, performID = -1 ;
  
  
  public Cantina(Base base) {
    super(4, 3, Venue.ENTRANCE_SOUTH, base) ;
    structure.setupStats(150, 2, 200, 0, false) ;
    ///personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Cantina(Session s) throws Exception {
    super(s) ;
    nameID = s.loadInt() ;
    //performID = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(nameID) ;
    //s.saveInt(performID) ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    nameID = Rand.index(VENUE_NAMES.length) ;
  }
  


  /**  Upgrades, services and economic functions-
    */
  protected Background[] careers() {
    return new Background[] { Background.SOMA_VENDOR, Background.PERFORMER } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.SOMA_VENDOR) return nO + 1 ;
    if (v == Background.PERFORMER) return nO + 1 ;
    return 0 ;
  }
  

  public Behaviour jobFor(Actor actor) {
    if (actor.vocation() == Background.SOMA_VENDOR) {
      return new Supervision(actor, this) ;
    }
    if (actor.vocation() == Background.PERFORMER) {
      return new Performance(actor, this, ActorConstants.MUSIC_AND_SONG) ;
    }
    return null ;
  }
  
  
  public Service[] services() {
    return new Service[] { SERVICE_PERFORM } ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    stocks.forceDemand(SOMA, 10, 0) ;
  }
  
  
  private Performance performance() {
    for (Actor actor : personnel.workers()) {
      if (actor.aboard() != this) continue ;
      for (Behaviour b : actor.AI.agenda()) if (b instanceof Performance) {
        return (Performance) b ;
      }
    }
    return null ;
  }
  
  
  private Batch <Actor> audience() {
    final Batch <Actor> b = new Batch <Actor> () ;
    for (Mobile m : inside()) if (m instanceof Actor) {
      final Actor a = (Actor) m ;
      if (a.AI.work() != this) b.add(a) ;
    }
    return b ;
  }
  
  
  public float performValue() {
    float value = 0, count = 0 ;
    for (Mobile m : inside()) if (m instanceof Actor) {
      final Actor visits = (Actor) m ;
      Performance p = null ;
      for (Behaviour b : visits.AI.agenda()) if (b instanceof Performance) {
        value += ((Performance) b).performValue() ;
        count++ ;
        break ;
      }
    }
    if (count == 0) return 0 ;
    value /= count ;
    value *= 1 + ((count - 1) / 2f) ;
    return value ;
  }
  
  
  


  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/cantina_button.gif") ;
  }
  
  
  public String fullName() {
    if (nameID == -1) return "Cantina" ;
    return VENUE_NAMES[nameID] ;
  }
  
  
  public String helpInfo() {
    return
      "Citizens can seek lodgings or simply rest and relax at the Cantina, "+
      "which serves as both a social focal point and a potential breeding "+
      "ground for criminal activities." ;
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_AESTHETE ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    
    
    if (categoryID == 0) {
      super.writeInformation(d, categoryID, UI) ;
      
      final Batch <Actor> audience = audience() ;
      final Performance p = this.performance() ;
      d.append("\n\nCurrent performance:\n  ") ;
      
      if (p == null) d.append("No performance.") ;
      else d.append(p.performDesc()) ;
      d.append("\n  ") ;
      if (audience.size() == 0) d.append("No audience.") ;
      else {
        if (p != null) d.append(p.qualityDesc()) ;
        d.append("\n\nAudience:") ;
        for (Actor a : audience) {
          d.append("\n  ") ;
          d.append(a) ;
        }
      }
    }
    else super.writeInformation(d, categoryID, UI) ;
    //
    //  Enable gambling/games of chance/cards.
    //  ...Select participants from among the visitors, and place your bets
    //  against them.  The odds of their victory are based on insight scores,
    //  sleight of hand, et cetera.  Everyone puts in money, whoever wins takes
    //  the pot, minus a slice for the house.
    
    
    //
    //  Enable chance meetings with Runners, based on visitors looking for an
    //  illegal good or service.  You could theoretically hire them for a
    //  mission or two yourself...
  }
  
}







