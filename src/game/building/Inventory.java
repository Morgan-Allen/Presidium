/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.util.* ;
import src.user.* ;



/**  An inventory allows for the storage, transfer and tracking of discrete
  *  items.
  */
public class Inventory {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public Owner owner ;
  final protected Table <Item, Item> itemTable = new Table(10) ;
  float credits, taxed ;
  
  
  public Inventory(Owner owner) {
    this.owner = owner ;
  }
  
  
  public static interface Owner extends Target, Session.Saveable {
    Inventory inventory() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(itemTable.size()) ;
    for (Item item : itemTable.values()) Item.saveTo(s, item) ;
    s.saveFloat(credits) ;
    s.saveFloat(taxed  ) ;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int i = s.loadInt() ; i-- > 0 ;) {
      final Item item = Item.loadFrom(s) ;
      itemTable.put(item, item) ;
    }
    credits = s.loadFloat() ;
    taxed   = s.loadFloat() ;
  }
  
  
  /**  Writes inventory information to the given text field.
    */
  public void writeInformation(Description text) {
    for (Item item : itemTable.values()) {
      text.append("\n") ;
      text.insert(item.type.pic, 25) ;
      text.append(item) ;
      //text.append("\n"+item) ;
    }
    if (credits() != 0) {
      text.append("\n"+((int) credits())+" credits") ;
      text.append((credits() < 0) ? " (in debt)" : "") ;
    }
    text.append("\n") ;
  }
  
  
  
  /**  Financial balance-
    */
  public void incCredits(float inc) {
    if (inc > 0) {
      credits += inc ;
    }
    else {
      credits += inc ;
      if (credits < 0) {
        taxed += credits ;
        credits = 0 ;
      }
    }
    //owner.afterTransaction() ;
  }
  
  
  public float credits() {
    return credits + taxed ;
  }
  
  
  public float unTaxed() {
    return credits ;
  }
  
  
  public void taxDone() {
    taxed += credits ;
    credits = 0 ;
    //owner.afterTransaction() ;
  }
  
  
  
  /**  Returns whether this inventory is empty.
   */
  public boolean empty() {
    return itemTable.size() == 0 && (credits + taxed) <= 0 ;
  }
  
  
  public Batch <Item> allItems() {
    final Batch <Item> allItems = new Batch <Item> () ;
    for (Item item : itemTable.values()) allItems.add(item) ;
    return allItems ;
  }
  
  
  public void clearItems(Service type) {
    itemTable.remove(type) ;
  }
  
  
  public void removeAllItems() {
    itemTable.clear() ;
    credits = taxed = 0 ;
    //owner.afterTransaction() ;
  }
  
  
  
  /**  Adds the given item to the inventory.  If the item is one with 'free
    *  terms' used for matching purposes, returns false- only fully-specified
    *  items can be added.
    */
  public boolean addItem(Item item) {
    if (item.isMatch()) return false ;
    //
    //  Check to see if a similar item already exists.
    final Item oldItem = itemTable.get(item) ;
    float amount = item.amount ;
    if (oldItem != null) amount += oldItem.amount ;
    final Item entered = Item.withAmount(item, amount) ;
    itemTable.put(entered, entered) ;
    //owner.afterTransaction() ;
    return true ;
  }
  
  
  public void addItem(Service type, float amount) {
    addItem(Item.withAmount(type, amount)) ;
  }
  
  
  
  /**  Removes the given item from this inventory.  The item given must have a
    *  single unique match, and the match must be greater in amount.  Returns
    *  false otherwise.
    */
  public boolean removeItem(Item item) {
    final Item oldItem = itemTable.get(item) ;
    if (oldItem == null || oldItem.amount < item.amount) {
      itemTable.remove(item) ;
      return false ;
    }
    final float newAmount = oldItem.amount - item.amount ;
    if (newAmount <= 0) itemTable.remove(oldItem) ;
    else {
      final Item entered = Item.withAmount(item, newAmount) ;
      itemTable.put(entered, entered) ;
    }
    //owner.afterTransaction() ;
    return true ;
  }
  

  public float transfer(Service type, Owner to) {
    float amount = 0 ;
    for (Item item : matches(type)) {
      removeItem(item) ;
      to.inventory().addItem(item) ;
      amount += item.amount ;
    }
    return amount ;
  }
  
  
  public float transfer(Item item, Owner to) {
    final float amount = Math.min(item.amount, amountOf(item)) ;
    final Item transfers = Item.withAmount(item, amount) ;
    removeItem(transfers) ;
    to.inventory().addItem(transfers) ;
    return amount ;
  }
  
  
  
  /**  Returns the total amount of the given item type, for all owners and
    *  qualities.
    */
  public float amountOf(Service type) {
    return amountOf(Item.withType(type)) ;
  }
  
  
  
  /**  Returns all matches with the given item.
    */
  public Batch <Item> matches(Item item) {
    final Batch <Item> matches = new Batch <Item> (4) ;
    //final Stack <Item> ofType = types.get(item.type) ;
    //if (ofType == null) return matches ;
    for (Item found : itemTable.values()) {
      if (item.matches(found)) matches.add(found) ;
    }
    return matches ;
  }
  
  
  public Batch <Item> matches(Service type) {
    final Batch <Item> matches = new Batch <Item> (4) ;
    for (Item found : itemTable.values()) {
      if (found.type == type) matches.add(found) ;
    }
    return matches ;
  }
  
  
  
  /**  Returns the sum total amount of all matches with the given item.
    */
  public float amountOf(Item item) {
    float amount = 0 ;
    for (Item found : itemTable.values()) {
      if (item.matchKind(found)) amount += found.amount ;
    }
    return amount ;
  }
  
  
  
  /**  Returns whether this inventory has enough of the given item to satisfy
    *  match criteria.
    */
  public boolean hasItem(Item item) {
    final float amount = amountOf(item) ;
    if (item.amount == Item.ANY) return amount > 0 ;
    else return amount >= item.amount ;
  }
}




