/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.graphics.widgets.* ;
import src.graphics.cutout.* ;
import src.graphics.sfx.* ;
import src.user.* ;
import src.util.* ;




public abstract class Venue extends Fixture implements
  Schedule.Updates, Boardable, Installation,
  Inventory.Owner, ActorAI.Employment,
  Selectable
{
  
  
  /**  Field definitions, constants, constructors, and save/load methods.
    */
  final public static int
    ENTRANCE_NONE  = -1,
    ENTRANCE_NORTH =  0,
    ENTRANCE_EAST  =  1,
    ENTRANCE_SOUTH =  2,
    ENTRANCE_WEST  =  3,
    NUM_SIDES      =  4 ;
  final public static int
    SHIFTS_ALWAYS      = 0,
    SHIFTS_BY_HOURS    = 1,   //different 8-hour periods off.
    SHIFTS_BY_DAY      = 2,   //every second or third day off.
    SHIFTS_BY_CALENDAR = 3 ;  //weekends and holidays off.  NOT DONE YET
  
  
  BuildingSprite buildSprite ;
  Healthbar healthbar ;
  final public TalkFX chat = new TalkFX() ;
  
  int entranceFace ;
  Tile entrance ;
  
  Base base ;
  List <Mobile> inside = new List <Mobile> () ;
  
  final public VenuePersonnel personnel = new VenuePersonnel(this) ;
  final public VenueStocks    stocks    = new VenueStocks(this)    ;
  final public VenueStructure structure = new VenueStructure(this) ;
  
  
  
  public Venue(int size, int high, int entranceFace, Base base) {
    super(size, high) ;
    this.base = base ;
    this.entranceFace = entranceFace ;
  }
  
  
  public Venue(Session s) throws Exception {
    super(s) ;
    buildSprite = (BuildingSprite) sprite() ;
    
    entranceFace = s.loadInt() ;
    entrance = (Tile) s.loadTarget() ;
    base = (Base) s.loadObject() ;
    s.loadObjects(inside) ;
    
    personnel.loadState(s) ;
    stocks.loadState(s) ;
    structure.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    s.saveInt(entranceFace) ;
    s.saveTarget(entrance) ;
    s.saveObject(base) ;
    s.saveObjects(inside) ;
    
    personnel.saveState(s) ;
    stocks.saveState(s) ;
    structure.saveState(s) ;
  }
  
  
  public Inventory inventory() { return stocks ; }
  public float priceFor(Service service) { return stocks.priceFor(service) ; }
  protected Index <Upgrade> allUpgrades() { return null ; }

  public int owningType() { return VENUE_OWNS ; }
  public Base base() { return base ; }
  
  protected BuildingSprite buildSprite() { return buildSprite ; }
  
  
  
  /**  Installation and positioning-
    */
  public boolean canPlace() {
    if (origin() == null) return false ;
    final World world = origin().world ;
    //
    //  Make sure we don't displace any more important object, or occupy their
    //  entrances.  In addition, the entrance must be clear.
    if (mainEntrance() == null) return false ;
    for (Tile t : world.tilesIn(area(), false)) {
      if (t == null || t.owningType() >= owningType()) return false ;
      if (Spacing.isEntrance(t)) return false ;
    }
    /*
    for (Tile n : Spacing.perimeter(area(), world)) {
      if (n == null) return false ;
    }
    //*/
    //
    //  And make sure we don't create isolated areas of unreachable tiles-
    if (! Spacing.perimeterFits(this)) return false ;
    if (mainEntrance().owningType() >= owningType()) return false ;
    return true ;
  }
  
  
  public void setPosition(float x, float y, World world) {
    super.setPosition(x, y, world) ;
    final Tile o = origin() ;
    final int off[] = Spacing.entranceCoords(size, size, entranceFace) ;
    entrance = world.tileAt(o.x + off[0], o.y + off[1]) ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    world.presences.togglePresence(this, true , services()) ;
    ///if (base != null) updatePaving(true) ;
    world.schedule.scheduleForUpdates(this) ;
    personnel.onCommission() ;
  }
  
  
  public void exitWorld() {
    personnel.onDecommission() ;
    world.presences.togglePresence(this, false, services()) ;
    if (base != null) updatePaving(false) ;
    world.schedule.unschedule(this) ;
    super.exitWorld() ;
  }
  
  
  public void setAsEstablished(boolean isDone) {
    super.setAsEstablished(isDone) ;
    ///if (isDone) structure.setState(VenueStructure.STATE_INTACT, 1.0f) ;
  }
  
  
  public void onCompletion() {
    world.ephemera.addGhost(this, size, buildSprite.scaffolding(), 2.0f) ;
    setAsEstablished(false) ;
    //personnel.onCompletion() ;
  }
  
  
  public void onDecommission() {
    world.ephemera.addGhost(this, size, buildSprite.baseSprite(), 2.0f) ;
    setAsEstablished(false) ;
    //personnel.onDecommission() ;
  }
  
  
  public void setAsDestroyed() {
    final World world = this.world ;
    final Box2D area = this.area() ;
    super.setAsDestroyed() ;
    Wreckage.reduceToSlag(area, world) ;
  }
  
  
  
  /**  Implementing the Boardable interface-
    */
  public void setInside(Mobile m, boolean is) {
    if (is) {
      inside.include(m) ;
    }
    else {
      inside.remove(m) ;
    }
  }
  
  
  public List <Mobile> inside() {
    return inside ;
  }
  
  
  public Tile mainEntrance() {
    return entrance ;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    final Tile o = origin() ;
    put.set(o.x - 0.5f, o.y - 0.5f, size, size) ;
    return put ;
  }
  
  
  public Boardable[] canBoard(Boardable batch[]) {
    final int minSize = 1 + inside.size() ;
    if (batch == null || batch.length < minSize) {
      batch = new Boardable[minSize] ;
    }
    else for (int i = batch.length ; i-- > 1 ;) batch[i] = null ;
    batch[0] = entrance ;
    int i = 1 ; for (Mobile m : inside) if (m instanceof Boardable) {
      batch[i++] = (Boardable) m ;
    }
    return batch ;
  }
  
  
  public boolean isEntrance(Boardable t) {
    return entrance == t ;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    return m.base() == base ;
  }
  
  
  
  /**  Updates and life cycle-
    */
  public float scheduledInterval() {
    return 1 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    if (! structure.intact()) {
      personnel.updatePersonnel(numUpdates) ;
      return ;
    }
    if (base != null && numUpdates % 10 == 0) updatePaving(true) ;
    stocks.updateStocks(numUpdates) ;
    personnel.updatePersonnel(numUpdates) ;
    structure.updateStructure(numUpdates) ;
  }
  
  
  protected void updatePaving(boolean inWorld) {
    if (inWorld) {
      base.paving.updateJunction(this, mainEntrance(), true) ;
      base.paving.updatePerimeter(this, true) ;
    }
    else {
      base.paving.updateJunction(this, mainEntrance(), false) ;
      base.paving.updatePerimeter(this, false) ;
    }
  }
  
  
  
  /**  Recruiting staff and assigning manufacturing tasks-
    */
  public void setWorker(Actor actor, boolean is) {
    personnel.setWorker(actor, is) ;
  }
  
  
  public int numOpenings(Background v) {
    return structure.upgradeBonus(v) - personnel.numPositions(v) ;
  }
  
  
  //  Careers, services, goods produced, and goods required.  Space has to be
  //  reserved for both item-orders and service-seating.  And what about shifts
  //  at work?
  protected abstract Background[] careers() ;
  public abstract Service[] services() ;
  
  
  public boolean isManned() {
    for (Actor a : personnel.workers) {
      if (a.health.conscious() && a.aboard() == this) return true ;
    }
    return false ;
  }
  
  
  public boolean privateProperty() {
    return false ;
  }
  
  
  
  /**  Installation interface-
    */
  public int buildCost() {
    return structure.buildCost() ;
  }
  
  
  public boolean pointsOkay(Tile from, Tile to) {
    //  You have to check for visibility too.  Have a Base argument?
    if (from == null) return false ;
    final Tile t = from ;
    setPosition(t.x, t.y, t.world) ;
    return canPlace() ;
  }
  
  
  public void doPlace(Tile from, Tile to) {
    if (sprite() != null) sprite().colour = null ;
    final Tile t = from ;
    setPosition(t.x, t.y, t.world) ;
    clearSurrounds() ;
    enterWorld() ;
    VenuePersonnel.fillVacancies(this) ;
    if (GameSettings.buildFree) {
      structure.setState(VenueStructure.STATE_INTACT , 1) ;
    }
    else {
      structure.setState(VenueStructure.STATE_INSTALL, 0) ;
    }
  }


  public void preview(
    boolean canPlace, Rendering rendering, Tile from, Tile to
  ) {
    if (from == null) return ;
    final Tile t = from ;
    final World world = t.world ;
    setPosition(t.x, t.y, t.world) ;
    final TerrainMesh overlay = world.terrain().createOverlay(
      world, surrounds(), false, Texture.WHITE_TEX
    ) ;
    overlay.colour = canPlace ? Colour.GREEN : Colour.RED ;
    rendering.addClient(overlay) ;
    
    if (sprite() == null) return ;
    this.position(sprite().position) ;
    sprite().colour = canPlace ? Colour.GREEN : Colour.RED ;
    rendering.addClient(sprite()) ;
  }
  
  
  
  /**  Interface methods-
    */
  public String toString() {
    return fullName() ;
  }
  
  
  
  public String[] infoCategories() {
    return new String[] { "STATUS", "STAFF", "UPGRADES" } ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    if (categoryID == 0) describeCondition(d, UI) ;
    if (categoryID == 1) describePersonnel(d, UI) ;
    if (categoryID == 2) describeUpgrades(d, UI) ;
  }
  
  
  private void describeCondition(Description d, HUD UI) {
    
    d.append("Condition and Repair:") ;
    d.append("\n  Integrity: ") ;
    d.append(structure.repair()+" / "+structure.maxIntegrity()) ;
    //  If there's an upgrade in progress, list it here.
    final String CUD = structure.currentUpgradeDesc() ;
    if (CUD != null) d.append("\n  "+CUD) ;
    d.append("\n  Materials Needed: "+"None") ;
    d.append("\n  Untaxed Credits: "+(int) stocks.credits()) ;
    
    d.append("\n\nStocks and Orders:") ;
    boolean empty = true ;
    for (Service type : BuildConstants.ALL_ITEM_TYPES) {
      if (describeStocks(type, d)) empty = false ;
    }
    for (Manufacture m : stocks.specialOrders()) {
      d.append("\n  ") ; m.describeBehaviour(d) ; empty = false ;
    }
    if (empty) d.append("\n  No stocks or orders.") ;
  }
  
  
  protected boolean describeStocks(Service type, Description d) {
    final int needed ;
    if (this instanceof Service.Trade) {
      final Service.Trade trade = (Service.Trade) this ;
      needed = (int) Math.ceil(Math.max(
        trade.exportDemand(type),
        trade.importDemand(type)
      )) ;
    }
    else needed = (int) Math.ceil(stocks.demandFor(type)) ;
    final int amount = (int) Math.ceil(stocks.amountOf(type) ) ;
    if (needed == 0 && amount == 0) return false ;
    
    if (Visit.arrayIncludes(services(), type)) {
      final int price  = (int) Math.ceil(priceFor(type)) ;
      d.append("\n  "+type.name+" "+amount+"/"+needed+" (Price "+price+")") ;
    }
    else d.append("\n  "+type.name+" "+amount+"/"+needed) ;
    return true ;
  }
  
  
  
  private void describePersonnel(Description d, HUD UI) {
    d.append("Personnel and Visitors:") ;
    final Batch <Mobile> considered = new Batch <Mobile> () ;
    for (Actor m : personnel.residents()) considered.include(m) ;
    for (Actor m : personnel.workers()) considered.include(m) ;
    for (Mobile m : inside) considered.include(m) ;
    
    for (Mobile m : considered) {
      d.append("\n  ") ; d.append(m) ;
      d.append("\n  ") ; d.append(dutyDesc(m)) ;
      d.append("\n  ") ; m.describeStatus(d) ;
    }
    
    d.append("\n\nVacancies and Applications:") ;
    boolean none = true ;
    if (careers() != null) for (Background v : careers()) {
      final int numOpen = numOpenings(v) ;
      if (numOpen > 0) none = false ;
      if (numOpen > 0) d.append("\n  "+numOpen+" "+v.name+" vacancies") ;
    }
    for (final VenuePersonnel.Position p : personnel.positions) {
      if (p.wages != -1) continue ;
      none = false ;
      d.append("\n  ") ;
      d.append(p.works) ;
      d.append("\n  ("+p.salary+" credits) ") ;
      d.append(new Description.Link("HIRE") {
        public void whenClicked() { personnel.confirmApplication(p) ; }
      }) ;
    }
    if (none) d.append("\n  No vacancies or applications.") ;
  }
  
  
  private String dutyDesc(Mobile m) {
    if (! (m instanceof Actor)) return "" ;
    final Actor a = (Actor) m ;
    if (a.AI.home() == this) return "(Resident)" ;
    if (a.AI.work() != this) return "(Visitor)" ;
    final String duty = personnel.onShift(a) ? "On-Duty" : "Off-Duty" ;
    return "("+duty+" "+a.vocation()+")" ;
  }
  
  
  private static Upgrade lastCU ;  //last clicked...
  
  private void describeUpgrades(Description d, HUD UI) {
    
    final Batch <String> DU = structure.descUpgrades() ;
    final int numU = structure.numUpgrades(), maxU = structure.maxUpgrades() ;
    d.append("Upgrade slots ("+numU+"/"+maxU+")") ;
    for (String s : DU) d.append("\n  "+s) ;
    
    d.append("\n\nUpgrades available: ") ;
    final Index <Upgrade> upgrades = allUpgrades() ;
    if (upgrades != null && upgrades.members().length > 0) {
      for (final Upgrade upgrade : upgrades) {
        d.append("\n  ") ;
        d.append(new Description.Link(upgrade.name) {
          public void whenClicked() { lastCU = upgrade ; }
        }) ;
        d.append(" (x"+structure.upgradeLevel(upgrade)+")") ;
      }
      if (lastCU != null) {
        d.append("\n\n") ;
        d.append(lastCU.description) ;
        d.append("\n  Cost: "+lastCU.buildCost+"   ") ;
        if (lastCU.required != null) {
          d.append("\n  Requires: "+lastCU.required.name) ;
        }
        if (structure.upgradePossible(lastCU)) {
          d.append(new Description.Link("\n  BEGIN UPGRADE") {
            public void whenClicked() {
              structure.beginUpgrade(lastCU, false) ;
            }
          }) ;
        }
      }
    }
    else d.append("\n  No upgrades.") ;
  }

  
  public void whenClicked() {
    lastCU = null ;
    ((BaseUI) PlayLoop.currentUI()).selection.pushSelection(this, false) ;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new InfoPanel(UI, this, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }
  
  
  public Target subject() { return this ; }
  
  
  
  /**  Rendering methods-
    */
  protected void attachSprite(Sprite sprite) {
    if (! (sprite instanceof ImageSprite)) {
      I.complain("VENUES MUST HAVE IMAGE-BASED SPRITES!") ;
    }
    buildSprite = new BuildingSprite(sprite, size, high) ;
    super.attachSprite(buildSprite) ;
  }
  
  
  protected float fogFor(Base base) {
    if (base == this.base) return 1 ;
    return super.fogFor(base) ;
  }
  
  
  protected void renderHealthbars(Rendering rendering, Base base) {
    if (healthbar == null) healthbar = new Healthbar() ;
    healthbar.level = structure.repairLevel() ;
    final int NU = structure.numUpgrades() ;
    healthbar.size = (radius() * 50) ;
    healthbar.size *= 1 + VenueStructure.UPGRADE_HP_BONUSES[NU] ;
    healthbar.matchTo(buildSprite) ;
    healthbar.position.z += height() ;
    rendering.addClient(healthbar) ;
    
    if (base() == null) healthbar.full = Colour.LIGHT_GREY ;
    else healthbar.full = base().colour ;
    
    if (structure.needsUpgrade()) {
      Healthbar progBar = new Healthbar() ;
      progBar.level = structure.upgradeProgress() ;
      progBar.size = healthbar.size ;
      progBar.position.setTo(healthbar.position) ;
      progBar.yoff = Healthbar.BAR_HEIGHT ;
      progBar.full = Colour.GREY ;
      progBar.empty = Colour.GREY ;
      progBar.colour = Colour.WHITE ; //paler version of main bar colour?
      rendering.addClient(progBar) ;
    }
  }
  
  
  protected void toggleStatusDisplay() {
    final boolean burns = structure.burning() ;
    buildSprite.toggleFX(BuildingSprite.BLAST_MODEL, burns) ;
    
    final float t = 0.5f ;
    final boolean noLS = stocks.shortageOf(BuildConstants.LIFE_SUPPORT) >= t ;
    buildSprite.toggleFX(BuildConstants.LIFE_SUPPORT.model, noLS) ;
    final boolean noPower = stocks.shortageOf(BuildConstants.POWER) >= t ;
    buildSprite.toggleFX(BuildConstants.POWER.model, noPower) ;
    final boolean noWater = stocks.shortageOf(BuildConstants.WATER) >= t ;
    buildSprite.toggleFX(BuildConstants.WATER.model, noWater) ;
    final boolean noDL = stocks.shortageOf(BuildConstants.DATALINKS) >= t ;
    buildSprite.toggleFX(BuildConstants.DATALINKS.model, noDL) ;
  }
  
  
  protected void renderChat(Rendering rendering, Base base) {
    if (chat.numPhrases() > 0) {
      chat.position.setTo(sprite().position) ;
      chat.position.z += height() ;
      chat.update() ;
      rendering.addClient(chat) ;
    }
  }
  
  
  //
  //  TODO:  Allow this to be customised by subclasses.
  final private static float ITEM_S_OFF[] = {
     0, 0,
    -1, 0,
     0, 1,
    -2, 0,
    -3, 0,
     0, 2,
     0, 3,
  } ;
  
  
  private boolean canShow(Service type) {
    if (type.form != BuildConstants.FORM_COMMODITY) return false ;
    if (type.pic == Service.DEFAULT_PIC) return false ;
    return true ;
  }
  
  
  protected void updateItemSprites() {
    final Service services[] = services() ;
    final float
      initX = (size / 2f) - 0.5f,
      initY = 0.5f - (size / 2f) ;
    int index = -1 ;
    for (Service s : services) if (canShow(s)) index += 2 ;
    if (index < 0) return ;
    index = Visit.clamp(index, ITEM_S_OFF.length) ;
    
    for (Service s : services) if (canShow(s)) {
      if (index < 0) break ;
      final float y = ITEM_S_OFF[index--], x = ITEM_S_OFF[index--] ;
      if (y >= size || size <= -x) continue ;
      
      buildSprite.updateItemDisplay(
        s.model, stocks.amountOf(s),
        x + initX, y + initY
      ) ;
    }
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    
    position(buildSprite.position) ;
    buildSprite.updateCondition(
      structure.repairLevel(),
      structure.intact(),
      structure.burning()
    ) ;
    toggleStatusDisplay() ;
    updateItemSprites() ;
    renderHealthbars(rendering, base) ;
    renderChat(rendering, base) ;
    
    super.renderFor(rendering, base) ;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (destroyed() || ! inWorld()) return ;
    Selection.renderPlane(
      rendering, position(null), (xdim() / 2f) + 1,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_SQUARE
    ) ;
  }
  
}







