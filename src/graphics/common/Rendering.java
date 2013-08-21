/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.common ;
import src.util.* ;
import src.graphics.widgets.HUD ;
import org.lwjgl.opengl.* ;



public class Rendering {

  
  
  final public Viewport port ;
  final public Lighting lighting ;
  private int viewWide, viewHigh ;
  private List <Client> clients = new List <Client> () ;
  private HUD HUD = null ;
  
  
  
  public Rendering(
    int xd, int yd,
    int hz, boolean full
  ) {
    try {
      DisplayMode
        choices[] = Display.getAvailableDisplayModes(),
        chosen = choices[0] ;
      float bestFit = Float.POSITIVE_INFINITY, fit ;
      for (DisplayMode mode : choices) {
        if (mode.getWidth() < xd) continue ;
        if (mode.getHeight() < yd) continue ;
        //
        //  I always get zero here, for some reason-
        //if (mode.getFrequency() < hz) continue ;
        fit = mode.getWidth() + mode.getHeight() - (xd + yd) ;
        if (fit < bestFit) {
          chosen = mode ;
          bestFit = fit ;
        }
      }
      viewWide = chosen.getWidth() ;
      viewHigh = chosen.getHeight() ;
      I.say("View width/height are: "+viewWide+"/"+viewHigh) ;
      Display.setDisplayMode(chosen) ;
      Display.setFullscreen(full) ;
      Display.setResizable(true) ;
      Display.setVSyncEnabled(true) ;
      Display.setSwapInterval(25) ;
      Display.create() ;
    }
    catch (Exception e) {
      e.printStackTrace() ;
    }
    port = new Viewport() ;
    lighting = new Lighting() ;
    lighting.setup(1.0f, 1.0f, 1.0f, true, true) ;
    lighting.direct(Lighting.DEFAULT_ANGLE.normalise()) ;
  }
  
  
  /**  A generalised interface for stuff that wants to be rendered-
    */
  void initSettings() {
    GL11.glEnable(GL11.GL_TEXTURE_2D) ;
    GL11.glEnable(GL11.GL_LIGHTING) ;
    GL11.glEnable(GL11.GL_LIGHT0) ;
    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY) ;
    GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY) ;
    GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY) ;
    
    //
    //  Consider having most sprites ignore the depth test and depth-mask
    //  functions.  Then you can just sort them manually.
    
    GL11.glEnable(GL11.GL_NORMALIZE) ;  //  IF MISSING, COLOURS ARE TOO BRIGHT.
    //GL11.glDisable(GL11.GL_DEPTH_TEST) ;
    GL11.glEnable(GL11.GL_DEPTH_TEST) ;
    GL11.glDepthFunc(GL11.GL_LEQUAL)  ;
    GL11.glDepthMask(true) ;
    GL11.glEnable(GL11.GL_CULL_FACE)  ;
    GL11.glCullFace(GL11.GL_BACK) ;
    
    GL11.glEnable(GL11.GL_BLEND) ;
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA) ;
    GL11.glDisable(GL11.GL_ALPHA_TEST) ;
    /*
    GL11.glEnable(GL11.GL_ALPHA_TEST) ;
    GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f) ;
    //*/
    GL11.glEnable(GL11.GL_COLOR_MATERIAL) ;
    GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE) ;
  }
  
  
  public void updateViews() {
    viewWide = Display.getWidth()  ;
    viewHigh = Display.getHeight() ;
    if (Display.isCloseRequested()) {
      Display.destroy() ;
      System.exit(0) ;
    }
    else {
      port.viewBounds.set(
        0, 0, viewWide, viewHigh
      ) ;
      port.updateView() ;
      port.setIsoMode() ;
    }
  }
  
  
  public static interface Client {
    void renderTo(Rendering rendering) ;
    int[] GL_disables() ;
  }
  
  
  public void addClient(Client client) {
    clients.add(client) ;
  }
  
  
  public void clearDepth() {
    addClient(new Client() {
      public void renderTo(Rendering rendering) {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT) ;
      }
      public int[] GL_disables() {
        return new int[0] ;
      }
    }) ;
  }
  
  
  public void assignHUD(HUD toAssign) {
    HUD = toAssign ;
  }
  
  
  public void clearAll() {
    clients.clear() ;
    HUD = null ;
  }
  
  
  public void renderDisplay() {
    initSettings() ;
    GL11.glClearColor(0.2f, 0.2f, 0.2f, 1) ;
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT) ;
    port.applyView() ;
    lighting.bindLight(0) ;
    
    for (Client client : clients) {
      GL11.glColor4f(1, 1, 1, 1) ;
      GL11.glMatrixMode(GL11.GL_MODELVIEW) ;
      final int disabled[] = client.GL_disables() ;
      if (disabled != null) for (int d : disabled) GL11.glDisable(d) ;
      client.renderTo(this) ;
      if (disabled != null) for (int d : disabled) GL11.glEnable(d) ;
    }
    clients.clear() ;
    
    if (HUD != null) {
      //  Alpha blending doesn't seem to work here.  Fix this.
      GL11.glEnable(GL11.GL_COLOR_MATERIAL) ;
      HUD.renderHUD(new Box2D().set(0, 0, viewWide, viewHigh)) ;
    }
    
    Display.update() ;
  }
  
}



