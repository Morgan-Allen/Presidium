/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.building ;
import src.game.common.* ;
import src.game.common.Session.Saveable ;
import src.game.actors.* ;



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
  final public Saveable refers ;
  final public float amount ;
  final public int quality ;
  
  
  private Item(
    Service type, Saveable refers, float amount, int quality
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
  
  
  public static Item withType(Service type, Saveable refers) {
    return new Item(type, refers, 1, 0) ;
  }
  
  
  public static Item withType(Service type) {
    return new Item(type, null, -1, -1) ;
  }
  
  
  public static Item withQuality(Service type, int quality) {
    return new Item(type, null, 1, quality) ;
  }
  
  
  public static Item withReference(Item item, Saveable refers) {
    return new Item(item.type, refers, item.amount, item.quality) ;
  }
  
  
  
  
  public static Item loadFrom(Session s) throws Exception {
    final int typeID = s.loadInt() ;
    if (typeID == -1) return null ;
    return new Item(
      ALL_ITEM_TYPES[typeID],
      s.loadObject(),
      s.loadFloat(),
      s.loadInt()
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
    if (q == -1) return (int) (type.basePrice * amount) ;
    return (int) (type.basePrice * PRICE_MULTS[q] * amount) ;
  }
  
  
  
  /**  Rendering/interface functions-
    */
  public String toString() {
    if (refers != null) {
      return type+" ("+refers+")" ;
    }
    else if (quality != -1) {
      final String progress = amount == 1 ?
        "" : "("+((int) (amount * 100))+" %)" ;
      return QUAL_NAMES[quality]+" "+type+" "+progress ;
    }
    else return ((int) amount)+" "+type ;
  }
}







