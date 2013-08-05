


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.social.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.user.* ;
import src.util.* ;

//
//  The delay after completion is too long.  Also, actors don't seem to be
//  switching over to the mission fast enough.



public abstract class Mission implements
  Behaviour, Session.Saveable, Selectable
{
  
	
  final static int
    TYPE_VOLUNTEER     = 0,
    TYPE_BOUNTY_SMALL  = 1,
    TYPE_BOUNTY_MEDIUM = 2,
    TYPE_BOUNTY_LARGE  = 3,
    ALL_TYPES[] = { 0, 1, 2, 3 } ;
  final static String TYPE_DESC[] = {
    "Volunteer", "Small Payment", "Medium Payment", "Large Payment"
  } ;
  final static int REWARD_AMOUNTS[] = {
    0, 25, 100, 400
  } ;
  
  
  Base base ;
  Target subject ;
  int rewardType = TYPE_VOLUNTEER ;
  List <Role> roles = new List <Role> () ;
  
  float priority ;
  boolean begun ;
  
  ImageSprite flagSprite ;
  Texture flagTex ;
  String description ;
  
  
  
  protected Mission(
    Base base, Target subject,
    ImageSprite flagSprite, String description
  ) {
    this.base = base ;
    this.subject = subject ;
    this.flagSprite = flagSprite ;
    this.flagTex = ((ImageModel) flagSprite.model()).texture() ;
    this.description = description ;
  }
  
  
  public Mission(Session s) throws Exception {
    s.cacheInstance(this) ;
    base = (Base) s.loadObject() ;
    subject = s.loadTarget() ;
    rewardType = s.loadInt() ;
    priority = s.loadFloat() ;
    begun = s.loadBool() ;
    
    for (int i = s.loadInt() ; i-- > 0 ;) {
      final Role role = new Role() ;
      role.applicant = (Actor) s.loadObject() ;
      role.approved = s.loadBool() ;
      roles.add(role) ;
    }
    
    flagSprite = (ImageSprite) Model.loadSprite(s.input()) ;
    flagTex = ((ImageModel) flagSprite.model()).texture() ;
    description = s.loadString() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(base) ;
    s.saveTarget(subject) ;
    s.saveInt(rewardType) ;
    s.saveFloat(priority) ;
    s.saveBool(begun) ;
    
    s.saveInt(roles.size()) ;
    for (Role role : roles) {
      s.saveObject(role.applicant) ;
      s.saveBool(role.approved) ;
    }
    
    Model.saveSprite(flagSprite, s.output()) ;
    s.saveString(description) ;
  }
  
  
  public Target subject() {
    return subject ;
  }
  
  
  public int rewardAmount() {
    return REWARD_AMOUNTS[rewardType] ;
  }
  
  
  
  /**  Adding and screening applicants-
    */
  class Role {
    Actor applicant ;
    ///Pledge pledgeMade ;  //Not used at the moment.
    boolean approved ;
  }
  
  
  public boolean begun() {
    return begun ;
  }
  
  
  public boolean active() {
    return begun && ! complete() ;
  }
  
  
  public boolean open() {
    return (! begun) ;
  }
  
  
  public int numApproved() {
    int count = 0 ;
    for (Role role : roles) if (role.approved) count++ ;
    return count ;
  }
  
  
  public int numApplied() {
    return roles.size() ;
  }
  
  
  public void setApplicant(Actor actor, boolean is) {
    if (is) {
      for (Role role : roles) if (role.applicant == actor) return ;
      Role role = new Role() ;
      role.applicant = actor ;
      role.approved = false ;
      roles.add(role) ;
    }
    else {
      for (Role role : roles) if (role.applicant == actor) {
        roles.remove(role) ;
      }
    }
  }
  
  
  protected void beginMission() {
    for (Role role : roles) {
      if (! role.approved) roles.remove(role) ;
      else {
        role.applicant.psyche.assignMission(this) ;
        role.applicant.psyche.assignBehaviour(this) ;
      }
    }
    begun = true ;
  }
  
  
  protected void endMission(boolean cancelled) {
    int reward = REWARD_AMOUNTS[rewardType] ;
    for (Role role : roles) {
      role.applicant.psyche.assignMission(null) ;
      if (! cancelled) role.applicant.gear.incCredits(reward) ;
    }
    base.removeMission(this) ;
  }
  
  
  
  /**  Default behaviour implementation and utility methods-
    */
  public boolean monitor(Actor actor) { return false ; }
  public void abortStep() {}
  
  public void setPriority(float priority) { this.priority = priority ; }
  public float priorityFor(Actor actor) { return priority ; }
  
  
  public void updateMission() {
    if (complete()) endMission(false) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() { return description ; }
  public String helpInfo() { return description ; }
  public String toString() { return description ; }
  
  
  public Composite portrait(BaseUI UI) {
    final Composite c = new Composite(UI, flagTex) ;
    return c ;
  }
  
  public String[] infoCategories() { return null ; }
  
  
  
  public void writeInformation(
    Description d, int categoryID, final BaseUI UI
  ) {
    //
    //  You need the ability to set overall urgency, see volunteers, and screen
    //  those who seem suitable.
    //
    //  Here, you can approve the mission, cancel the mission, or visit your
    //  personnel listings (full household.)
    if (! begun) {
      d.append("CHOOSE PAYMENT:") ;
      for (final int type : ALL_TYPES) {
        d.append("\n  ") ;
        d.append(new Description.Link(TYPE_DESC[type]) {
          public void whenClicked() {
            rewardType = type ;
          }
        }, type == rewardType ? Colour.GREEN : Colour.BLUE) ;
      }
      d.append("\n") ;
      d.append("\n("+REWARD_AMOUNTS[rewardType]+" CREDITS OFFERED)") ;
      
    }
    else {
      d.append("\n("+REWARD_AMOUNTS[rewardType]+" CREDITS OFFERED)") ;
    }
    
    
    if (begun) d.append("\n\nTEAM MEMBERS:") ;
    else d.append("\n\nAPPLICANTS:") ;
    //
    //  First, list the team members that have been approved-
    for (final Role role : roles) {
      if (! role.approved) continue ;
      d.append("\n  ") ;
      d.append(role.applicant) ;
      d.append(" ") ;
      if (! begun) d.append(new Description.Link("(DISMISS)") {
        public void whenClicked() {
          role.approved = false ;
        }
      }) ;
    }
    //
    //  Then, the applicants awaiting a decision-
    for (final Role role : roles) {
      if (role.approved) continue ;
      d.append("\n  ") ;
      d.append(role.applicant) ;
      d.append(" ") ;
      d.append(new Description.Link("(SELECT)") {
        public void whenClicked() {
          role.approved = true ;
        }
      }) ;
    }
    if (numApplied() == 0) d.append("\n  (None)") ;
    
    d.append("\n\n") ;
    if (! begun && numApproved() > 0) {
      d.append(new Description.Link("(APPROVE) ") {
        public void whenClicked() {
          beginMission() ;
        }
      }) ;
    }
    else d.append("(APPROVE) ", Colour.GREY) ;
    d.append(new Description.Link("(ABORT)") {
      public void whenClicked() {
        if (begun) endMission(true) ;
        else endMission(false) ;
        UI.selection.setSelected(null) ;
      }
    }, Colour.RED) ;
  }
  

  public void whenClicked() {
    ((BaseUI) PlayLoop.currentUI()).selection.setSelected(this) ;
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    //  Have a dedicated MissionPanel?
    return new InfoPanel(UI, this, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }
  
  
  public Texture flagTex() {
    return flagTex ;
  }
  
  
  public Sprite flagSprite() {
    placeFlag(flagSprite, subject) ;
    return flagSprite ;
  }
  
  
  public static void placeFlag(Sprite flag, Target subject) {
    if (subject instanceof Element) {
      final Element e = (Element) subject ;
      flag.position.setTo(e.viewPosition(null)) ;
      flag.position.z += e.height() + 1 ;
      flag.scale = 0.5f ;
    }
    else {
      flag.position.setTo(subject.position(null)) ;
      flag.position.z += 1.5f ;
      flag.scale = 0.5f ;
    }
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    final Vec3D pos = (subject instanceof Mobile) ?
      ((Mobile) subject).viewPosition(null) :
      subject.position(null) ;
    Selection.renderPlane(
      rendering, pos, subject.radius() + 0.5f,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    ) ;
  }
}






/**  Targeting/position methods.  (Some are essentially unused.)
public boolean inWorld() { return subject.inWorld() ; }
public Vec3D position(Vec3D v) { return subject.position(v) ; }

public float height() { return 1 ; }
public float radius() { return 0 ; }

public void flagWith(Object f) {}
public Object flaggedWith() { return null ; }
//*/


