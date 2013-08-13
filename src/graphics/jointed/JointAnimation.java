


package src.graphics.jointed ;
import src.graphics.common.* ;
import src.util.* ;



public class JointAnimation {
  
  
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
  
  
  JointAnimation(JointSprite sprite, Model.AnimRange range) {
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