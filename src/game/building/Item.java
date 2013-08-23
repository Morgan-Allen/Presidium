/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.building ;
import src.game.common.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;



/**  More representative of the asbtract 'listing' of an item than a specific
  *  concrete object.
  *  
  *  Setting quality or amount to -1 will allow for matches with items of any
  *  quality and amount.
  */
public class Item implements BuildConstants {
  
  
  /**  Type definition.
    */
  final static String QUAL_NAMES[] = {
    "Crude", "Basic", "Standard", "Quality", "Luxury"
  } ;
  final static float PRICE_MULTS[] = {
    1.0f, 2.0f, 3.0f, 4.0f, 5.0f
  } ;
  
  
  
  /**  Field definitions, standard constructors and save/load functionality-
    */
  final public static float ANY = Float.NEGATIVE_INFINITY ;
  final public static int MAX_QUALITY = 4 ;
  
  
  final public Service type ;
  final public Session.Saveable refers ;
  final public float amount ;
  final public int quality ;
  //  TODO:  Make quality an atttribute?  Yeah.  It's simpler.
  
  
  private Item(
    Service type, Session.Saveable refers, float amount, int quality
  ) {
    this.type = type ;
    this.amount = amount ;
    this.quality = quality ;
    this.refers = refers ;
  }
  
  
  public static Item withAmount(Service type, float amount) {
    return new Item(type, null, amount, -1) ;
  }
  
  
  public static Item withAmount(Item item, float amount) {
    return new Item(item.type, item.refers, amount, item.quality) ;
  }
  
  
  public static Item withQuality(Service type, int quality) {
    return new Item(type, null, 1, quality) ;
  }
  
  
  public static Item withType(Service type) {
    return new Item(type, null, -1, -1) ;
  }
  /*
  public Item(Item item) {
    this(item.type, item.amount, item.refers) ;
  }
  

  public Item(Item item, float amount) {
    this(item.type, amount, item.refers) ;
  }
  
  
  public Item(Type type, Quality quality) {
    this(type, 1, quality) ;
  }
  
  
  public Item(Type type) {
    this(type, 1) ;
  }
  
  
  public Item(Type type, float amount) {
    this(type, amount > 0 ? amount : ANY, null) ;
  }
  //*/
  
  
  public static Item loadFrom(Session s) throws Exception {
    final int typeID = s.loadInt() ;
    if (typeID == -1) return null ;
    return new Item(
      ALL_ITEM_TYPES[typeID], s.loadObject(),
      s.loadFloat(), s.loadInt()
    ) ;
  }
  
  
  public static void saveTo(Session s, Item item) throws Exception {
    if (item == null) { s.saveInt(-1) ; return ; }
    s.saveInt(item.type.typeID) ;
    s.saveObject(item.refers) ;
    s.saveFloat(item.amount) ;
    s.saveInt(item.quality) ;
  }
  
  
  public boolean equals(Object o) {
    final Item i = (Item) o ;
    return i.type == type && i.refers == refers ;
  }
  
  
  public int hashCode() {
    return type.typeID * 13 + (refers == null ? 0 : (refers.hashCode() % 13)) ;
  }
  
  
  
  /**  Matching/equality functions-
    */
  public boolean matchKind(Item item) {
    if (this.type != item.type) return false ;
    if (this.refers != null) {
      if (item.refers == null) return false ;
      if (! this.refers.equals(item.refers)) return false ;
    }
    return true ;
  }
  
  
  public boolean matches(Item item) {
    if (item == null) return false ;
    if (amount != ANY && item.amount < this.amount) return false ;
    return matchKind(item) ;
  }
  
  
  public boolean isMatch() {
    return amount == ANY ;
  }
  
  
  public int price() {
    final int q = quality ;
    if (q == 1) return type.basePrice ;
    return (int) (type.basePrice * PRICE_MULTS[q]) ;
  }
  
  
  /**  Rendering/interface functions-
    */
  public String toString() {
    if (refers != null) {
      return refers+" "+type ;
    }
    else if (quality != -1) return QUAL_NAMES[quality]+" "+type ;
    else return ((int) amount)+" "+type ;
  }
}







/**  Qualities are used to annotate personal effects, such as weapons and
  *  outfits.
  */
/*
public static class Quality implements Session.Saveable {
  
  final int level ;
  
  private Quality(int level) {
    this.level = level ;
  }
  
  public static Session.Saveable loadConstant(Session s) throws Exception {
    return QUALITIES[s.loadInt()] ;
  }
  
  public void saveState(Session s) throws Exception {
    s.saveInt(level) ;
  }
  
  public String toString() { return QUAL_NAMES[level] ; }
}

final public static Quality
  CRUDE_QUALITY    = new Quality(0),
  BASIC_QUALITY    = new Quality(1),
  STANDARD_QUALITY = new Quality(2),
  HIGH_QUALITY     = new Quality(3),
  LUXURY_QUALITY   = new Quality(4),
  QUALITIES[] = {
    CRUDE_QUALITY,
    BASIC_QUALITY,
    STANDARD_QUALITY,
    HIGH_QUALITY,
    LUXURY_QUALITY 
  } ;
public static Quality quality(int level) {
  return QUALITIES[Visit.clamp(level, 5)] ;
}
//*/





