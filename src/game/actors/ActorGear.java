/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */



package src.game.actors ;
import src.game.building.* ;
import src.game.common.* ;
import src.graphics.jointed.* ;
import src.util.* ;



//
//  You need to generate Special Effects for weapon-beams, shield bursts, and
//  acquisitions of credits and loot.



public class ActorGear extends Inventory implements Economy {
  
  
  final public static float
    SHIELD_CHARGE     = 5f,
    SHIELD_SHORTS     = 2f,
    SHIELD_REGEN_TIME = 10f ;
  final public static int
    MAX_RATIONS    = 5,
    MAX_FOOD_TYPES = 5,
    MAX_FUEL_CELLS = 5 ;
  
  private static boolean verbose = false ;
  
  
  final Actor actor ;
  float baseDamage, baseArmour ;
  Item device = null ;
  Item outfit = null ;
  float fuelCells, currentShields ;
  
  
  public ActorGear(Actor actor) {
    super(actor) ;
    this.actor = actor ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveFloat(baseDamage) ;
    s.saveFloat(baseArmour) ;
    Item.saveTo(s, device) ;
    Item.saveTo(s, outfit) ;
    s.saveFloat(fuelCells) ;
    s.saveFloat(currentShields) ;
  }
  

  public void loadState(Session s) throws Exception {
    super.loadState(s) ;
    baseDamage = s.loadFloat() ;
    baseArmour = s.loadFloat() ;
    device = Item.loadFrom(s) ;
    outfit = Item.loadFrom(s) ;
    fuelCells = s.loadFloat() ;
    currentShields = s.loadFloat() ;
  }
  
  
  public void setDamage(float d) {
    baseDamage = d ;
  }
  
  
  public void setArmour(float a) {
    baseArmour = a ;
  }
  
  
  
  /**  Maintenance, updates and spring cleaning-
    */
  public void updateGear(int numUpdates) {
    if (Float.isNaN(credits)) credits = 0 ;
    if (Float.isNaN(taxed)) taxed = 0 ;
    
    if (verbose) I.sayAbout(actor, "Updating gear...") ;
    if (outfit != null) regenerateShields() ;
    else currentShields = 0 ;
    for (Item item : allItems()) {
      if (item.refers instanceof Action) {
        if (verbose) I.sayAbout(actor, "  Applying item effect: "+item.refers) ;
        ((Action) item.refers).applyEffect() ;
      }
    }
  }
  
  
  public boolean addItem(Item item) {
    if (item == null || item.amount == 0) return false ;
    if (item.refers == actor) item = Item.withReference(item, null) ;
    if      (item.type instanceof DeviceType) equipDevice(item) ;
    else if (item.type instanceof OutfitType) equipOutfit(item) ;
    else if (! super.addItem(item)) return false ;
    if (actor.inWorld()) actor.chat.addPhrase("+"+item) ;
    return true ;
  }
  
  
  public void incCredits(float inc) {
    if (Float.isNaN(inc)) I.complain("INC IS NOT-A-NUMBER!") ;
    if (Float.isNaN(credits)) credits = 0 ;
    if (inc == 0) return ;
    final int oldC = (int) credits() ;
    super.incCredits(inc) ;
    final int newC = (int) credits() ;
    if (! actor.inWorld() || oldC == newC) return ;
    String phrase = inc >= 0 ? "+" : "-" ;
    phrase+=" "+(int) Math.abs(inc)+" credits" ;
    actor.chat.addPhrase(phrase) ;
  }
  
  
  public float encumbrance() {
    //
    //  TODO:  Cache this each second?
    float sum = 0 ; for (Item i : allItems()) sum += i.amount ;
    sum /= actor.health.maxHealth() * (1 - actor.health.fatigueLevel()) ;
    return sum * sum ;
  }
  
  
  
  /**  Returns this actor's effective attack damage.  Actors without equipped
    *  weapons, or employing weapons in melee, gain a bonus based on their
    *  physical brawn.
    */
  public float attackDamage() {
    final Item weapon = deviceEquipped() ;
    final float brawnBonus = actor.traits.traitLevel(BRAWN) / 4 ;
    if (weapon == null) return 2 + brawnBonus + baseDamage ;
    final DeviceType type = (DeviceType) weapon.type ;
    final float damage = type.baseDamage * (weapon.quality + 2f) / 4 ;
    if (type.hasProperty(MELEE)) return damage + brawnBonus + baseDamage ;
    else return damage + baseDamage ;
  }
  
  
  public float attackRange() {
    if (deviceType().hasProperty(RANGED))
      return actor.health.sightRange() ;
    else
      return 1 ;
    /*
    final Item weapon = equipment.weaponEquipped() ;
    if (weapon == null) return 1 ;
    final ImplementType type = (ImplementType) weapon.type ;
    return type.baseRange ;
    //*/
  }
  
  
  public boolean meleeWeapon() {
    final Item weapon = deviceEquipped() ;
    if (weapon == null) return true ;
    if (deviceType().hasProperty(MELEE)) return true ;
    return false ;
  }
  
  
  public boolean physicalWeapon() {
    final Item weapon = deviceEquipped() ;
    if (weapon == null) return true ;
    if (deviceType().hasProperty(PHYSICAL)) return true ;
    return false ;
  }
  
  
  public boolean armed() {
    final DeviceType type = deviceType() ;
    return (type != null) && type.baseDamage > 0 ;
  }
  
  
  
  /**  Returns this actor's effective armour rating.  Actors not wearing any
    *  significant armour, or only lightly armoured, gain a bonus based on
    *  their reflexes.
    */
  public float armourRating() {
    final Item armour = outfitEquipped() ;
    final float reflexBonus = actor.traits.traitLevel(REFLEX) / 4 ;
    if (armour == null) return 2 + reflexBonus + baseArmour ;
    final OutfitType type = (OutfitType) armour.type ;
    final float rating = type.defence * (armour.quality + 1) / 4 ;
    if (type.defence <= 10) return rating + reflexBonus + baseArmour ;
    else return rating + baseArmour ;
  }
  
  /*
  //  Armour only provides half protection against energy weapons.
  public float afterArmour(Target threat, float damage, boolean physical) {
    float reduction = armourRating() * Rand.num() ;
    if (! physical) reduction /= 2 ;
    if (reduction > damage) reduction = damage ;
    //  TODO:  Raise possibility of damage to armour itself?
    return damage - reduction ;
  }
  //*/
  
  
  /**  Shield depletion and regeneration are handled here-
    */
  public float shieldCharge() {
    if (outfit == null) return 0 ;
    final OutfitType type = (OutfitType) outfit.type ;
    return type.shieldBonus * currentShields / SHIELD_CHARGE ;
  }
  
  
  public void boostShields(float boost) {
    currentShields += boost ;
  }
  
  
  
  /**  Returns the amount of damage left after shield reductions are taken into
    *  account.  (Also initialises and updates SFX for shields, if needed.)
    */
  /*
  public float afterShields(Target threat, float damage, boolean physical) {
    float reduction = shieldCharge() * Rand.num() ;
    if (reduction > 0 && shieldFX == null) {
      shieldFX = new ShieldFX() ;
      actor.position(shieldFX.position) ;
    }
    if (shieldFX != null) {
      //I.say("Threat is: "+threat.targPos()) ;
      shieldFX.attachBurstFromPoint(threat.position(null), damage > reduction) ;
    }
    if (physical) reduction /= 2 ;
    if (reduction > damage) reduction = damage ;
    currentShields -= reduction ;
    return damage - reduction ;
  }
  //*/
  
  
  private void regenerateShields() {
    final OutfitType type = (OutfitType) outfit.type ;
    final float regenTime =
      SHIELD_REGEN_TIME * 2f / (2 + type.shieldBonus) ;
    final float maxShield =
      (SHIELD_CHARGE + type.shieldBonus) *
      (float) Math.sqrt(fuelCells / MAX_FUEL_CELLS) ;
    if (currentShields < maxShield) {
      final float nudge = maxShield / regenTime ;
      currentShields += nudge ;
      fuelCells -= nudge / 10f ;
      if (currentShields > maxShield) currentShields = maxShield ;
      if (fuelCells < 0) fuelCells = 0 ;
    }
    else {
      currentShields -= SHIELD_CHARGE / regenTime ;
      if (currentShields < maxShield) currentShields = maxShield ;
    }
  }
  
  /*
  public ShieldFX shieldFX() {
    //if (shieldFX == null) shieldFX = new ShieldFX() ;
    return shieldFX ;
  }
  //*/
  

  /**  Here we deal with equipping/removing Devices-
    */
  public void equipDevice(Item device) {
    if (device != null && ! (device.type instanceof DeviceType))
      return ;
    this.device = device ;
    /*
    final Actor actor = (Actor) owner ;
    final JointSprite sprite = (JointSprite) actor.sprite() ;
    final Item oldItem = this.device ;
    //
    //  Attach/detach the appropriate media-
    if (oldItem != null && sprite != null) {
      final DeviceType oldType = (DeviceType) oldItem.type ;
      sprite.toggleGroup(oldType.groupName, false) ;
    }
    if (device != null && sprite != null) {
      final DeviceType newType = (DeviceType) device.type ;
      sprite.toggleGroup(newType.groupName, true) ;
    }
    //*/
  }
  
  
  public Item deviceEquipped() {
    return device ;
  }
  
  
  public DeviceType deviceType() {
    if (device == null) return null ;
    return (DeviceType) device.type ;
  }
  
  
  
  /**  Here, we deal with applying/removing Outfits-
    */
  public void equipOutfit(Item outfit) {
    if (! (outfit.type instanceof OutfitType)) return ;
    final Actor actor = (Actor) owner ;
    final JointSprite sprite = (JointSprite) actor.sprite() ;
    final Item oldItem = this.outfit ;
    this.outfit = outfit ;
    //
    //  Attach/detach the appropriate media-
    if (oldItem != null) {
      final OutfitType type = (OutfitType) oldItem.type ;
      if (type.skin != null) sprite.removeOverlay(type.skin) ;
    }
    if (outfit != null) {
      final OutfitType type = (OutfitType) outfit.type ;
      if (type.skin != null) sprite.overlayTexture(type.skin) ;
      currentShields = SHIELD_CHARGE + type.shieldBonus ;
    }
  }
  
  
  public Item outfitEquipped() {
    return outfit ;
  }
  
  
  public OutfitType outfitType() {
    if (outfit == null) return null ;
    return (OutfitType) outfit.type ;
  }
}








