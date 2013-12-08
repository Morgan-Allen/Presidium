


package src.game.social ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.wild.* ;
import src.game.building.* ;
import src.game.common.* ;
//import src.game.common.Session.Saveable ;
import src.user.Description;
import src.util.* ;



public class FindHome extends Plan implements Economy {
  
  
  private static boolean verbose = false ;
  


  public FindHome(Actor actor) {
    super(actor) ;
  }


  public FindHome(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  

  public float priorityFor(Actor actor) {
    if (actor.mind.home() == null) return ROUTINE ;
    return 0 ;
  }
  
  
  protected Behaviour getNextStep() {
    return null ;
  }
  
  
  public void describeBehaviour(Description d) {
  }
  
  
  
  /**  Static helper methods for home placement/location-
    */
  //
  //  ...No.
  public static interface Residence {
    
  }
  
  
  
  public static Holding lookForHome(Human client, Base base) {
    final World world = base.world ;
    final Venue oldHome = client.mind.home() ;
    
    Holding best = null ;
    float bestRating = 0 ;
    
    //
    //  TODO:  Also, Native Huts and the Bastion need to count here!
    
    if (oldHome instanceof Holding) {
      final Holding h = (Holding) oldHome ;
      best = h ;
      bestRating = rateHolding(client, h) * 2f ;
    }
    
    for (Object o : world.presences.sampleFromKey(
      client, world, 3, null, Holding.class
    )) {
      final Holding h = (Holding) o ;
      final float rating = rateHolding(client, h) ;
      if (rating > bestRating) { bestRating = rating ; best = h ; }
    }

    //
    //  TODO:  You need to allow for construction of native hutments if there's
    //  no more conventional refuge available-
    
    if (best == null || Rand.index(10) == 0) {
      final Venue refuge = (Venue) world.presences.nearestMatch(
        SERVICE_REFUGE, client, World.SECTOR_SIZE
      ) ;
      final Holding h = (refuge == null || refuge.base() != client.base()) ?
        null : newHoldingFor(client) ;  //  Use newHutFor(client)!
      final float rating = rateHolding(client, h) ;
      if (rating > bestRating) { bestRating = rating ; best = h ; }
    }
    
    if (verbose && I.talkAbout == client) {
      I.say("Looking for home, best site: "+best) ;
    }
    
    if (best != null && best != oldHome) {
      if (! best.inWorld()) {
        best.doPlace(best.origin(), null) ;
      }
      client.mind.setHomeVenue(best) ;
      return best ;
    }
    else return null ;
  }
  
  
  private static Holding newHoldingFor(Actor client) {
    final World world = client.world() ;
    final int maxDist = World.SECTOR_SIZE ;
    final Holding holding = new Holding(client.base()) ;
    final Tile origin = searchPoint(client) ;
    final Vars.Bool found = new Vars.Bool() ;
    
    final TileSpread spread = new TileSpread(origin) {
      
      protected boolean canAccess(Tile t) {
        if (Spacing.distance(t, origin) > maxDist) return false ;
        return ! t.blocked() ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        holding.setPosition(t.x, t.y, world) ;
        if (holding.canPlace()) { found.val = true ; return true ; }
        return false ;
      }
    } ;
    spread.doSearch() ;
    
    if (found.val == true) return holding ;
    else return null ;
  }
  
  
  private static NativeHut newHutFor(Actor client) {
    return null ;
  }
  
  
  private static Tile searchPoint(Actor client) {
    if (client.mind.work() instanceof Venue) {
      return ((Venue) client.mind.work()).mainEntrance() ;
    }
    return client.origin() ;
  }


  private static float rateHolding(Actor actor, Holding holding) {
    if (holding == null || holding.base() != actor.base()) return -1 ;
    float rating = 1 ;
    
    final int
      UL = holding.upgradeLevel(),
      maxPop = HoldingUpgrades.OCCUPANCIES[UL] ;
    float crowding = holding.personnel.residents().size() * 1f / maxPop ;
    
    //
    //  TODO:  Base this in part on relations with other residents, possibly of
    //  an explicitly sexual/familial nature?  Actual housewife/husbander
    //  status?
    
    rating *= (UL + 1) * (2f - crowding) ;
    if (holding.inWorld()) rating += 0.5f ;
    rating -= actor.mind.greedFor(HoldingUpgrades.TAX_LEVELS[UL]) * 5 ;
    rating -= Plan.rangePenalty(actor.mind.work(), holding) ;
    ///I.say("  Rating for holding is: "+rating) ;
    return rating ;
  }
  
}






/*
//
//  TODO:  You'll need to apply for a home at a nearby refuge-structure
//  instead.  Either that, or try building a hut from local materials
//  using handicrafts.
if (this.home == null && (work == null || work instanceof Venue)) {
  final Holding newHome = Holding.findHoldingFor(actor) ;
  if (newHome != null) {
    if (! newHome.inWorld()) newHome.doPlace(newHome.origin(), null) ;
    setHomeVenue(newHome) ;
  }
}
//*/