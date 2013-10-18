


package src.game.base ;
import src.game.building.* ;
import src.game.actors.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class Archives extends Venue implements Economy {
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asSolidModel(
    Archives.class, "media/Buildings/physician/archives.png", 4, 3
  ) ;
  

  public Archives(Base base) {
    super(4, 3, Venue.ENTRANCE_SOUTH, base) ;
    structure.setupStats(
      250, 4, 500,
      Structure.NO_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public Archives(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  

  /**  Upgrades, economic functions and behaviour implementations-
    */
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    //
    //  See if any new datalinks need to be installed or manufactured-
    final Manufacture m = stocks.nextManufacture(
      actor, CIRCUITRY_TO_DATALINKS
    ) ;
    if (m != null) {
      choice.add(m) ;
    }
    final Manufacture o = stocks.nextSpecialOrder(actor) ;
    if (o != null) {
      choice.add(o) ;
    }
    //
    //  Check to see if any datalinks require delivery- if so, key them to the
    //  client first.
    final Delivery baseD = Deliveries.nextDeliveryFor(
      actor, this, services(), 1, world
    ) ;
    if (baseD != null) {
      final Item custom = Item.withReference(DATALINKS, baseD.destination) ;
      if (! stocks.hasItem(custom)) {
        final Action personalise = new Action(
          actor, baseD.destination,
          this, "actionKeyDatalink",
          Action.BUILD, "Personalising Datalinks"
        ) ;
        personalise.setMoveTarget(this) ;
        personalise.setPriority(Action.ROUTINE) ;
        choice.add(personalise) ;
      }
      else choice.add(new Delivery(custom, this, baseD.destination)) ;
    }
    if (choice.size() > 0) return choice.weightedPick(actor.mind.whimsy()) ;
    //
    //  Otherwise, just hang around and help folks with their inquiries-
    return new Supervision(actor, this) ;
  }
  
  
  public boolean actionKeyDatalink(Actor actor, Venue client) {
    if (stocks.amountOf(DATALINKS) < 1) return false ;
    if (! actor.traits.test(INSCRIPTION, SIMPLE_DC, 1)) return false ;
    final Item custom = Item.withReference(DATALINKS, client) ;
    stocks.bumpItem(DATALINKS, -1) ;
    stocks.addItem(custom) ;
    return true ;
  }
  
  
  public boolean hasDataFor(Skill skill) {
    final Item match = Item.withReference(DATALINKS, skill) ;
    return stocks.hasItem(match) ;
  }
  
  
  public Background[] careers() {
    return new Background[] { Background.ARCHIVIST } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.ARCHIVIST) {
      return nO + 1 + (int) (structure.numUpgrades() / 3) ;
    }
    return 0 ;
  }
  
  
  public Service[] services() {
    return new Service[] { DATALINKS } ;
  }
  
  
  
  /**  Rendering and interface-
    */
  protected Service[] goodsToShow() {
    return new Service[] { CIRCUITRY, DATALINKS } ;
  }
  
  
  final static Skill[][] SKILL_CATS = {
    ARTIFICER_SKILLS, ECOLOGIST_SKILLS, PHYSICIAN_SKILLS, HISTORY_SKILLS
  } ;
  final static String CAT_NAMES[] = {
    "Artificer", "Ecologist", "Physician", "History"
  } ;
  final static int CAT_IDS[] = { 0, 1, 2, 3 } ;
  
  private static int lastSkillCat = 0 ;
  //private static Skill lastSkillPick = null ;
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    if (categoryID != 3 || ! structure.intact()) return ;
    final Venue archives = this ;
    //
    //  List the various research categories.
    d.append("Information Categories:\n  ") ;
    for (final int n : CAT_IDS) {
      d.append(new Description.Link(CAT_NAMES[n]) {
        public void whenClicked() { lastSkillCat = n ; }
      }) ;
      d.append(" ") ;
    }
    d.append("\n") ;
    
    for (final Skill skill : SKILL_CATS[lastSkillCat]) {
      final Item match = Item.withReference(DATALINKS, skill) ;
      
      d.append("\n  "+skill.name) ;
      if (stocks.hasItem(match)) {
        d.append(" Installed") ;
      }
      else if (stocks.hasOrderFor(match)) {
        d.append(" Ordered") ;
      }
      else d.append(new Description.Link(" Install") {
        public void whenClicked() {
          stocks.addSpecialOrder(new Manufacture(
            null, archives, CIRCUITRY_TO_DATALINKS, match
          )) ;
        }
      }) ;
    }
    
  }
  
  
  public String fullName() {
    return "Archives" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/archives_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "The Archives facilitates research, administration and self-education "+
      "programs." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_PHYSICIAN ;
  }
}



/*
final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  Archives.class, "archives_upgrades"
) ;
//public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
//
//  TODO:  You might not want to implement these as upgrades?  They shouldn't
//  really be contributing toward hit-points, for example.  Might be better
//  to model them as imported items, or some kind of custom object.
/*
final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
  Archives.class, "archives_upgrades"
) ;
public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
static {
  for (Skill skill : COGNITIVE_SKILLS) {
    if (skill.parent != INTELLECT) continue ;
    final Upgrade dataLink = new Upgrade(
      skill.name+" datalinks", "Allows research in "+skill.name,
      250, skill, 5, null, ALL_UPGRADES
    ) ;
  }
}
//*/




