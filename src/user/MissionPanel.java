


package src.user ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.social.* ;
import src.util.* ;





public class MissionPanel extends InfoPanel {
  
  
  final static int
    TYPE_ROUTINE   = 0,
    TYPE_URGENT    = 1,
    TYPE_CRITICAL  = 2,
    //TYPE_VOLUNTEER = 3,
    //TYPE_COVERT    = 4,
    //TYPE_BOUNTY    = 5,
    NUM_TYPES = 3 ;
  final static String TYPE_DESC[] = {
    "Routine", "Urgent", "Critical",
    //"Volunteer", "Covert", "Bounty"
  } ;
  
  int missionType = TYPE_ROUTINE ;
  
  
  class Role {
    Actor applicant ;
    //int roleType ;
    Pledge pledgeMade ;
    boolean approved ;
  }
  
  List <Role> roles = new List <Role> () ;
  
  
  
  
  public MissionPanel(BaseUI UI, Selectable selected) {
    super(UI, selected, 0) ;
  }
  
  

  protected void updateText() {
    //
    //  You need the ability to set overall urgency, see volunteers, and screen
    //  those who seem suitable.  
    detailText.append("Type: "+TYPE_DESC[missionType]) ;
    //
    //  Here, you can approve the mission, cancel the mission, or visit your
    //  personnel listings (full household.)
    
    
    
    for (Role role : roles) {
      detailText.append("\n\n  ") ;
      detailText.append(role.applicant) ;
      if (role.pledgeMade != null) {
        String response = Wording.response(role.applicant, role.pledgeMade) ;
        detailText.append("\n  "+response) ;
        detailText.append("\n  ") ;
        detailText.append(role.pledgeMade) ;
      }
      //
      //  You can approve, reject, or negotiate over the offer.
    }
  }
}

















