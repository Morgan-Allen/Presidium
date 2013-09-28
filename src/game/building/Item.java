/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.building ;
import src.game.common.* ;
import src.game.common.Session.Saveable ;
import src.game.actors.* ;
import src.util.* ;
import src.user.* ;



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
  final public static int ANY = -1 ;
  final public static int MAX_QUALITY = 4 ;
  
  final public Service type ;
  final public Saveable refers ;
  final public float amount ;
  
  //
  //  TODO:  Make quality a float, so you can blend and average it.
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
    return new Item(type, null, amount, 0) ;
  }
  
  
  public static Item withAmount(Item item, float amount) {
    final Item i = new Item(item.type, item.refers, amount, item.quality) ;
    return i ;
  }
  
  
  public static Item withReference(Service type, Saveable refers) {
    return new Item(type, refers, 1, 0) ;
  }
  
  
  public static Item withReference(Item item, Saveable refers) {
    return new Item(item.type, refers, item.amount, item.quality) ;
  }
  
  
  public static Item withQuality(Service type, int quality) {
    return new Item(type, null, 1, Visit.clamp(quality, 5)) ;
  }
  
  
  public static Item withQuality(Item item, int quality) {
    return new Item(
      item.type, item.refers, item.amount, Visit.clamp(quality, 5)
    ) ;
  }
  
  
  //*
  public static Item asMatch(Service type, Saveable refers) {
    return new Item(type, refers, ANY, ANY) ;
  }
  
  
  public static Item asMatch(Service type, int quality) {
    return new Item(type, null, ANY, quality) ;
  }
  
  
  public static Item asMatch(Service type, Saveable refers, int quality) {
    return new Item(type, refers, ANY, quality) ;
  }
  //*/
  
  
  
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
    return matchKind((Item) o) ;
  }
  
  
  public int hashCode() {
    return
      (type.typeID * 13 * 5) + quality +
      ((refers == null ? 0 : (refers.hashCode() % 13)) * 5) ;
  }
  
  
  
  /**  Matching/equality functions-
    */
  protected boolean matchKind(Item item) {
    if (this.type != item.type) return false ;
    if (this.refers != null) {
      if (item.refers == null) return false ;
      if (! this.refers.equals(item.refers)) return false ;
    }
    if (this.quality != ANY) {
      if (item.quality != this.quality) return false ;
    }
    return true ;
  }
  
  /*
  public boolean matches(Item item) {
    if (item == null) return false ;
    if (quality != ANY && item.quality != this.quality) return false ;
    if (amount != ANY && item.amount < this.amount) return false ;
    return matchKind(item) ;
  }
  //*/
  
  
  public boolean isMatch() {
    return amount == ANY || quality == ANY ;
  }
  //*/
  
  
  public int price() {
    final int q = quality ;
    if (q == ANY) return (int) (type.basePrice * amount) ;
    return (int) (type.basePrice * PRICE_MULTS[q] * amount) ;
  }
  
  
  
  /**  Rendering/interface functions-
    */
  //
  //  TODO:  Replace this with a describeTo function, so that you can hyperlink
  //         to referenced objects and give them proper names.
  public void describeTo(Description d) {
    String s = ""+type ;
    if (quality != ANY && type.form != FORM_COMMODITY) {
      s = QUAL_NAMES[quality]+" "+s ;
    }
    if (amount != ANY) s = (I.shorten(amount, 1))+" "+s ;
    d.append(s) ;
    if (refers != null) {
      d.append(" (") ;
      d.append(refers) ;
      d.append(")") ;
    }
  }

  public String toString() {
    final StringDescription SD = new StringDescription() ;
    describeTo(SD) ;
    return SD.toString() ;
  }
  
  /*
  public String toString() {
    String s = ""+type ;
    if (quality != ANY && type.form != FORM_COMMODITY) {
      s = QUAL_NAMES[quality]+" "+s ;
    }
    if (amount != ANY) s = (I.shorten(amount, 1))+" "+s ;
    if (refers != null) {
      s = s+" ("+refers+")" ;
    }
    return s ;
  }
  //*/
}












