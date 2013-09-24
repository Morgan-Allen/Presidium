/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.base ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;



//
//  TODO:  Return forestry plans as well...



public class BotanicalStation extends Venue implements BuildConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist/" ;
  final static Model
    STATION_MODEL = ImageModel.asIsometricModel(
      BotanicalStation.class, IMG_DIR+"botanical_station.png", 4, 3
    ) ;
  
  
  
  final static int MAX_PLANT_RANGE = 16 ;
  
  final List <Plantation> allotments = new List <Plantation> () ;
  
  
  
  public BotanicalStation(Base belongs) {
    super(4, 3, Venue.ENTRANCE_SOUTH, belongs) ;
    structure.setupStats(
      150, 3, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(STATION_MODEL.makeSprite()) ;
  }
  
  
  public BotanicalStation(Session s) throws Exception {
    super(s) ;
    s.loadObjects(allotments) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(allotments) ;
  }
  
  
  
  /**  Handling upgrades and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    BotanicalStation.class, "botanical_upgrades"
  ) ;
  protected Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    CEREAL_LAB = new Upgrade(
      "Cereal Lab",
      "Improves cereal yields.  Cereals yield more calories than other crop "+
      "species, but lack the full range of nutrients required in a healthy "+
      "diet.",
      100,
      CARBS, 1,
      null, ALL_UPGRADES
    ),
    BROADLEAF_LAB = new Upgrade(
      "Broadleaf Lab",
      "Improves broadleaf yields.  Broadleaves provide a wider range of "+
      "nutrients, and are valued as luxury exports, but their yield is small.",
      150,
      GREENS, 1,
      null, ALL_UPGRADES
    ),
    FIELD_HAND_STATION = new Upgrade(
      "Field Hand Station",
      "Hire additional field hands to plant and reap the harvest more "+
      "quickly, maintain equipment, and bring land under cultivation.",
      50,
      Background.FIELD_HAND, 1,
      null, ALL_UPGRADES
    ),
    TREE_FARMING = new Upgrade(
      "Tree Farming",
      "Forestry programs assist in terraforming efforts and climate "+
      "moderation, as well as providing carbons for plastic production.",
      100,
      PETROCARBS, 1,
      BROADLEAF_LAB, ALL_UPGRADES
    ),
    INSECTRY_LAB = new Upgrade(
      "Insectry Lab",
      "Many plantations cultivate colonies of social insects or other "+
      "invertebrates, both as a source of protein and pollination, pest "+
      "control, or recycling services.",
      150,
      PROTEIN, 1,
      BROADLEAF_LAB, ALL_UPGRADES
    ),
    ECOLOGIST_STATION = new Upgrade(
      "Ecologist Station",
      "Ecologists are highly-skilled students of plants, animals and gene "+
      "modification, capable of adapting species to local climate conditions.",
      150,
      Background.ECOLOGIST, 1,
      TREE_FARMING, ALL_UPGRADES
    ) ;
  

  public Behaviour jobFor(Actor actor) {
    if (! structure.intact()) return null ;
    if (Planet.isNight(world)) return null ;
    //
    //  If the harvest is really coming in, pitch in regardless-
    final Choice choice = new Choice(actor) ;
    for (Plantation p : allotments) {
      if (p.needForFarming() > 0.5f) choice.add(new Farming(actor, p)) ;
    }
    if (choice.size() > 0) return choice.weightedPick(0) ;
    //
    //  Otherwise, perform deliveries and more casual work-
    if (! personnel.onShift(actor)) return null ;
    final Delivery d = Deliveries.nextDeliveryFor(
      actor, this, services(), 10, world
    ) ;
    choice.add(d) ;
    
    final Forestry f = new Forestry(actor, this) ;
    choice.add(f) ;
    
    for (Plantation p : allotments) choice.add(new Farming(actor, p)) ;
    return choice.weightedPick(0) ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    if (numUpdates % 50 == 0) {
      final int STRIP_SIZE = 4 ;
      int numCovered = 0 ;
      //
      //  First of all, remove any missing allotments (and their siblings in
      //  the same strip.)
      for (Plantation p : allotments) {
        if (p.destroyed()) {
          allotments.remove(p) ;
          for (Plantation s : p.strip) if (s != p) {
            s.structure.setState(Structure.STATE_SALVAGE, -1) ;
          }
        }
        else if (p.type == Plantation.TYPE_COVERED) numCovered++ ;
      }
      //
      //  Then, calculate how many allotments one should have.
      int maxAllots = 3 + (structure.upgradeBonus(Background.FIELD_HAND) * 2) ;
      maxAllots *= STRIP_SIZE ;
      if (maxAllots > allotments.size()) {
        //
        //  If you have too few, try to find a place for more-
        final boolean covered = numCovered <= allotments.size() / 3 ;
        Plantation allots[] = Plantation.placeAllotment(
          this, covered ? STRIP_SIZE : STRIP_SIZE, covered
        ) ;
        if (allots != null) for (Plantation p : allots) {
          allotments.add(p) ;
        }
      }
      if (maxAllots + STRIP_SIZE < allotments.size()) {
        //
        //  And if you have too many, flag them for salvage.
        for (int n = STRIP_SIZE ; n-- > 0 ;) {
          final Plantation p = allotments.removeLast() ;
          p.structure.setState(Structure.STATE_SALVAGE, -1) ;
        }
      }
    }
  }
  

  public int numOpenings(Background v) {
    int num = super.numOpenings(v) ;
    if (v == Background.FIELD_HAND) return num + 1 ;
    if (v == Background.ECOLOGIST ) return num + 1 ;
    return 0 ;
  }
  
  
  protected List <Plantation> allotments() {
    return allotments ;
  }
  
  
  protected float growBonus(Tile t, int varID, boolean natural) {
    final float pollution = t.world.ecology().squalorAt(t) / 10f ;
    final float hB = 1 - pollution ;
    if (hB <= 0) return 0 ;
    float bonus = 1 ;
    if (varID == 4) {
      if (natural) return hB ;
      return (structure.upgradeBonus(PROTEIN) + 2) * 0.1f * bonus * hB ;
    }
    //
    //  Crops are, in sequence, rice, wheat, lily-things, tubers/veg and grubs.
    bonus = Math.max(0, (t.habitat().moisture() - 5) / 5f) ;
    if (varID == 1 || varID == 3) bonus = (1 - bonus) / 2f ;  //Dryland crops.
    if (varID < 2) {
      if (natural) return 1.0f * bonus * hB ;
      return (structure.upgradeBonus(CARBS) + 2) * 1.0f * bonus * hB ;
    }
    else {
      if (natural) return 0.5f * bonus * hB ;
      return (structure.upgradeBonus(GREENS) + 2) * 0.5f * bonus * hB ;
    }
  }
  
  
  public Service[] services() {
    return new Service[] { PETROCARBS, GREENS, PROTEIN, CARBS } ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.ECOLOGIST, Background.FIELD_HAND } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/nursery_button.gif") ;
  }
  
  
  public String fullName() { return "Botanical Station" ; }
  
  
  public String helpInfo() {
    return
      "Botanical Stations are responsible for agriculture and forestry, "+
      "helping to secure food supplies and advance terraforming efforts." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST ;
  }
}









/**  Grabbing areas suitable for plantation-
  */

//final List <Tile> toPlant = new List <Tile> () ;
//final List <Crop> planted = new List <Crop> () ;
//private int onceGrabbed = 0 ;
/*
protected float ratePlantArea() {
  if (inWorld()) I.complain("Only intended for rating potential sites!") ;
  if (origin() == null) I.complain("Must set position first!") ;
  grabPlantArea() ;
  
  float rating = 0 ;
  for (Tile t : toPlant) {
    rating += t.habitat().moisture() ;
  }
  final int dim = 2 + (MAX_PLANT_RANGE * 2) ;
  return rating / (dim * dim) ;
}


public void enterWorldAt(int x, int y, World world) {
  super.enterWorldAt(x, y, world) ;
  toPlant.clear() ;
  onceGrabbed = 0 ;
}


protected void grabPlantArea() {
  final Tile o = origin() ;
  final int r = MAX_PLANT_RANGE, span = r + size + r ;
  final Box2D area = new Box2D().set(
    (o.x - r) - 0.5f, (o.y - r) - 0.5f,
    span, span
  ) ;
  toPlant.clear() ;
  
  //
  //  TODO:  You also want to put a Nursery someplace nearby.  But this will
  //  do for now.
  
  final BotanicalStation nursery = this ;
  final TileSpread spread = new TileSpread(o) {
    protected boolean canAccess(Tile t) {
      return nursery.canAccess(t, area) ;
    }
    protected boolean canPlaceAt(Tile t) {
      if ((t.y - o.y) % 3 == 0) return false ;
      if (nursery.canPlant(t)) toPlant.add(t) ;
      return false ;
    }
  } ;
  spread.doSearch() ;
  onceGrabbed = toPlant.size() + planted.size() ;
}


protected boolean canAccess(Tile t, Box2D area) {
  if (! t.habitat().pathClear) return false ;
  if (! area.contains(t.x, t.y)) return false ;
  if (t.owner() == this) return true ;
  if (t.owningType() >= Element.FIXTURE_OWNS) return false ;
  return true ;
}


protected boolean canPlant(Tile t) {
  if (t.pathType() == Tile.PATH_ROAD) return false ;
  if (t.owner() == this) return false ;
  if (t.owner() instanceof Crop) return false ;
  if (t.owningType() >= Element.FIXTURE_OWNS) return false ;
  return true ;
}


protected int onceGrabbed() {
  return onceGrabbed ;
}
//*/




