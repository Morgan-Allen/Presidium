/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.wild ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.game.building.* ;
import src.util.* ;



public class Micovore extends Fauna {
  
  
  
  /**  Constructors, setup and save/load methods-
    */
  public Micovore() {
    super(Species.MICOVORE) ;
  }
  
  
  public Micovore(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  protected void initStats() {
    traits.initAtts(20, 15, 5) ;
    health.initStats(
      20,  //lifespan
      2.5f,//bulk bonus
      1.5f,//sight range
      1.3f //move speed
    ) ;
    gear.setDamage(15) ;
    gear.setArmour(5) ;
  }
  
  
  public float radius() {
    return 0.5f ;
  }
  
  
  
  /**  Supplemental behaviour methods-
    */
  protected Behaviour nextDefence(Actor near) {
    final Choice choice = new Choice(this) ;
    choice.add(new Retreat(this)) ;
    if (near != null) choice.add(new Combat(this, near)) ;
    return choice.weightedPick(1) ;
  }
  
  
  protected void addChoices(Choice choice) {
    super.addChoices(choice) ;
    
    //
    //  Determine whether you should fight with others of your kind-
    final int range = species.forageRange() ;
    final Ecology ecology = world.ecology() ;
    float crowding = ecology.relativeAbundanceAt(species, origin(), range) ;
    crowding = Visit.clamp(crowding - 1, 0, 10) ;
    crowding *= 1 - health.energyLevel() ;
    final Fauna fights = findCompetition() ;
    
    if (fights != null && crowding > 1) {
      ///I.say(this+" FIGHTING PRIORITY X5: "+crowding) ;
      final Combat fighting = new Combat(this, fights) ;
      fighting.priorityMod = (crowding - 1) * Plan.URGENT ;
      choice.add(fighting) ;
    }
    //
    //  Determine whether you should mark your territory-
    final Tile toMark = findTileToMark() ;
    if (toMark != null) {
      final Action marking = new Action(
        this, toMark,
        this, "actionMarkTerritory",
        Action.STAND, "Marking Territory"
      ) ;
      marking.setPriority(Action.CASUAL) ;
      choice.add(marking) ;
    }
  }
  
  
  private Fauna findCompetition() {
    final Batch <Fauna> tried = new Batch <Fauna> () ;
    for (Element e : AI.seen()) if (e instanceof Micovore) {
      if (e == this) continue ;
      final Micovore m = (Micovore) e ;
      tried.add(m) ;
    }
    return (Fauna) Rand.pickFrom(tried) ;
  }
  
  
  private Tile findTileToMark() {
    final Venue lair = AI.home() ;
    if (lair == null) return null ;
    float angle = Rand.num() * (float) Math.PI * 2 ;
    final Vec3D p = lair.position(null) ;
    final int range = species.forageRange() / 2 ;
    
    final Tile tried = world.tileAt(
      p.x + (float) (Math.cos(angle) * range),
      p.x + (float) (Math.sin(angle) * range)
    ) ;
    if (tried == null) return null ;
    final Tile free = Spacing.nearestOpenTile(tried, tried) ;
    if (free == null) return null ;
    
    final PresenceMap markMap = world.presences.mapFor(SpiceMidden.class) ;
    final SpiceMidden near = (SpiceMidden) markMap.pickNearest(free, range) ;
    final float dist = near == null ? 10 : Spacing.distance(near, free) ;
    if (dist < 5) return null ;
    
    return free ;
  }
  
  
  public boolean actionMarkTerritory(Micovore actor, Tile toMark) {
    if (toMark.owner() != null || toMark.blocked()) return false ;
    final SpiceMidden midden = new SpiceMidden() ;
    midden.enterWorldAt(toMark.x, toMark.y, world) ;
    return true ;
  }
}









/**  Behaviour implementation-
  */
/*
protected float rateMigratePoint(Tile point) {
  final float sampleRange = Lair.PEER_SAMPLE_RANGE ;
  float rating = super.rateMigratePoint(point) ;
  final Batch <Fauna> nearPrey = specimens(
    point, sampleRange, null, Species.Type.BROWSER, 3
  ) ;
  if (nearPrey.size() == 0) return rating ;
  float avgDistance = 0 ;
  for (Fauna f : nearPrey) avgDistance += Spacing.distance(point, f) ;
  avgDistance /= nearPrey.size() * sampleRange ;
  return rating * (2 - avgDistance) ;
}
//*/



