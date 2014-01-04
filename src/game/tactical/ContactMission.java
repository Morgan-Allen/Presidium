


package src.game.tactical ;
import src.game.civic.*;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



//
//  TODO:  Try and convince a particular subject to join your settlement, not
//  be hostile, or simply accept a gift.  Applies to all actors encountered
//  that belong to the same base, but with particular emphasis on the primary
//  subject.
//  (Use the Dialogue class.)


public class ContactMission extends Mission implements Economy {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static int
    SETTING_TRIBUTE  = 0,
    SETTING_ALLIANCE = 1,
    SETTING_FEALTY   = 2 ;
  final static String SETTING_NAMES[] = {
    "Send Tribute",
    "Secure Peace",
    "Demand Fealty"
  } ;
  
  //  TODO:  Base gifts on the item's demanded at the subject's home or work
  //         venue, while defaulting to food, money, or bling.
  //  TODO:  Create a new plan for gift-giving.
  final Service GIFT_TYPES[] = {
    PROTEIN, GREENS, SPICE,
    PLASTICS, DECOR, MEDICINE
  } ;
  
  int contactType = 0 ;
  
  
  
  
  public ContactMission(Base base, Target subject) {
    super(
      base, subject,
      MissionsTab.CONTACT_MODEL.makeSprite(),
      "Making Contact with "+subject
    ) ;
  }
  
  
  public ContactMission(Session s) throws Exception {
    super(s) ;
    contactType = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(contactType) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    float reward = actor.mind.greedFor(rewardAmount(actor)) * ROUTINE ;
    float priority = 1 * reward ;
    
    return priority ;
  }
  
  
  public void beginMission() {
    super.beginMission() ;
  }
  
  
  public boolean finished() {
    return false ;
  }
  
  
  private float relationLevelNeeded() {
    if (contactType == SETTING_TRIBUTE ) return 0    ;
    if (contactType == SETTING_ALLIANCE) return 0.5f ;
    if (contactType == SETTING_FEALTY  ) return 1.0f ;
    return -1 ;
  }
  
  
  private Batch <Actor> talksTo() {
    final Batch <Actor> batch = new Batch <Actor> () ;
    if (subject instanceof Actor) {
      final Actor a = (Actor) subject ;
      batch.add(a) ;
    }
    else if (subject instanceof Venue) {
      final Venue v = (Venue) subject ;
      for (Actor a : v.personnel.residents()) batch.include(a) ;
      for (Actor a : v.personnel.workers()  ) batch.include(a) ;
    }
    return batch ;
  }


  public Behaviour nextStepFor(Actor actor) {
    final float minRelation = relationLevelNeeded() ;
    
    Dialogue picked = null ;
    float maxUrgency = Float.NEGATIVE_INFINITY ;
    for (Actor a : talksTo()) {
      final float relation = a.mind.relationValue(actor) ;
      if (relation > minRelation) continue ;
      
      final Dialogue d = new Dialogue(actor, a, null) ;
      float urgency = d.priorityFor(actor) ;
      urgency += a.mind.relationValue(actor) * ROUTINE ;
      if (urgency > maxUrgency) { maxUrgency = urgency ; picked = d ; }
    }
    
    if (picked != null) return picked ;
    
    //
    //  Otherwise, see if you can give them a gift, or find a suitable task
    //  from the subject's home or work venue.
    
    return null ;
  }
  
  

  
  
  Behaviour assembleGift(Actor actor) {
    float giftValue = this.rewardAmount(actor) / 2f ;
    //
    //  Check a few nearby storage depots- vault,
    final World world = base.world ;
    
    final Batch <Venue> depots = new Batch <Venue> () ;
    world.presences.sampleFromKeys(
      actor, world, 2, depots,
      SupplyDepot.class,
      StockExchange.class,
      VaultSystem.class
    ) ;
    
    Venue pickDepot = null ;
    Item pickItem = null ;
    float bestRating = 0, amount ;
    
    for (Venue depot : depots) for (Service type : GIFT_TYPES) {
      amount = depot.stocks.amountOf(type) ;
      amount = Math.min(amount, giftValue / type.basePrice) ;
      amount = Math.min(amount, 5) ;
      final Item item = Item.withAmount(type, amount) ;
      final float rating = amount * type.basePrice ;
      //
      //  Get the most valuable good of bulk less than 5 and under gift value.
      if (rating > bestRating) {
        bestRating = rating ;
        pickItem = item ;
        pickDepot = depot ;
      }
    }
    if (pickItem == null) return null ;
    
    final Delivery delivery = new Delivery(pickItem, pickDepot, actor) ;
    return delivery ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    d.append("\n\nArea: ") ;
    if (begun()) d.append(SETTING_NAMES[contactType]) ;
    else d.append(new Description.Link(SETTING_NAMES[contactType]) {
      public void whenClicked() {
        contactType = (contactType + 1) % SETTING_NAMES.length ;
      }
    }) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Contact Mission", this) ;
    d.append(" around ") ;
    d.append(subject) ;
  }
}



