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
//  ...I need to start working on the 'hero' side of things.  Who really
//  qualifies at the moment?
//      Volunteers/Mercs & Mech Pilots (the MCHQ or House Garrison)
//      Runners/Mercs & Enforcers (the Cantina & Enforcer Bloc)
//      Guerillas & Medics (the Survey Post & Sickbay)
//      Tech Reserve & Psyon (the Town Vault & Psyon Choir)
//  ...These are the folks able to fight, but not necessarily devoted to it.
//  You can call on them to get missions done.

/*
   TODO:  TUTORIAL MISSION
   Contact/relations & danger map use/long-range pathing cache.
   Stock exchange & internal demand bug.
   Walls, arcology and causeways need art/functions tweaked or redone.
   General polish for mines, the surveyor, maybe the cantina & holo arcade.
   Housing stability, and funds deduction during construction.
//*/
//
//  TODO:  Test explorer hunting.



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
    TG.setupMinerals(world, 0.5f, 1, 0.5f) ;
    TG.setupOutcrops(world) ;
    return world ;
  }
  
  
  public void updateGameState() {
    super.updateGameState() ;
    //PlayLoop.setGameSpeed(0.25f) ;
    //PlayLoop.setGameSpeed(5.0f) ;
  }
  
  
  protected void configureScenario(World world, Base base, BaseUI UI) {
    //securityScenario(world, base, UI) ;
    contactScenario(world, base, UI) ;
  }
  
  
  private void securityScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree   = true ;
    GameSettings.buildFree = true ;
    GameSettings.hireFree  = true ;
    GameSettings.psyFree   = true ;
    //
    //  Step 1:  Generate a structure, generate some actors around it, create
    //  a security mission for the structure, and assign the actors to it.
    Venue defended = Placement.establishVenue(
      new Sickbay(base), 10, 10, true, world
    ) ;
    final Mission mission = new SecurityMission(base, defended) ;
    mission.assignReward(1000) ;
    base.addMission(mission) ;
    for (int n = 5 ; n-- > 0 ;) {
      final Actor defends = new Human(Background.VETERAN, base) ;
      Tile free = Spacing.pickRandomTile(defended, 10, world) ;
      free = Spacing.nearestOpenTile(free, defended) ;
      if (free == null) continue ;
      defends.enterWorldAt(free, world) ;
      mission.setApplicant(defends, true) ;
      mission.setApprovalFor(defends, true) ;
    }
    mission.beginMission() ;
    //
    //  Step 2:  Generate one or more hostile actors, and have them try to
    //  assault the structure.
    final Base artilects = world.baseWithName(Base.KEY_ARTILECTS, true, true) ;
    final Venue lair = Placement.establishVenue(
      new Ruins(), 25, 25, true, world
    ) ;
    lair.assignBase(artilects) ;
    for (int n = 2 ; n-- > 0 ;) {
      final Actor assaults = new Tripod() ;
      assaults.assignBase(artilects) ;
      assaults.mind.setHome(lair) ;
      assaults.enterWorldAt(lair, world) ;
      assaults.goAboard(lair, world) ;
      UI.selection.pushSelection(assaults, true) ;
    }
    artilects.setRelation(base, -1) ;
    base.setRelation(artilects, -1) ;
  }
  
  
  private void contactScenario(World world, Base base, BaseUI UI) {
    GameSettings.fogFree   = true ;
    GameSettings.buildFree = true ;
    GameSettings.hireFree  = true ;
    GameSettings.psyFree   = true ;
    //
    //  Generate a basic native settlement in the far corner of the map.
    final Base natives = world.baseWithName(Base.KEY_NATIVES, true, true) ;
    final Venue huts = Placement.establishVenue(
      new NativeHut(Habitat.ESTUARY, natives), 21, 21, true, world
    ) ;
    for (int n = 5 ; n-- > 0 ;) {
      final Career c = new Career(
        Rand.yes(), Background.HUNTER,
        Background.NATIVE_BIRTH,
        Background.PLANET_DIAPSOR
      ) ;
      final Actor lives = new Human(c, natives) ;
      lives.mind.setHome(huts) ;
      lives.mind.setWork(huts) ;
      lives.enterWorldAt(huts, world) ;
      lives.goAboard(huts, world) ;
    }
    natives.setRelation(base, -0.2f) ;
    base.setRelation(natives, -0.2f) ;
    //
    //  Generate a basic settlement for the home team, and some diplomacy-
    //  oriented inhabitants, and see how they get on.
    final ContactMission mission = new ContactMission(base, huts) ;
    mission.assignReward(1000) ;
    base.addMission(mission) ;
    final Venue exchange = Placement.establishVenue(
      new StockExchange(base), 2, 2, true, world
    ) ;
    exchange.stocks.bumpItem(PROTEIN, 20) ;
    exchange.stocks.bumpItem(GREENS, 10) ;
    exchange.stocks.bumpItem(SPICE, 5) ;
    final Venue station = new SurveyStation(base) ;
    for (int n = 2 ; n-- > 0 ;) {
      final Actor talks = new Human(Background.SURVEY_SCOUT, base) ;
      talks.mind.setWork(station) ;
      talks.enterWorldAt(9, 4, world) ;
      mission.setApplicant(talks, true) ;
      mission.setApprovalFor(talks, true) ;
    }
    Placement.establishVenue(station, 7, 2, true, world) ;
    mission.beginMission() ;
  }
}





/*
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

//*/


