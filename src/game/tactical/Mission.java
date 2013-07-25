


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.social.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.user.* ;
import src.util.* ;



public abstract class Mission implements
  Behaviour, Session.Saveable, Selectable
{
  
	
  final static int
    TYPE_NOT_READY     = 0,
    TYPE_COVERT        = 1,
    TYPE_VOLUNTEER     = 2,
    TYPE_BOUNTY_SMALL  = 3,
    TYPE_BOUNTY_MEDIUM = 4,
    TYPE_BOUNTY_LARGE  = 5,
    ALL_TYPES[] = { 0, 1, 2, 3, 4, 5 } ;
  final static String TYPE_DESC[] = {
    "Not Ready", "Covert", "Volunteer",
    "Small Payment", "Medium Payment", "Large Payment"
  } ;
  final static int REWARD_AMOUNTS[] = {
    0, 0, 0, 20, 100, 500
  } ;
  
  
  class Role {
    Actor applicant ;
    Pledge pledgeMade ;  //Not used at the moment.
    boolean approved ;
  }
  
  
  Target subject ;
  int rewardType = TYPE_NOT_READY ;
  List <Role> roles = new List <Role> () ;
  
  float priority ;
  boolean begun ;
  
  ImageSprite flagSprite ;
  Texture flagTex ;
  String description ;
  
  
  
  protected Mission(
    Target subject,
    ImageSprite flagSprite, String description
  ) {
    this.subject = subject ;
    this.flagSprite = flagSprite ;
    this.flagTex = ((ImageModel) flagSprite.model()).texture() ;
    this.description = description ;
  }
  
  
  public Mission(Session s) throws Exception {
    s.cacheInstance(this) ;
  }
  
  
  public void saveState(Session s) throws Exception {
  }
  
  
  public ImageSprite flagSprite() {
    if (subject instanceof Element) {
      final Element e = (Element) subject ;
      flagSprite.position.setTo(e.viewPosition(null)) ;
      flagSprite.position.z += e.height() + 1 ;
      flagSprite.scale = 0.5f ;
    }
    return flagSprite ;
  }
  
  
  
  /**  Adding and screening applicants-
    */
  public boolean begun() {
    return begun ;
  }
  
  
  public boolean covert() {
    return rewardType == TYPE_COVERT ;
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
      role.applicant.psyche.assignMission(this) ;
    }
    begun = true ;
  }
  
  
  protected void endMission(boolean cancelled) {
    int reward = 0 ;
    if (rewardType == TYPE_BOUNTY_SMALL ) reward = 20  ;
    if (rewardType == TYPE_BOUNTY_MEDIUM) reward = 100 ;
    if (rewardType == TYPE_BOUNTY_LARGE ) reward = 500 ;
    for (Role role : roles) {
      role.applicant.psyche.assignMission(null) ;
      if (! cancelled) role.applicant.gear.incCredits(reward) ;
    }
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
  
  
  
  /**  Targeting/position methods.  (Some are essentially unused.)
    */
  public boolean inWorld() { return subject.inWorld() ; }
  public Vec3D position(Vec3D v) { return subject.position(v) ; }
  
  public float height() { return 1 ; }
  public float radius() { return 0 ; }
  
  public void flagWith(Object f) {}
  public Object flaggedWith() { return null ; }
  
  
  
  /**  Rendering and interface methods-
    */
  
  public String fullName() { return description ; }
  public String helpInfo() { return description ; }
  public String[] infoCategories() { return null ; }
  
  
  public Composite portrait(BaseUI UI) {
    final Composite c = new Composite(UI, flagTex) ;
    return c ;
  }
  
  
  public void writeInformation(Description d, int categoryID, BaseUI UI) {
    //
    //  You need the ability to set overall urgency, see volunteers, and screen
    //  those who seem suitable.
    d.append("CHOOSE TYPE:") ;
    for (final int type : ALL_TYPES) {
      d.append("\n  ") ;
      d.append(new Description.Link(TYPE_DESC[type]) {
        public void whenClicked() {
          rewardType = type ;
        }
      }, type == rewardType ? Colour.GREEN : Colour.BLUE) ;
    }
    if (rewardType >= TYPE_BOUNTY_SMALL) {
      d.append("\n("+REWARD_AMOUNTS[rewardType]+" credits)\n") ;
    }
    if (rewardType == TYPE_COVERT) {
      d.append("\nCitizens will only join covert missions if you summon ") ;
      d.append("them to your presence and negotiate their services.\n") ;
    }
    if (rewardType == TYPE_VOLUNTEER) {
      d.append("Use volunteers if you want to guarantee loyalty.") ;
    }
    if (rewardType == TYPE_NOT_READY) {
      d.append("Select a reward type to entice applicants.") ;
    }
    //
    //  Here, you can approve the mission, cancel the mission, or visit your
    //  personnel listings (full household.)
    if (! begun && numApproved() > 0) {
      d.append(new Description.Link("[APPROVE TEAM]") {
        public void whenClicked() {
          beginMission() ;
        }
      }) ;
    }
    else d.append("[TEAM APPROVED]", Colour.GREEN) ;
    d.append(new Description.Link("[ABORT MISSION]") {
      public void whenClicked() {
        if (begun) endMission(true) ;
        else endMission(false) ;
      }
    }, Colour.RED) ;
    //
    //  First, list the team members that have been approved-
    d.append("\nTEAM MEMBERS:") ;
    for (Role role : roles) {
      if (! role.approved) continue ;
      d.append("\n  ") ;
      d.append(role.applicant) ;
      //  Allow them to be rejected too.
    }
    //
    //  Then, the applicants awaiting a decision-
    for (Role role : roles) {
      if (role.approved) continue ;
    }
  }
  

  public void whenClicked() {
    ((BaseUI) PlayLoop.currentUI()).selection.setSelected(this) ;
  }

  
  public InfoPanel createPanel(BaseUI UI) {
    //  TODO:  Have a dedicated MissionPanel.
    return new InfoPanel(UI, this, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }
}








