/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.common.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.* ;
import src.util.* ;



public class MissionsTab extends InfoPanel {
  
  
  final static String
    IMG_DIR = "media/GUI/Missions/" ;
  final public static Texture
    STRIKE_ICON   = Texture.loadTexture(IMG_DIR+"mission_strike.png"  ),
    RECON_ICON    = Texture.loadTexture(IMG_DIR+"mission_recon.png"   ),
    CONTACT_ICON  = Texture.loadTexture(IMG_DIR+"mission_contact.png" ),
    SECURITY_ICON = Texture.loadTexture(IMG_DIR+"mission_security.png") ;
  final public static ImageModel
    STRIKE_MODEL = ImageModel.asFlatModel(
      MissionsTab.class, STRIKE_ICON, 1, 1
    ) ;
  
  
  
  public MissionsTab(BaseUI UI) {
    super(UI, null, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }
  
  
  protected void updateText(BaseUI UI, Text headerText, Text detailText) {
    //super.updateText(UI, headerText, detailText) ;
    headerText.setText("Missions") ;
    detailText.setText("") ;
    //
    //  List Strike, Recon, Contact and Security missions for now.
    
    detailText.insert(STRIKE_ICON, 40) ;
    detailText.append(" Strike Mission\n") ;
    detailText.append(new Text.Clickable() {
      public String fullName() { return "Target" ; }
      public void whenClicked() { initStrikeTask() ; }
    }) ;
    detailText.append("\n") ;
    //  TODO:  You also need to list similar missions, and give info.
    
    
    detailText.insert(RECON_ICON, 40) ;
    detailText.append(" Recon Mission\n") ;
    detailText.append(new Text.Clickable() {
      public String fullName() { return "Target" ; }
      public void whenClicked() { initReconTask() ; }
    }) ;
    detailText.append("\n") ;
    
    detailText.insert(CONTACT_ICON, 40) ;
    detailText.append(" Contact Mission\n") ;
    detailText.append(new Text.Clickable() {
      public String fullName() { return "Target" ; }
      public void whenClicked() { initContactTask() ; }
    }) ;
    detailText.append("\n") ;
    
    detailText.insert(SECURITY_ICON, 40) ;
    detailText.append(" Strike Mission\n") ;
    detailText.append(new Text.Clickable() {
      public String fullName() { return "Target" ; }
      public void whenClicked() { initSecurityTask() ; }
    }) ;
    detailText.append("\n") ;
  }
  
  
  private void appendHelp(String helpString) {
  }
  
  
  
  //
  //  TODO:  A lot of this will have functionality in common with the
  //  InstallTask class and other mission placements.  Consider factoring it
  //  out.
  
  private void previewFlag(Sprite flagSprite, Target picked, boolean valid) {
    if (! valid) {
      final World world = UI.world() ;
      final Vec3D onGround = world.pickedGroundPoint(UI, UI.rendering.port) ;
      flagSprite.position.setTo(onGround) ;
      flagSprite.colour = Colour.RED ;
    }
    else {
      final Element e = (Element) picked ;
      flagSprite.position.setTo(e.viewPosition(null)) ;
      flagSprite.position.z += e.height() ;
      flagSprite.colour = Colour.GREEN ;
    }
    UI.rendering.addClient(flagSprite) ;
  }
  
  
  protected void initStrikeTask() {
    final Sprite flagSprite = STRIKE_MODEL.makeSprite() ;
    UI.beginTask(new TargetTask(UI, STRIKE_ICON) {
      
      boolean validPick(Target pick) {
        return pick instanceof Mobile || pick instanceof Fixture ;
      }
      
      void previewAt(Target picked, boolean valid) {
        previewFlag(flagSprite, picked, valid) ;
      }
      
      void performAt(Target picked) {
        UI.played().addMission(new StrikeMission(picked)) ;
      }
    }) ;
  }
  
  
  protected void initReconTask() {
  }
  
  
  protected void initContactTask() {
  }
  
  
  protected void initSecurityTask() {
  }
}













