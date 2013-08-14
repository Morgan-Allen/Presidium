



package src.game.tactical ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.util.* ;



public class Retreat implements ActorConstants {
  
  
  //
  //  Retreat should become less attractive the closer you are to home?  Well,
  //  more so the further you are from a safe haven, anyway.  I dunno.
  
  
  
  public static Venue nearestHaven(Actor actor, Class prefClass) {
    final Presences p = actor.world().presences ;
    int numC = (int) (actor.traits.trueLevel(INSIGHT) / 3) ;
    
    Object picked = null ;
    float bestRating = 0 ;
    int numChecked = 0 ;
    
    for (Object t : p.matchesNear(actor.base(), actor, -1)) {
      if (numChecked++ > numC) break ;
      float rating = rateHaven(t, actor, prefClass) ;
      if (rating > bestRating) { bestRating = rating ; picked = t ; }
    }
    numChecked = 0 ;
    for (Object t : p.matchesNear(prefClass, actor, -1)) {
      if (numChecked++ > numC) break ;
      float rating = rateHaven(t, actor, prefClass) ;
      if (rating > bestRating) { bestRating = rating ; picked = t ; }
    }
    return (Venue) picked ;
  }
  
  
  private static float rateHaven(Object t, Actor actor, Class prefClass) {
    //
    //  TODO:  Don't pick anything too close by either.  That'll be in a
    //  dangerous area.
    if (! (t instanceof Venue)) return -1 ;
    final Venue haven = (Venue) t ;
    float rating = 1 ;
    if (haven.getClass() == prefClass) rating *= 2 ;
    if (haven.base() == actor.base()) rating *= 2 ;
    final int SS = Terrain.SECTOR_SIZE ;
    rating *= SS / (SS + Spacing.distance(actor, haven)) ;
    return rating ;
  }
  
  
  
  /**  These are general evaluation and utility methods.
    */
  /*
  public static float dangerAtSpot(Target spot, Actor actor) {
    //
    //  Get a reading of threats based on all actors visible to this one, and
    //  their distance from the spot in question.
    float seenDanger = 0 ;
    final float range = Sector.SECTOR_SIZE / 2 ;
    for (Actor near : actor.AI.seenActors()) {
      if (near == actor) continue ;
      ///I.say("    "+near+" might threaten "+actor) ;
      float danger = near.health.combatStrength() ;
      float attitude = near.AI.attitudeTo(actor) ;
      if (! near.AI.attentionOn(actor, Combat.class)) {
        danger /= 2 ;
        if (near.AI.attentionOn(null, Retreat.class)) danger /= 2 ;
        danger *= Visit.clamp(attitude, -1, 1) ;
      }
      final float dist = Visit.clamp(World.distance(spot, near), 0, range) ;
      danger *= 1 - (dist / range) ;
      seenDanger += danger ;
    }
    if (seenDanger == 0) return 0 ;
    //
    //  Get a general reading based on the ambient 'danger level' of this
    //  location.
    float areaDanger = 0 ;
    final Tile t = actor.world.tileAt(spot) ;
    for (Realm realm : actor.world.allRealms()) {
      final int status = realm.relationTo(actor.realm()) ;
      final float power = realm.powerMap.valueAt(t.x, t.y) ;
      if (status == Realm.ALLIANCE) areaDanger -= power ;
      if (status == Realm.AT_WAR) areaDanger += power ;
    }
    return (areaDanger + seenDanger) / actor.health.combatStrength() ;
  }

  public static Tile pickRandomTile(Actor actor, float range) {
    final double angle = Rand.num() * Math.PI ;
    final float dist = Rand.num() * range ;
    return actor.world.tileAt(
      actor.cornerX() + (float) (Math.cos(angle) * dist),
      actor.cornerY() + (float) (Math.sin(angle) * dist)
    ) ;
  }

  public static Tile pickWithdrawPoint(Actor actor) {
    return pickWithdrawPoint(actor, actor.health.sightRange()) ;
  }
  
  public static Tile pickWithdrawPoint(Actor actor, float range) {
    //
    //  Firstly, set up data caches, and rate the actor's current tile-
    final int numPicks = 3 ;
    Tile pick = actor.world.tileAt(actor) ;
    float bestRating = dangerAtSpot(pick, actor) ;
    //
    //  Now pick other tiles at random, and compare them-
    for (int i = numPicks ; i-- > 0 ;) {
      final Tile tried = pickRandomTile(actor, range) ;
      final float tryRating = dangerAtSpot(tried, actor) ;
      ///I.say("    Evaluating point: "+tried+", danger: "+tryRating) ;
      if (tryRating < bestRating) { bestRating = tryRating ; pick = tried ; }
    }
    return pick == actor.world.tileAt(actor) ? null : pick ;
  }
  
  
  public static Target pickShelter(Actor actor) {
    Target picked = actor.realm().nearestVenueTo(actor) ;
    if (picked == null) picked = pickWithdrawPoint(actor) ;
    if (picked == null) picked = actor.location() ;
    return picked ;
  }
  //*/
}







