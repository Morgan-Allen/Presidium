/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.jointed ;
import src.util.* ;
import src.graphics.common.* ;
//import java.io.* ;
import java.nio.* ;
import org.lwjgl.opengl.GL11 ;



//
//  Next, you'll want to selectively apply animations just to particular
//  joints (and their descendants.)  Maybe do some rotation/targeting/inverse-
//  kinematics in future.


public class JointSprite extends Sprite {
  
  
  
  /**  Field definitions and constructor-
    */
  JointModel model ;
  //JointAnimation animation = null ;
  final Mat3D transform = new Mat3D().setIdentity() ;
  
  Joint
    joints[],
    root ;
  Vec3D verts[] ;
  Stack <Group> groups = new Stack <Group> () ;
  
  final static int
    ATTACH_NONE  = 0,
    ATTACH_JOINT = 1,
    ATTACH_FULLY = 2 ;
  Stack <Texture> overlays = new Stack <Texture> () ;
  int attachMode = ATTACH_NONE ;
  
  
  
  public JointSprite(JointModel model) {
    this.model = model ;
    //
    //  Geometry setup is done first.
    int
      numV = model.vertAbs.length,
      numG = model.groups.length,
      numJ = model.joints.length ;
    verts = new Vec3D[numV] ;
    for (int n = 0 ; n < numV ; n++)
      verts[n] = new Vec3D() ;
    //
    //  Set up initial groups...
    Group group ;
    for (int n = 0 ; n < numG ; n++) {
      groups.addLast(group = new Group()) ;
      group.setup(model.groups[n], this) ;
    }
    //
    //  Joint setup is performed here.
    joints = new Joint[numJ] ;
    for (int n = 0 ; n < numJ ; n++) {
      joints[n] = new Joint(model.joints[n], this) ;
    }
    root = joints[model.root.ID] ;
  }
  
  
  
  /**  Setting animations-
    */
  public void setAnimation(String animName, float progress) {
    setJointAnimation(root.model.name, animName, progress) ;
  }
  
  
  public void setJointAnimation(
    String jointName, String animName, float progress
  ) {
    final int jID = model.jointID(jointName) ;
    if (jID == -1) return ;// I.complain("No Such Joint: "+jointName) ;
    final Model.AnimRange range = rangeFor(animName) ;
    if (range == null) return ;// I.complain("No such animation: "+animName) ;
    final Joint joint = joints[jID] ;
    if (joint.animation != null && joint.animation.animRange == range) {
      joint.animation.setProgress(progress) ;
      return ;
    }
    final JointAnimation anim = new JointAnimation(this, range) ;
    anim.setProgress(progress) ;
    applyAnimation(joint, anim) ;
  }
  
  
  private void applyAnimation(Joint joint, JointAnimation a) {
    joint.animation = a ;
    for (JointModel.Joint mJ : joint.model.children) {
      final Joint kid = this.joints[mJ.ID] ;
      applyAnimation(kid, a) ;
    }
  }
  
  
  
  /**  Here we have methods related to adding and removing attachments-
    */
  public void overlayTexture(Texture overlaid) {
    if (overlaid == null) return ;
    overlays.addLast(overlaid) ;
  }
  
  public void removeOverlay(Texture overlaid) {
    if (overlaid == null) return ;
    overlays.remove(overlaid) ;
  }
  
  
  
  /**  Allows a given group to be enabled/disabled.
    */
  public void toggleGroup(String groupName, boolean toggle) {
    for (Group g : groups) if (g.modelGroup.name.equals(groupName)) {
      g.toggled = toggle ;
      return ;
    }
  }
  
  
  public Vec3D attachPoint(String function) {
    Joint j = root ;
    for (Model.AttachPoint point : model.attachPoints()) {
      if (! point.function.equals(function)) continue ;
      final int jointID = model.jointID(point.pointName) ;
      if (jointID == -1) continue ;
      j = this.joints[jointID] ;
      break ;
    }
    final Vec3D JP = new Vec3D(j.position) ;
    JP.scale(this.scale * model.scale()) ;
    JP.add(position) ;
    return JP ;
  }
  
  
  
  /**  Default drawing and update methods.
    */
  ///final int ENABLES[] = { GL11.GL_DEPTH_TEST } ;
  public int[] GL_disables() {
    return null ;
  }
  
  
  public Model model() {
    return model ;
  }
  
  
  public void renderTo(Rendering rendering) {
    ///if (true) return ;
    ///I.say("Rendering joint sprite") ;
    root.rotation.setIdentity() ;
    root.rotation.rotateZ((float) Math.toRadians(270 - rotation)) ;
    root.rotation.trans(root.model.rotation, root.rotation) ;
    root.position.setTo(root.model.position) ;
    transform.trans(root.rotation) ;
    root.update() ;
    //
    //  TODO:  Leave model vertices stored in absolute space, so that multiple
    //  bone weights can be supported better later on?
    //
    //  Translate model vertices from relative to absolute coordinates...
    //  final float absScale = scale * model.scale() ;
    Vec3D v ;
    for (int n = verts.length; n-- > 0 ;) {
      v = verts[n].setTo(model.vertRel[n]) ;
      joints[model.joinID[n]].trans(v) ;
    }
    //
    //  And transfer geometry data to native buffers-
    for (Group group : groups) {
      group.updateGroup() ;
      if (! group.toggled) continue ;
      group.modelGroup.material.texture.bindTex() ;
      group.renderTo(rendering) ;
      //
      //  We only render overlay textures on the main skin surface.
      if (! group.modelGroup.material.name.equals("main_skin")) continue ;
      for (Texture overlaid : overlays) {
        overlaid.bindTex() ;
        group.renderTo(rendering) ;
      }
    }
  }
  
  
  
  static class Joint extends Tran3D {
    
    final JointModel.Joint model ;
    final JointSprite sprite ;
    private JointAnimation animation = null ;
    
    
    Joint(JointModel.Joint j, JointSprite s) {
      sprite = s ;
      model = j ;
    }
    

    void update() {
      if (model.parent != null) {
        final Joint parent = sprite.joints[model.parent.ID] ;
        if (animation == null) {
          setTo(model) ;
        }
        else {
          animation.getTransform(model, this) ;
          model.trans(this, this) ;
        }
        parent.trans(this, this) ;
      }
      for (JointModel.Joint child : model.children) {
        sprite.joints[child.ID].update() ;
      }
    }
  }
  
  
  static class Group implements Rendering.Client {
    
    JointSprite sprite ;
    JointModel.Group modelGroup ;
    boolean toggled = true ;
    
    private float vertA[], normA[] ;
    private FloatBuffer vertB, normB, textB ;
    
    
    Group setup(JointModel.Group g, JointSprite s) {
      modelGroup = g ;
      sprite = s ;
      final Object geometry[] = modelGroup.getGeometry() ;
      vertA = (float[]) geometry[JointModel.Group.VERTA_GI] ;
      normA = (float[]) geometry[JointModel.Group.NORMA_GI] ;
      vertB = (FloatBuffer) geometry[JointModel.Group.VERTB_GI] ;
      normB = (FloatBuffer) geometry[JointModel.Group.NORMB_GI] ;
      textB = (FloatBuffer) geometry[JointModel.Group.UVB_GI] ;
      return this ;
    }
    
    
    protected void updateGroup() {
      final int numP = modelGroup.polyID.length ;
      int i = 0, n, v , p, c ;
      Vec3D normal = new Vec3D(), vertex ;
      Joint joint ;
      final JointModel model = modelGroup.model ;
      for (p = 0 ; p < numP ; p++) {
        n = modelGroup.polyID[p] * 3 ;
        for (c = 3 ; c-- > 0 ; n++) {
          v = model.vertID[n] ;
          joint = sprite.joints[model.joinID[v]] ;
          joint.rotation.trans(normal.setTo(model.norms[n])) ;
          vertex = sprite.verts[v] ;
          normA[i]   = normal.x ;
          vertA[i++] = vertex.x ;
          normA[i]   = normal.y ;
          vertA[i++] = vertex.y ;
          normA[i]   = normal.z ;
          vertA[i++] = vertex.z ;
        }
      }
      normB.clear() ;
      vertB.clear() ;
      vertB.put(vertA).flip() ;
      normB.put(normA).flip() ;
    }


    public void renderTo(Rendering rendering) {
      rendering.port.setIsoMode() ;
      final Colour c = sprite.colour ;
      final float f = sprite.fog ;
      if (c == null) GL11.glColor4f(f, f, f, 1) ;
      else GL11.glColor4f(c.r * f, c.g * f, c.b * f, c.a) ;
      MeshBuffer.render(
        sprite.scale * sprite.model.scale(), 0, sprite.position,
        vertB, normB, textB, -1
      ) ;
    }
    
    
    public int[] GL_disables() {
      return null ;
    }
  }
}



