/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class Foundry extends Venue implements BuildConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    Foundry.class, "media/Buildings/artificer/artificer.png", 4, 2
  ) ;
  
  
  public Foundry(Base base) {
    super(4, 2, ENTRANCE_WEST, base) ;
    structure.setupStats(
      200, 5, 350, VenueStructure.NORMAL_MAX_UPGRADES,
      false
    ) ;
    this.attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Foundry(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Economic functions, upgrades and employee behaviour-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    Foundry.class, "foundry_upgrades"
  ) ;
  protected Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    ASSEMBLY_LINE = new Upgrade(
      "Assembly Line",
      "An assembly line allows standardised parts to manufactured quickly, "+
      "cheaply and in greater abundance.",
      200,
      PARTS, 2, null, ALL_UPGRADES
    ),
    MOLDING_PRESS = new Upgrade(
      "Molding Press",
      "The molding press allows materials to be recycled and sculpted to fit "+
      "new purposes, reducing waste and pollution, and speeding production "+
      "of custom-made parts.",
      150,
      PLASTICS, 1, null, ALL_UPGRADES
    ),
    TECHNICIAN_QUARTERS = new Upgrade(
      "Technician Quarters",
      "Technicians are trained to operate and perform routine maintenance on "+
      "common machinery, but lack the theoretical grounding needed for "+
      "fundamental design or customisation.",
      50,
      Vocation.TECHNICIAN, 2, null, ALL_UPGRADES
    ),
    COMPOSITE_MATERIALS = new Upgrade(
      "Composite Materials",
      "Composite materials enhance the production of lightweight and "+
      "flexible armours, as well as close-range melee weaponry.",
      200,
      null, 2, MOLDING_PRESS, ALL_UPGRADES
    ),
    FLUX_CONTAINMENT = new Upgrade(
      "Flux Containment",
      "Flux containment allows high-energy plasmas to be generated and "+
      "controlled, permitting refinements to shield technology and ranged "+
      "energy weapons.",
      250,
      null, 2, TECHNICIAN_QUARTERS, ALL_UPGRADES
    ),
    ARTIFICER_QUARTERS = new Upgrade(
      "Artificer Quarters",
      "Artificers are highly-skilled as physicists and engineers, and can "+
      "tackle the most taxing commissions reliant on dangerous or arcane "+
      "technologies.",
      150,
      Vocation.ARTIFICER, 1, TECHNICIAN_QUARTERS, ALL_UPGRADES
    ) ;
  
  
  protected Service[] services() {
    return new Service[] {
      PARTS, SHOCK_STAFF, PHASE_PISTOL,
      SHIELD_BELT, BODY_ARMOUR, GOLEM_ARMOUR
    } ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.TECHNICIAN, Vocation.ARTIFICER } ;
  }
  
  
  public int numOpenings(Vocation v) {
    int num = super.numOpenings(v) ;
    if (v == Vocation.TECHNICIAN) return num + 2 ;
    if (v == Vocation.ARTIFICER ) return num + 0 ;
    return 0 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    
    /*
    if (stocks.amountOf(METALS) < 10) {
      stocks.addItem(Item.withAmount(METALS, 10)) ;
    }
    //*/
    if (stocks.receivedShortage(PARTS) < 5) stocks.setRequired(PARTS, 5) ;
    stocks.translateDemands(METALS_TO_PARTS) ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if (! structure.intact()) return null ;
    
    final Choice choice = new Choice(actor) ;
    
    final Building b = Building.getNextRepairFor(actor) ;
    if (b != null) {
      b.priorityMod = Behaviour.CASUAL ;
      choice.add(b) ;
    }
    
    final Manufacture o = stocks.nextSpecialOrder(actor) ;
    if (o != null) {
      o.checkBonus = structure.upgradeLevel(MOLDING_PRESS) + 2 ;
      final int CMB = structure.upgradeLevel(COMPOSITE_MATERIALS) + 2 ;
      final int FCB = structure.upgradeBonus(FLUX_CONTAINMENT) + 2 ;
      
      if (o.made.type instanceof DeviceType) {
        final DeviceType DT = (DeviceType) o.made.type ;
        if (DT.hasProperty(PHYSICAL)) o.checkBonus += CMB ;
        if (DT.hasProperty(ENERGY)) o.checkBonus += FCB ;
      }
      if (o.made.type instanceof OutfitType) {
        //
        //  TODO:  Have separate ratings for shield and armour values
        //  associated with armour, and scale appropriately.
        final OutfitType OT = (OutfitType) o.made.type ;
        if (OT.defence <= 10) o.checkBonus += FCB ;
        else o.checkBonus += CMB ;
      }
      o.timeMult = 4 ;
      choice.add(o) ;
    }
    
    final Manufacture m = stocks.nextManufacture(actor, METALS_TO_PARTS) ;
    if (m != null) {
      I.say("Next manufacture: "+m) ;
      m.checkBonus = structure.upgradeBonus(PARTS) ;
      choice.add(m) ;
    }
    
    return choice.weightedPick(actor.AI.whimsy()) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/artificer_button.gif") ;
  }
  
  
  public String fullName() {
    return "The Foundry" ;
  }
  
  
  public String helpInfo() {
    return
      "The Foundry manufactures parts, inscriptions, devices and armour "+
      "for your citizens." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ARTIFICER ;
  }
}







