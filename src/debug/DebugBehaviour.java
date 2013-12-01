/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.debug ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.game.wild.* ;
import src.game.campaign.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



//
//  TODO:  Contact missions, holo-theatre, and ambience.  Those are the ones.

//
//  TODO:  If you lack funding for construction, buildings need to display an
//  error message.

//
//  TODO:  Test out the archives, finish the surveillance post, and polish the
//  arcology/edifice/arcade thing.  Test out Research and the HoloArcade.
//
//  Have pollution/terraforming gradually change the landscape.  Or at least
//  represent squalor correctly.  Also, finish up mining mechanics and the
//  causeway.
//
//  Spruce up the ambience/aesthetics structures, including biomass FX.
//
//  Tweak mechanics for diplomacy and citizen mood.  (Good relations are way
//  too easy/quick at the moment.)  Actors should call for help from allies,
//  and need proper line of sight.  Add Security and Contact missions.  Have
//  missions modify choice priorities?


public class DebugBehaviour extends Scenario implements Economy {
  
  
  
  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    if (Scenario.loadedFrom("test_behaviour")) return ;
    DebugBehaviour test = new DebugBehaviour() ;
    PlayLoop.setupAndLoop(test.UI(), test) ;
  }
  
  
  protected DebugBehaviour() {
    super("test_behaviour", true) ;
  }
  
  
  public DebugBehaviour(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Setup and updates-
    */
  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      32, 0.2f,
      Habitat.MEADOW , 0.7f,
      Habitat.BARRENS, 0.3f
    ) ;
    final World world = new World(TG.generateTerrain()) ;
    TG.setupMinerals(world, 0, 0, 0) ;
    //TG.setupOutcrops(world) ;
    return world ;
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI HUD) {
    combatScenario(world, base, UI()) ;
  }
  
  
  //
  //  ...Okay.  This will clearly take some time.
  private void combatScenario(World world, Base base, BaseUI UI) {
    
    //GameSettings.buildFree = true ;
    //base.incCredits(4000) ;
    GameSettings.fogFree = true ;
    
    Tripod tripod = new Tripod() ;
    tripod.health.setupHealth(0.5f, 1, 0) ;
    tripod.enterWorldAt(31, 31, world) ;
    
    int numH = 5 ;
    Human humans[] = new Human[numH] ;
    while (numH-- > 0) {
      humans[numH] = new Human(Background.VOLUNTEER, base) ;
    }
    Scenario.establishVenue(new Bastion(base), 4, 4, true, world, humans) ;
    
    
    //Micovore micovore = new Micovore() ;
    //micovore.health.setupHealth(0.5f, 1, 0) ;
    //micovore.enterWorldAt(9, 9, world) ;
    
    UI.selection.pushSelection(humans[0], true) ;
  }
}



  /*
  /**  Testing out interactions between alien creatures or primitive humanoids.
  private void natureScenario(World world, Base base, HUD UI) {
    GameSettings.fogFree = true ;
    PlayLoop.setGameSpeed(5.0f) ;
    
    final EcologyGen EG = new EcologyGen(world) ;
    EG.populateFlora(world) ;
    
    final Actor hunter = new Micovore() ;
    hunter.health.setupHealth(0.5f, 1, 0) ;
    hunter.enterWorldAt(6, 6, world) ;
    ((BaseUI) UI).selection.pushSelection(hunter, true) ;
    
    for (int n = 16 ; n-- > 0 ;) {
      final Actor prey = Rand.yes() ? new Quud() : new Vareen() ;
      final Tile e = Spacing.pickRandomTile(world.tileAt(16, 16), 8, world) ;
      prey.health.setupHealth(Rand.num(), 1, 0) ;
      prey.enterWorldAt(e.x, e.y, world) ;
    }
  }
  
  
  
  /**  These are scenarios associated with upkeep, maintenance and
    *  construction of the settlement-
  private void baseScenario(World world, Base base, HUD UI) {
    
    ///PlayLoop.rendering().port.cameraZoom = 1.33f ;
    base.incCredits(10000) ;
    base.commerce.assignHomeworld(Background.PLANET_HALIBAN) ;
    final Bastion bastion = new Bastion(base) ;
    
    //
    //  TODO:  Aristocrats like these might need different behavioural routines.
    final Human knight = new Human(Background.KNIGHTED, base) ;
    Human spouse = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    for (int n = 10 ; n-- > 0 ;) {
      final Human match = new Human(Background.FIRST_CONSORT, base) ;
      final float rating =
        knight.mind.attraction(match) +
        (match.mind.attraction(knight) / 2) ;
      if (rating > bestRating) { spouse = match ; bestRating = rating ; }
    }
    
    knight.mind.setHomeVenue(bastion) ;
    spouse.mind.setHomeVenue(bastion) ;
    final int initTime = 0 - Rand.index(100) ;
    final float relation = bestRating * Rand.avgNums(2) ;
    knight.mind.setRelation(spouse, relation, initTime) ;
    spouse.mind.setRelation(knight, relation, initTime) ;
    
    base.assignRuler(knight) ;
    Scenario.establishVenue(bastion, 9, 9, true, world, knight, spouse) ;
    ((BaseUI) UI).selection.pushSelection(knight, true) ;
    
    Scenario.establishVenue(new SupplyDepot(base), 20, 10, true, world) ;
  }
  
  
  
  /**  Testing out directed behaviour like combat, exploration, security or
    *  contact missions.
  private void missionScenario(World world, Base base, HUD UI) {
    
    GameSettings.fogFree = true ;
    //GameSettings.hireFree = true ;
    PlayLoop.setGameSpeed(1.0f) ;
    
    final Base otherBase = Base.createFor(world) ;
    world.registerBase(otherBase, true) ;
    base.setRelation(otherBase, -1) ;
    otherBase.setRelation(base, -1) ;
    otherBase.colour = Colour.CYAN ;
    
    final Batch <Actor> allies = new Batch <Actor> () ;
    float sumPower = 0 ;
    for (int n = 5 ; n-- > 0 ;) {
      final Actor actor = new Human(Background.EXPLORER, base) ;
      actor.setPosition(
        24 + Rand.range(-4, 4),
        24 + Rand.range(-4, 4),
        world
      ) ;
      actor.enterWorld() ;
      allies.add(actor) ;
      sumPower += Combat.combatStrength(actor, null) ;
      ///establishVenue(new SurveyorRedoubt(base), 4, 4, true, actor) ;
    }
    ///I.say("TOTAL POWER OF ALLIES: "+sumPower) ;
    
    //*
    final EcologyGen EG = new EcologyGen(world) ;
    //final Batch <Ruins> ruins = EG.populateRuins(world.tileAt(8, 8), 16) ;
    //EG.populateArtilects(ruins, world) ;
    EG.populateFlora(world) ;
    //*/
    
    /*
    final Actor enemy = new Tripod() ;
    //enemy.assignBase(otherBase) ;
    enemy.enterWorldAt(24, 24, world) ;
    ((BaseUI) UI).selection.pushSelection(enemy, true) ;
    
    I.say("POWER OF ENEMY: "+Combat.combatStrength(enemy, null)) ;
    //*/
    //((BaseUI) UI).selection.pushSelection(allies.atIndex(0), true) ;
  /*
  }
  
  
  
  /**  Testing out pro-social behaviour like dialogue, recreation and medical
    *  treatment.
  private void socialScenario(final World world, Base base, HUD UI) {
    
    //
    //  TODO:  Transfer to DebugSituation.
    
    base.incCredits(10000) ;
    GameSettings.fogFree = true ;
    GameSettings.buildFree = true ;
    GameSettings.hireFree = true ;
    
    final Actor actor = new Human(Background.PHYSICIAN, base) ;
    final Actor other = new Human(Background.VETERAN  , base) ;
    other.health.takeInjury(other.health.maxHealth()) ;
    
    other.enterWorldAt(15, 6, world) ;
    final Sickbay sickbay = new Sickbay(base) ;
    Scenario.establishVenue(sickbay, 9, 2, true, world, actor) ;
    Scenario.establishVenue(new CultureVats(base), 9, 8, true, world) ;
    Scenario.establishVenue(new VaultSystem(base), 3, 5, true, world) ;
    
    sickbay.stocks.bumpItem(STIM_KITS, 5) ;
    sickbay.stocks.bumpItem(MEDICINE , 5) ;
    ((BaseUI) UI).selection.pushSelection(other, true) ;
  }
  
  
  /*
  public static Venue establishVenue(
    Venue v, int atX, int atY, boolean intact,
    Actor... employed
  ) {
    final World world = PlayLoop.currentScenario().world ;
    v.enterWorldAt(atX, atY, world) ;
    if (intact) {
      v.structure.setState(Structure.STATE_INTACT, 1.0f) ;
      v.onCompletion() ;
    }
    else {
      v.structure.setState(Structure.STATE_INSTALL, 0.0f) ;
    }
    final Tile e = world.tileAt(v) ;
    for (Actor a : employed) {
      a.mind.setEmployer(v) ;
      if (! a.inWorld()) {
        a.enterWorldAt(e.x, e.y, world) ;
        a.goAboard(v, world) ;
      }
    }
    if (GameSettings.hireFree) VenuePersonnel.fillVacancies(v) ;
    v.setAsEstablished(true) ;
    return v ;
  }
  //*/
//}


