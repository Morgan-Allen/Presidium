


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



//
//  TODO:  The Cantina should appear by itself once your settlement gets big
//  enough.  It counts as private property.  (Which is why you can't get rid of
//  the criminal element!)
//
//  TODO:  Citizens should be able to dine here while relaxing (for a price...)


public class Cantina extends Venue implements Economy {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static Model MODEL = ImageModel.asSolidModel(
    Cantina.class, "media/Buildings/merchant/cantina.gif", 4, 3
  ) ;
  final static String VENUE_NAMES[] = {
    "The Hive From Home",
    "The Square In Verse",
    "Uncle Fnargex-3Zs",
    "Feynmann's Fortune",
    "The Heavenly Body",
    "The Plug And Play",
    "The Zeroth Point",
    "Lensmans' Folly",
    "The Purple Haze",
    "The Moving Earth",
    "Eisley's Franchise",
    "The Silver Pill",
    "The Happy Morlock",
    "Misery Loves Sompany",
    "The Welcome Fnord",
    "Nordsei's Landing",
    "Teller's Afterglow",
    "The Touchdown",
    "The Missing Hour",
    "Bailey's Casket",
    "The Bastard House",
  } ;
  
  final static float
    LODGING_PRICE = 20,
    
    GAMBLE_PRICE  = 50,
    BET_SMALL     = 20,
    BET_LARGE     = 50,
    POT_INTERVAL  = 20,
    
    SOMA_MARGIN   = 1.5f,
    GAMBLE_MARGIN = 0.2f ;
  
  
  private int nameID = -1 ;
  
  float gamblePot = 0 ;
  Actor playerBets = null ;
  int playerBetSize = 0 ;
  final Table <Actor, Float> gambleResults = new Table <Actor, Float> () ;
  
  
  public Cantina(Base base) {
    super(4, 3, Venue.ENTRANCE_SOUTH, base) ;
    structure.setupStats(150, 2, 200, 0, Structure.TYPE_VENUE) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Cantina(Session s) throws Exception {
    super(s) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    nameID = s.loadInt() ;
    gamblePot = s.loadFloat() ;
    playerBets = (Actor) s.loadObject() ;
    playerBetSize = s.loadInt() ;
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Actor a = (Actor) s.loadObject() ;
      final float f = s.loadFloat() ;
      gambleResults.put(a, f) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(nameID) ;
    s.saveFloat(gamblePot) ;
    s.saveObject(playerBets) ;
    s.saveInt(playerBetSize) ;
    s.saveInt(gambleResults.size()) ; for (Actor a : gambleResults.keySet()) {
      s.saveObject(a) ;
      s.saveFloat(gambleResults.get(a)) ;
    }
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    nameID = Rand.index(VENUE_NAMES.length) ;
  }
  


  /**  Upgrades, services and economic functions-
    */
  public Behaviour jobFor(Actor actor) {
    if (actor.vocation() == Background.SOMA_VENDOR) {
      final Service needed[] = { SOMA, CARBS, PROTEIN, GREENS } ;
      final Delivery d = Deliveries.nextCollectionFor(
        actor, this, needed, 5, null, world
      ) ;
      ///I.say("Next delivery is: "+d) ;
      if (d != null) return d ;
      return new Supervision(actor, this) ;
    }
    if (actor.vocation() == Background.PERFORMER) {
      return new Performance(actor, this, Aptitudes.MUSIC_AND_SONG) ;
    }
    return null ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    stocks.forceDemand(SOMA   , 5, VenueStocks.TIER_CONSUMER) ;
    stocks.forceDemand(CARBS  , 5, VenueStocks.TIER_CONSUMER) ;
    stocks.forceDemand(PROTEIN, 5, VenueStocks.TIER_CONSUMER) ;
    stocks.forceDemand(GREENS , 5, VenueStocks.TIER_CONSUMER) ;
    if (numUpdates % POT_INTERVAL == 0 && isManned()) splitGamblePot() ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.SOMA_VENDOR, Background.PERFORMER } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.SOMA_VENDOR) return nO + 1 ;
    if (v == Background.PERFORMER) return nO + 1 ;
    return 0 ;
  }
  
  //
  //  TODO:  Allow *any* good to be purchased here, but at double or triple
  //  normal prices.  And it has to be explicitly commissioned, then delivered
  //  from off the map by runners.
  public Service[] services() {
    //return ALL_UNIQUE_ITEMS ;
    return new Service[] { SERVICE_PERFORM } ;
  }
  
  
  public float priceLodgings() {
    return 20 ;
  }
  
  
  public float priceSoma() {
    return priceFor(SOMA) * 1.5f ;
  }
  
  
  public float priceBet() {
    return GAMBLE_PRICE ;
  }
  
  
  
  /**  Performance implementation-
    */
  private Performance performance() {
    for (Actor actor : personnel.workers()) {
      if (actor.aboard() != this) continue ;
      for (Behaviour b : actor.mind.agenda()) if (b instanceof Performance) {
        return (Performance) b ;
      }
    }
    return null ;
  }
  
  
  private Batch <Actor> audience() {
    final Batch <Actor> b = new Batch <Actor> () ;
    for (Mobile m : inside()) if (m instanceof Actor) {
      final Actor a = (Actor) m ;
      if (a.mind.work() != this) b.add(a) ;
    }
    return b ;
  }
  
  
  public float performValue() {
    float value = 0, count = 0 ;
    for (Mobile m : inside()) if (m instanceof Actor) {
      final Actor visits = (Actor) m ;
      Performance p = null ;
      for (Behaviour b : visits.mind.agenda()) if (b instanceof Performance) {
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
  
  
  
  /**  Gambling implementation-
    */
  public Action nextGambleFor(Actor actor) {
    if (isGambling(actor)) return null ;
    final int price = (int) priceBet() ;
    if ((price > actor.gear.credits() / 2) || ! isManned()) return null ;
    final Action gamble = new Action(
      actor, this,
      this, "actionGamble",
      Action.TALK_LONG, "Gambling"
    ) ;
    float priority = Rand.index(5) ;
    priority += actor.traits.traitLevel(OPTIMISTIC) * Rand.num() ;
    priority -= actor.traits.traitLevel(NERVOUS)    * Rand.num() ;
    priority -= actor.traits.traitLevel(STUBBORN)   * Rand.num() ;
    priority -= actor.mind.greedFor(price) * Action.ROUTINE ;
    gamble.setPriority(priority) ;
    return gamble ;
  }
  
  
  public boolean isGambling(Actor actor) {
    if (gambleResults.keySet().contains(actor)) return true ;
    return false ;
  }
  
  
  public boolean actionGamble(Actor actor, Cantina venue) {
    final float price = priceBet() ;
    actor.gear.incCredits(-price) ;
    venue.gamblePot += price ;
    
    if (actor == playerBets) {
      base().incCredits(-playerBetSize) ;
      venue.gamblePot += playerBetSize ;
    }
    
    float success = (Rand.num() * 2) - 1 ;
    if (actor.traits.test(ACCOUNTING, MODERATE_DC, 1)) success++ ;
    else success-- ;
    if (actor.traits.test(MASQUERADE, MODERATE_DC, 1)) success++ ;
    else success-- ;
    
    if (success > 0) {
      gambleResults.put(actor, success) ;
      return true ;
    }
    else {
      gambleResults.put(actor, -1f) ;
      return false ;
    }
  }
  
  
  private void splitGamblePot() {
    if (gambleResults.size() == 0) return ;
    
    Actor wins = null ;
    float bestResult = Float.NEGATIVE_INFINITY ;
    
    for (Actor gambles : gambleResults.keySet()) {
      if (gambles.aboard() != this) continue ;
      final float result = (Float) gambleResults.get(gambles) ;
      if (result > bestResult) { bestResult = result ; wins = gambles ; }
    }
    
    if (wins != null) {
      if (wins == playerBets) {
        float playerShare = playerBetSize * gambleResults.size() ;
        playerShare *= 1 - GAMBLE_MARGIN ;
        base().incCredits(playerShare) ;
        gamblePot -= playerShare ;
      }
      float winsShare = gamblePot * (1 - GAMBLE_MARGIN) ;
      wins.gear.incCredits(winsShare) ;
      gamblePot -= winsShare ;
    }
    
    stocks.incCredits(gamblePot) ;
    gamblePot = 0 ;
    playerBets = null ;
    gambleResults.clear() ;
  }
  
  


  /**  Rendering and interface methods-
    */
  final static float GOOD_DISPLAY_OFFSETS[] = {
    -0.00f, 1.0f,
    -0.00f, 1.5f,
    -0.00f, 2.0f,
    -0.00f, 2.5f,
  } ;
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS ;
  }
  
  
  protected Service[] goodsToShow() {
    return new Service[] { GREENS, PROTEIN, CARBS, SOMA } ;
  }
  
  
  protected float goodDisplayAmount(Service good) {
    return stocks.amountOf(good) * 2 ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/cantina_button.gif") ;
  }
  
  
  public String fullName() {
    if (nameID == -1) return "Cantina" ;
    return VENUE_NAMES[nameID] ;
  }
  
  
  public String helpInfo() {
    return
      "Citizens can seek lodgings or simply rest and relax at the Cantina. "+
      "Though a lively hub for social activities, something about the "+
      "atmosphere also tends to attract the criminal element." ;
  }
  

  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    if (categoryID == 0) {
      //
      //  Report information on the current performance-
      final Batch <Actor> audience = audience() ;
      final Performance p = performance() ;
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
      //
      //  List the current gambling participants, and allow the player to bet
      //  on one of them.
      d.append("\n\nGambling:") ;
      for (final Actor gambles : gambleResults.keySet()) {
        d.append("\n  ") ; d.append(gambles) ;
        d.append(new Description.Link("  SMALL BET") {
          public void whenClicked() {
            playerBets = gambles ;
            playerBetSize = (int) BET_SMALL ;
          }
        }) ;
        d.append(new Description.Link("  LARGE BET") {
          public void whenClicked() {
            playerBets = gambles ;
            playerBetSize = (int) BET_LARGE ;
          }
        }) ;
      }
      if (gambleResults.size() == 0) {
        d.append("\n  No gambling at present.") ;
      }
      else if (playerBets != null) {
        d.append("\n\n  Betting "+playerBetSize+" each round ") ;
        d.append(playerBets) ;
        d.append(new Description.Link("\n  CLEAR BETS") {
          public void whenClicked() {
            playerBets = null ;
            playerBetSize = 0 ;
          }
        }) ;
      }
    }
  }
  
}







