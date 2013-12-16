


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



public class Cantina extends Venue implements Economy {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  final static Model MODEL = ImageModel.asSolidModel(
    Cantina.class, "media/Buildings/merchant/cantina.png", 4, 3
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
    "Misery And Company",
    "The Welcome Fnord",
    "Norusei's Landing",
    "Teller's Afterglow",
    "The Old Touchdown",
    "The Missing Hour",
    "Bailey's Casket",
    "The Duke's Bastard",
  } ;
  
  final static float
    LODGING_PRICE = 20,
    GAMBLE_PRICE  = 50,
    POT_INTERVAL  = 20,
    
    SOMA_MARGIN    = 1.5f,
    GAMBLE_MARGIN  = 0.2f,
    SMUGGLE_MARGIN = 2.0f ;
  
  
  private int nameID = -1 ;
  float gamblePot = 0 ;
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
    s.saveInt(gambleResults.size()) ; for (Actor a : gambleResults.keySet()) {
      s.saveObject(a) ;
      s.saveFloat(gambleResults.get(a)) ;
    }
  }
  
  
  public boolean enterWorldAt(int x, int y, World world) {
    if (! super.enterWorldAt(x, y, world)) return false ;
    nameID = Rand.index(VENUE_NAMES.length) ;
    return true ;
  }
  


  /**  Upgrades, services and economic functions-
    */
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    if (actor.vocation() == Background.SOMA_VENDOR) {
      final Service needed[] = { SOMA, CARBS, PROTEIN } ;
      final Delivery d = Deliveries.nextCollectionFor(
        actor, this, needed, 5, null, world
      ) ;
      if (d != null) return d ;
      return new Supervision(actor, this) ;
    }
    if (actor.vocation() == Background.PERFORMER) {
      final Performance p = new Performance(
        actor, this, Recreation.TYPE_SONG, null
      ) ;
      p.priorityMod = Plan.CASUAL ;
      return p ;
    }
    return null ;
  }
  
  
  public void addServices(Choice choice, Actor forActor) {
    if (personnel.numPresent(Background.PERFORMER) > 0) {
      choice.add(new Recreation(forActor, this, Recreation.TYPE_SONG)) ;
      choice.add(nextGambleFor(forActor)) ;
      choice.add(new Performance(
        forActor, this, Recreation.TYPE_SONG, null
      )) ;
    }
    if (personnel.numPresent(Background.SOMA_VENDOR) > 0) {
      choice.add(nextSomaOrderFor(forActor)) ;
      final Resting resting = new Resting(forActor, this) ;
      resting.cost = (int) LODGING_PRICE ;
      choice.add(resting) ;
    }
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    stocks.forceDemand(SOMA   , 5, VenueStocks.TIER_CONSUMER) ;
    stocks.forceDemand(CARBS  , 5, VenueStocks.TIER_CONSUMER) ;
    stocks.forceDemand(PROTEIN, 5, VenueStocks.TIER_CONSUMER) ;
    updateGambling(numUpdates) ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Background.SOMA_VENDOR, Background.PERFORMER } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.SOMA_VENDOR) return nO + 1 ;
    if (v == Background.PERFORMER  ) return nO + 2 ;
    return 0 ;
  }
  
  
  public float priceFor(Service good) {
    if (good == SOMA) return SOMA.basePrice * SOMA_MARGIN ;
    return good.basePrice * SMUGGLE_MARGIN ;
  }
  
  
  public Service[] services() {
    return new Service[] { SERVICE_PERFORM } ;
  }
  
  
  
  /**  Soma Round implementation-
    */
  private Action nextSomaOrderFor(Actor actor) {
    if (stocks.amountOf(SOMA) <= 0) return null ;
    if (actor.traits.traitLevel(SOMA_HAZE) > 0) return null ;
    final float price = priceFor(SOMA) / 10f ;
    if ((price > actor.gear.credits() / 2) || ! isManned()) return null ;
    
    final Action drops = new Action(
      actor, this,
      this, "actionDropSoma",
      Action.FALL, "Dropping Soma"
    ) ;
    float priority = Action.ROUTINE ;
    priority += actor.traits.traitLevel(DEBAUCHED) / 2f ;
    priority += actor.traits.traitLevel(SOCIABLE)  / 2f ;
    priority += actor.traits.traitLevel(APPETITE)  / 2f ;
    priority -= actor.mind.greedFor((int) price) * Action.ROUTINE ;
    drops.setPriority(priority) ;
    return drops ;
  }
  
  
  public boolean actionDropSoma(Actor actor, Venue venue) {
    final float price = venue.priceFor(SOMA) / 10f ;
    if (price > actor.gear.credits() / 2) return false ;
    venue.stocks.incCredits(price) ;
    actor.gear.incCredits(-price) ;
    stocks.removeItem(Item.withAmount(SOMA, 0.1f)) ;
    actor.traits.incLevel(SOMA_HAZE, 0.1f) ;
    return true ;
  }
  
  
  
  /**  Gambling implementation-
    */
  private Action nextGambleFor(Actor actor) {
    if (isGambling(actor)) return null ;
    final int price = (int) GAMBLE_PRICE ;
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
  
  
  public boolean actionGamble(Actor actor, Cantina venue) {
    final float price = GAMBLE_PRICE ;
    actor.gear.incCredits(-price) ;
    venue.gamblePot += price ;
    venue.stocks.incCredits(price) ;
    
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
  
  
  private void updateGambling(int numUpdates) {
    for (Actor a : gambleResults.keySet()) {
      if (a.aboard() != this) gambleResults.remove(a) ;
    }
    if (numUpdates % POT_INTERVAL == 0) {
      Actor wins = null ;
      float bestResult = 0 ;
      for (Actor gambles : gambleResults.keySet()) {
        final float result = (Float) gambleResults.get(gambles) ;
        if (result > bestResult) { bestResult = result ; wins = gambles ; }
      }
      if (wins != null) {
        float winsShare = gamblePot * (1 - GAMBLE_MARGIN) ;
        wins.gear.incCredits(winsShare) ;
      }
      
      gamblePot = 0 ;
      gambleResults.clear() ;
    }
  }
  
  
  private boolean isGambling(Actor actor) {
    if (gambleResults.get(actor) != null) return true ;
    return false ;
  }
  


  /**  Rendering and interface methods-
    */
  final static float GOOD_DISPLAY_OFFSETS[] = {
    -0.5f, 0,
    -1.0f, 0,
    -1.5f, 0,
  } ;
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS ;
  }
  
  
  protected Service[] goodsToShow() {
    return new Service[] { PROTEIN, CARBS, SOMA } ;
  }
  
  
  protected float goodDisplayAmount(Service good) {
    return Math.min(5, stocks.amountOf(good)) ;
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
      d.append("\n") ;
      Performance.describe(d, "Current music:", Recreation.TYPE_SONG, this) ;
      //
      //  TODO:  Report on gambling, and allow player favourites?  Or a black
      //  market?  Or contacting runners?
      //
      //  ...Oh, wait.  I know what.  Runners are more likely to show up, based
      //  on how much vice you allow.
    }
  }
  
}





/*
//
//  TODO:  Allow *any* good to be purchased here, but at double or triple
//  normal prices.  And it has to be explicitly commissioned, then delivered
//  from off the map by runners.

//
//  Recruit the Runner to make a single delivery, then have them leave the
//  map.
private void listenForDemands() {
  
}
//*/

/*
if (categoryID == 0) {
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
//*/