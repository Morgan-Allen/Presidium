


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.social.* ;
import src.game.campaign.* ;
import src.graphics.common.* ;
import src.graphics.sfx.* ;
import src.util.* ;
import src.user.* ;



//
//  TODO:  The save/load methods need to schedule saving/loading, rather than
//  performing it instantly?

//
//  TODO:  Grant multiple options for saving/loading, so that you can choose
//  which scenario to load, or quit after saving (which restores more Psi, but
//  obliges time to pass.)


//
//  TODO:  These need to actually test the psyonic skills in question, and
//  (at least for testing purposes) work without a caster.
//
//  TODO:  Make this available to actors, once the feedback mechanism is in
//  place(?)  (This will require individual instancing.)



public class Power implements Abilities {
  
  
  final static String
    IMG_DIR = "media/GUI/Powers/",
    SFX_DIR = "media/SFX/" ;
  final static float FPS = PlayLoop.FRAMES_PER_SECOND ;
  
  final public static int
    NONE        = 0,
    PLAYER_ONLY = 1,
    MAINTAINED  = 2 ;
  
  
  final public String name, helpInfo ;
  final public Texture buttonTex ;
  final int properties ;
  
  
  Power(String name, int properties, String imgFile, String helpInfo) {
    this.name = name ;
    this.helpInfo = helpInfo ;
    this.buttonTex = Texture.loadTexture(IMG_DIR+imgFile) ;
    this.properties = properties ;
  }
  
  
  public boolean finishedWith(
    Actor caster, String option,
    Target selected, boolean clicked
  ) {
    return false ;
  }
  
  
  public String[] options() {
    return null ;
  }
  
  
  final public static PlaneFX.Model
    
    REMOTE_VIEWING_FX_MODEL = new PlaneFX.Model(
      "RV_swirl_fx", Power.class,
      SFX_DIR+"remote_viewing.png", 1, -360 / FPS, 2 / FPS, false
    ),
    
    LIGHT_BURST_MODEL = new PlaneFX.Model(
      "RV_burst_fx", Power.class,
      SFX_DIR+"light_burst.png", 1, 0, 2 / FPS, true
    ),
    
    KINESTHESIA_FX_MODEL = new PlaneFX.Model(
      "kinesthesia_fx", Power.class,
      SFX_DIR+"kinesthesia.png", 0.5f, 360 / FPS, 0, true
    ),
    
    SUSPENSION_FX_MODEL = new PlaneFX.Model(
      "suspension_fx", Power.class,
      SFX_DIR+"suspension.png", 0.5f, 360 / FPS, 0, true
    ),
    
    TELEKINESIS_FX_MODEL = new PlaneFX.Model(
      "telekinesis_fx", Power.class,
      SFX_DIR+"telekinesis.png", 0.5f, 360 / FPS, 0, true
    ),
    
    VOICE_OF_COMMAND_FX_MODEL = new PlaneFX.Model(
      "voice_command_fx", Power.class,
      SFX_DIR+"voice_of_command.png", 1, 360 / FPS, 1, true
    ) ;
  
  
  public static void applyTimeDilation(float gameSpeed, Scenario scenario) {
    final Actor caster = scenario.base.ruler() ;
    if (caster == null || GameSettings.psyFree) return ;
    final float bonus = caster.traits.useLevel(PROJECTION) / 10f ;
    float drain = -1 / (1 + bonus) ;
    
    caster.health.adjustPsy(drain) ;
  }
  
  
  public static void applyResting(float gameSpeed, Scenario scenario) {
    final Actor caster = scenario.base.ruler() ;
    if (caster == null || GameSettings.psyFree) {
      PlayLoop.setGameSpeed(1) ;
      return ;
    }
    //
    //  Restore psi points/fatigue/etc. and return to normal speed when done.
    caster.health.adjustPsy(2) ;
    PlayLoop.setNoInput(true) ;
    if (caster.health.psyPoints() >= caster.health.maxPsy()) {
      PlayLoop.setGameSpeed(1) ;
      PlayLoop.setNoInput(false) ;
    }
  }
  
  
  public static void applyWalkPath(Scenario scenario) {
    final Actor caster = scenario.base.ruler() ;
    if (caster == null || GameSettings.psyFree) return ;
    final float bonus = caster.traits.useLevel(PREMONITION) / 10 ;
    float lastSave = PlayLoop.timeSinceLastSave() ;
    caster.health.adjustPsy((lastSave / 1000f) * (0.5f + bonus)) ;
  }
  
  
  public static void applyDenyVision(Scenario scenario) {
    final Actor caster = scenario.base.ruler() ;
    if (caster == null || GameSettings.psyFree) return ;
    final float bonus = caster.traits.useLevel(PREMONITION) / 10 ;
    caster.health.adjustPsy(-10f / (0.5f + bonus)) ;
  }
  
  
  final public static Power
    
    WALK_THE_PATH = new Power(
      "Walk The Path", PLAYER_ONLY, "power_remembrance.gif",
      "Accept your vision of events and allow them to be fulfilled.\n(Saves "+
      "current game.)"
    ) {
      final String
        OPTION_QUIT = "Save and Exit",
        OPTION_REST = "Save and Rest",
        OPTION_MARK = "Save and Remember" ;
    
      public String[] options() {
        return new String[] {
          OPTION_QUIT, OPTION_REST, OPTION_MARK
        } ;
      }
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (option.equals(OPTION_QUIT)) {
          PlayLoop.currentScenario().saveProgress(true) ;
          PlayLoop.exitLoop() ;
        }
        if (option.equals(OPTION_REST)) {
          PlayLoop.currentScenario().saveProgress(true) ;
          PlayLoop.setGameSpeed(5.0f) ;
        }
        if (option.equals(OPTION_MARK)) {
          PlayLoop.currentScenario().saveProgress(false) ;
        }
        return true ;
      }
    },
    
    DENY_THE_VISION = new Power(
      "Deny The Vision", PLAYER_ONLY, "power_foresight.gif",
      "Aborts your precognitive vision and lets you choose a different path."+
      "\n(Loads a previous game.)"
    ) {
      public String[] options() {
        return PlayLoop.currentScenario().loadOptions() ;
      }
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        //
        //  Clean up arguments to allow garbage collection, and delete later
        //  saves in the timeline-
        caster = null ;
        selected = null ;
        PlayLoop.currentScenario().wipeSavesAfter(option) ;
        //
        //  Load the older save, and make it current-
        final String prefix = PlayLoop.currentScenario().savesPrefix() ;
        PlayLoop.loadGame(Scenario.fullSavePath(prefix, option)) ;
        PlayLoop.currentScenario().saveProgress(true) ;
        return true ;
      }
    },
    
    TIME_DILATION = new Power(
      "Time Dilation", PLAYER_ONLY | MAINTAINED, "power_time_dilation.gif",
      "Insulates your experience from temporal passage.\n(Reduces game speed.)"
    ) {
      //
      //  TODO:  Provide options for the rate of slowing here?
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        final boolean slowed = PlayLoop.gameSpeed() < 1 ;
        if (slowed) {
          PlayLoop.setGameSpeed(1) ;
          return true ;
        }
        
        if (caster == null || GameSettings.psyFree) {
          PlayLoop.setGameSpeed(0.5f) ;
          return true ;
        }
        
        final float bonus = caster.traits.useLevel(PROJECTION) / 2 ;
        PlayLoop.setGameSpeed(1f / (float) (1f + Math.sqrt(bonus))) ;
        caster.health.adjustPsy(-1) ;
        
        return true ;
      }
    },
    
    REMOTE_VIEWING = new Power(
      "Remote Viewing", PLAYER_ONLY, "power_remote_viewing.gif",
      "Grants you an extra-sensory perception of distant places or persons."+
      "\n(Lifts fog around target terrain.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Tile)) return false ;
        final Tile tile = (Tile) selected ;
        
        float bonus = 0 ;
        if (caster != null && ! GameSettings.psyFree) {
          bonus += caster.traits.useLevel(PROJECTION) / 5 ;
          
          float dist = (float) Math.sqrt(Spacing.distance(tile, caster)) ;
          caster.health.adjustPsy(
            -10 * (1 + (dist / World.SECTOR_SIZE))
          ) ;
        }
        
        final float radius = 9 + (bonus * bonus) ;
        final Base played = PlayLoop.currentScenario().base ;
        played.intelMap.liftFogAround(tile.x, tile.y, radius) ;
        
        final float SS = radius / 1.5f ;
        final Vec3D p = tile.position(null) ;
        p.z += 0.5f ;
        
        final Sprite swirlFX = REMOTE_VIEWING_FX_MODEL.makeSprite() ;
        swirlFX.scale = SS / 2 ;
        swirlFX.position.setTo(p) ;
        tile.world.ephemera.addGhost(null, SS, swirlFX, 1.0f) ;
        
        final Sprite burstFX = LIGHT_BURST_MODEL.makeSprite() ;
        burstFX.scale = SS / 2 ;
        burstFX.position.setTo(p) ;
        tile.world.ephemera.addGhost(null, SS, burstFX, 2.0f) ;
        
        return true ;
      }
    },
    
    TELEKINESIS = new Power(
      "Telekinesis", NONE, "power_telekinesis.gif",
      "Imparts spatial moment to a chosen material object.\n(Hurls or carries "+
      "the target in an indicated direction.)"
    ) {
      private Mobile grabbed = null ;
      
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (grabbed == null) {
          if (clicked && selected instanceof Mobile) {
            grabbed = (Mobile) selected ;
          }
          return false ;
        }
        else if (clicked) {
          grabbed = null ;
          return true ;
        }
        else return ! pushGrabbed(caster, selected) ;
      }
      
      
      private boolean pushGrabbed(Actor caster, Target toward) {
        if (grabbed == null) return false ;
        
        if (grabbed instanceof Actor) {
          ((Actor) grabbed).enterStateKO(Action.FALL) ;
        }
        
        float maxDist = 1 ;
        if (caster != null && ! GameSettings.psyFree) {
          maxDist = 1 + (caster.traits.useLevel(TRANSDUCTION) / 10f) ;
          caster.health.adjustPsy(-4f / PlayLoop.FRAMES_PER_SECOND) ;
        }
        maxDist *= 10f / PlayLoop.FRAMES_PER_SECOND ;
        maxDist /= 4 * grabbed.radius() ;
        
        final Vec3D pushed = new Vec3D() ;
        toward.position(pushed).sub(grabbed.position(null)) ;
        if (pushed.length() > maxDist) pushed.normalise().scale(maxDist) ;
        pushed.add(grabbed.position(null)) ;
        
        grabbed.setHeading(pushed, -1, false, grabbed.world()) ;
        grabbed.world().ephemera.updateGhost(
          grabbed, 1, TELEKINESIS_FX_MODEL, 0.2f
        ) ;
        return true ;
      }
    },
    
    FORCEFIELD = new Power(
      "Forcefield", MAINTAINED, "power_forcefield.gif",
      "Encloses the subject in a selectively permeable suspension barrier.\n"+
      "(Temporarily raises shields on target.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Actor)) return false ;
        final Actor subject = (Actor) selected ;
        
        if (caster == null || GameSettings.psyFree) {
          subject.gear.boostShields(5) ;
          return true ;
        }
        
        final float bonus = caster.traits.useLevel(TRANSDUCTION) / 2 ;
        subject.gear.boostShields(5 + bonus) ;
        caster.health.adjustPsy(-1.5f) ;
        return true ;
      }
    },
    
    SUSPENSION = new Power(
      "Suspension", MAINTAINED, "power_suspension.gif",
      "Suspends metabolic function in subject.\n(Can be used to arrest "+
      "injury or incapacitate foes.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Actor)) return false ;
        final Actor subject = (Actor) selected ;
        
        float bonus = 1 ;
        if (caster != null && ! GameSettings.psyFree) {
          caster.health.adjustPsy(-5) ;
          bonus += caster.traits.useLevel(METABOLISM) / 2 ;
        }
        
        if (subject.health.conscious()) {
          if (subject.traits.test(VIGOUR, 10 + bonus, 10)) bonus = 0 ;
        }
        if (bonus > 0) {
          subject.health.setState(ActorHealth.STATE_SUSPEND) ;
        }
        else bonus = 0.1f ;
        subject.traits.incLevel(SUSPENSION_EFFECT, bonus * 2 / 10f) ;
        return true ;
      }
    },
    
    KINESTHESIA = new Power(
      "Kinesthesia", MAINTAINED, "power_kinesthesia.gif",
      "Augments hand-eye coordination and reflex response.\n(Boosts most "+
      "combat and acrobatic skills.)"
    ) {
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Actor)) return false ;
        final Actor subject = (Actor) selected ;
        float bonus = 5f ;
        if (caster != null && ! GameSettings.psyFree) {
          bonus += caster.traits.useLevel(SYNESTHESIA) / 2 ;
          caster.health.adjustPsy(-2) ;
        }
        subject.traits.incLevel(KINESTHESIA_EFFECT, bonus * 2 / 10f) ;
        return true ;
      }
    },
    
    VOICE_OF_COMMAND = new Power(
      "Voice of Command", PLAYER_ONLY, "power_voice_of_command.gif",
      "Employs mnemonic triggering to incite specific behavioural response."+
      "\n(Compels subject to fight, flee, help or speak.)"
    ) {
      
      final String options[] = new String[] {
        "Flee",
        "Fight",
        "Treat",
        "Talk"
      } ;
      
      Actor affects = null ;
      
      public String[] options() { return options ; }
      
      
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (affects == null) {
          if (clicked && selected instanceof Actor) {
            affects = (Actor) selected ;
          }
        }
        else {
          if (clicked) return applyEffect(caster, option, selected) ;
        }
        return false ;
      }
      
      
      private boolean applyEffect(
        Actor caster, String option, Target selected
      ) {
        Plan command = null ;
        if (option == options[0] && selected instanceof Tile) {
          command = new Retreat(affects, (Tile) selected) ;
        }
        if (option == options[1] && selected instanceof Actor) {
          command = new Combat(affects, (Actor) selected) ;
        }
        if (option == options[2] && selected instanceof Actor) {
          command = new Treatment(affects, (Actor) selected, null) ;
        }
        if (option == options[3] && selected instanceof Actor) {
          command = new Dialogue(affects, (Actor) selected, null) ;
        }
        if (command == null) return false ;
        
        float priorityMod = Plan.PARAMOUNT ;
        if (caster != null && ! GameSettings.psyFree) {
          priorityMod += caster.traits.useLevel(SUGGESTION) / 5f ;
          caster.health.adjustPsy(-5) ;
        }
        command.priorityMod = priorityMod ;
        
        if (affects.mind.couldSwitchTo(command)) {
          affects.mind.assignBehaviour(command) ;
        }
        else {
          I.say("Compulsion refused! "+command) ;
          I.say("Priority was: "+command.priorityFor(affects)) ;
        }
        
        affects.world().ephemera.addGhost(
          affects, 1, VOICE_OF_COMMAND_FX_MODEL.makeSprite(), 0.5f
        ) ;
        final Sprite selectFX = VOICE_OF_COMMAND_FX_MODEL.makeSprite() ;
        selectFX.scale = selected.radius() ;
        selected.position(selectFX.position) ;
        affects.world().ephemera.addGhost(
          null, 1, selectFX, 0.5f
        ) ;

        affects = null ;
        return true ;
      }
    },
    
    
    BASIC_POWERS[] = {
        WALK_THE_PATH, DENY_THE_VISION,
        TIME_DILATION, REMOTE_VIEWING,
        TELEKINESIS, FORCEFIELD,
        SUSPENSION,
        KINESTHESIA,
        VOICE_OF_COMMAND
    }
  ;
}





