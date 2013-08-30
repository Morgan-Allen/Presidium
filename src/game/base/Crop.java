/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;



public class Crop extends Element {
  
  
  final static int
    MAX_GROWTH = 3 ;
  
  final BotanicalStation parent ;
  final int varID ;
  float growStage, health ;
  //  Health can be negative when/if diseased!  And spreads more easily to
  //  crops of the same type.
  
  
  Crop(BotanicalStation parent, int varID) {
    this.parent = parent ;
    this.varID = varID ;
    growStage = 0 ;
    health = 1 ;
    attachSprite(BotanicalStation.speciesModel(varID, 0).makeSprite()) ;
  }
  
  
  public Crop(Session s) throws Exception {
    super(s) ;
    varID = s.loadInt() ;
    growStage = s.loadFloat() ;
    health = s.loadFloat() ;
    parent = (BotanicalStation) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(varID) ;
    s.saveFloat(growStage) ;
    s.saveFloat(health) ;
    s.saveObject(parent) ;
  }
  
  
  protected void onGrowth() {
    final float growChance = Visit.clamp(
      origin().habitat().moisture() / 10f, 0.2f, 0.8f
    ) ;
    if (Rand.num() < growChance) growStage++ ;
    if (growStage > MAX_GROWTH) growStage = MAX_GROWTH ;
    //
    //  Update the sprite-
    final Model m = BotanicalStation.speciesModel(varID, (int) growStage) ;
    ((ImageSprite) sprite()).setModel((ImageModel) m) ;
  }
  
  
  public void exitWorld() {
    super.exitWorld() ;
    parent.planted.remove(this) ;
  }
  
  
  public int pathType() {
    return Tile.PATH_HINDERS ;
  }
  
  
  public int owningType() {
    return Element.FIXTURE_OWNS ;
  }
}




