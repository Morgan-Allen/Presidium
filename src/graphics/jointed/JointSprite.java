/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.jointed ;
import src.util.* ;
import src.graphics.common.* ;
import java.io.* ;
import java.nio.* ;
import org.lwjgl.opengl.GL11 ;




public class JointSprite extends Sprite {
  
  
  /**  Field definitions and constructor-
    */
  JointModel model ;
  Animation animation = null ;
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
    final Model.AnimRange range = rangeFor(animName) ;
    if (range == null) return ;
    if (animation == null || animation.animRange != range) {
      animation = new Animation(this, range) ;
    }
    animation.setProgress(progress) ;
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
  
  
  
  /**  Default drawing and update methods.
    */
  public int[] GL_disables() {
    return null ;
  }
  
  
  public Model model() {
    return model ;
  }
  
  
  public void renderTo(Rendering rendering) {
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
      for (Texture overlaid : overlays) {
        overlaid.bindTex() ;
        group.renderTo(rendering) ;
      }
    }
  }
  
  
  static class Joint extends Tran3D {
    
    final JointModel.Joint model ;
    final JointSprite sprite ;
    
    
    Joint(JointModel.Joint j, JointSprite s) {
      sprite = s ;
      model = j ;
    }
    

    void update() {
      if (model.parent != null) {
        final Joint parent = sprite.joints[model.parent.ID] ;
        if (sprite.animation == null) {
          setTo(model) ;
        }
        else {
          sprite.animation.getTransform(model, this) ;
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

  
  public static class Animation {
    
    JointSprite sprite ;
    Model.AnimRange animRange ;
    float
      progress = 0,
      currTime = 0 ;
    private int
      rotFrame = 1,
      posFrame = 1 ;
    private Quat
      rotation = new Quat(),
      position = new Quat() ;
    
    
    Animation(JointSprite sprite, Model.AnimRange range) {
      this.sprite = sprite ;
      this.animRange = range ;
    }
    
    void setProgress(float p) {
      progress = p ;
      if (progress < 0) progress++ ;
      if (progress > 1) progress %= 1 ;
      currTime =
        (animRange.start * (1 - progress)) +
        (animRange.end * progress) ;
    }
    
    Tran3D getTransform(JointModel.Joint joint, Tran3D result) {
      getFrame(joint, rotation, true) ;
      getFrame(joint, position, false) ;
      result.position.set(position.x, position.y, position.z) ;
      rotation.putMatrixForm(result.rotation) ;
      return result ;
    }
    
    Quat getRotation(JointModel.Joint j, Quat r) {
      return getFrame(j, r, true) ;
    }
    
    Quat getPosition(JointModel.Joint j, Quat r) {
      return getFrame(j, r, false) ;
    }
    
    private Quat getFrame(
      JointModel.Joint joint, Quat result, boolean rotation
    ) {
      JointModel.Keyframe frames[] = (
        rotation ? joint.rotFrames : joint.posFrames
      ) ;
      int numF = frames.length ;
      //Test.report("frames: " + numF) ;
      
      if (numF == 0)
        return result.set(0, 0, 0, 0) ;
      if ((numF == 1) || (currTime <= frames[0].time))
        return result.set(frames[0]) ;
      if (currTime >= frames[numF - 1].time)
        return result.set(frames[numF - 1]) ;
      //
      //  Thus we ascertain that the joint has at least two keyframes, and that
      //  the current time is within the interval of defined keyframes for this
      //  joint.
      int
        frame = Math.max(Math.min(
          (rotation ? rotFrame : posFrame),
          frames.length - 1), 0),
        inc = (frames[frame].time <= currTime) ? 1 : -1 ;  //forward or back?
      JointModel.Keyframe
        prior = frames[0],
        after = prior ;
      //
      //  Try to find the frame that best matches our needs...
      for (; (frame > 0) && (frame < numF) ; frame += inc) {
        prior = frames[frame - 1] ;
        after = frames[frame] ;
        if ((prior.time <= currTime) && (after.time >= currTime))
          break ;
      }
      
      float
        alpha = (currTime - prior.time) / (after.time - prior.time),
        nA = (1 - alpha) ;
      if (rotation) {
        rotFrame = frame ;  //update records...
        return prior.SLerp(after, alpha, result) ;
      }
      else {
        posFrame = frame ;  //update records...
        result.x = (after.x * alpha) + (prior.x * nA) ;
        result.y = (after.y * alpha) + (prior.y * nA) ;
        result.z = (after.z * alpha) + (prior.z * nA) ;
        return result ;
      }
    }
  }
}



