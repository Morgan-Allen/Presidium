

package src.game.common ;
import src.graphics.common.* ;
import src.util.* ;
import src.game.common.WorldSections.Section ;



/**  This class is used to add transitory or conditional special effects to the
  *  world.
  */
public class Ephemera {
  
  
  /**  Fields, constants, and save/load methods.
    */
  final World world ;
  final Table <WorldSections.Section, List <Ghost>> ghosts = new Table(100) ;
  
  
  protected Ephemera(World world) {
    this.world = world ;
  }
  
  
  protected void loadState(Session s) throws Exception {
  }
  
  
  protected void saveState(Session s) throws Exception {
  }
  
  
  
  /**  Adding ghost FX to the register-
    */
  static class Ghost implements World.Visible {
    
    Tile position ;
    int size ;
    float inceptTime ;
    Sprite sprite ;
    float duration = 2.0f ;
    
    public void renderFor(Rendering r, Base b) {
      r.addClient(sprite) ;
    }
    
    public Sprite sprite() {
      return sprite ;
    }
  }
  
  
  public void addGhost(Element e, float size, Sprite s, float duration) {
    if (s == null) return ;
    final Ghost ghost = new Ghost() ;
    ghost.size = (int) Math.ceil(size) ;
    ghost.inceptTime = world.currentTime() ;
    ghost.sprite = s ;
    ghost.duration = duration ;

    final Vec3D p = s.position ;
    if (e == null) {
      ghost.position = world.tileAt(p.x, p.y) ;
    }
    else {
      ghost.position = e.origin() ;
      e.viewPosition(p) ;
    }
    final Section section = world.sections.sectionAt((int) p.x, (int) p.y) ;
    List <Ghost> SG = ghosts.get(section) ;
    if (SG == null) ghosts.put(section, SG = new List <Ghost> ()) ;
    SG.add(ghost) ;
  }
  
  
  protected Batch <Ghost> visibleFor(Rendering rendering) {
    final Batch <Ghost> results = new Batch <Ghost> () ;
    final Viewport port = rendering.port ;
    
    for (Section section : world.visibleSections(rendering)) {
      final List <Ghost> SG = ghosts.get(section) ;
      if (SG != null) for (Ghost ghost : SG) {
        final float duration = ghost.duration ;
        float timeGone = world.currentTime() - ghost.inceptTime ;
        timeGone += PlayLoop.frameTime() / PlayLoop.UPDATES_PER_SECOND ;
        
        if (timeGone >= duration) { SG.remove(ghost) ; continue ; }
        else {
          final Sprite s = ghost.sprite ;
          if (! port.intersects(s.position, ghost.size)) continue ;
          s.colour = Colour.transparency((duration - timeGone) / duration) ;
          s.update() ;
          results.add(ghost) ;
        }
      }
    }
    return results ;
  }
}





