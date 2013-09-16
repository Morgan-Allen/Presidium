/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.ActorConstants ;
import src.game.base.* ;



/*
SPYCE_A = new Item.Type(C, COMMODITY, "Spyce A (Tinerazine)", 400),
SPYCE_B = new Item.Type(C, COMMODITY, "Spyce B (Halebdynum)", 400),
SPYCE_C = new Item.Type(C, COMMODITY, "Spyce C (Natrizoral)", 400),
//*/
  /*
    //TIMBER   = new Service(BC, FORM_COMMODITY, "Timber"  , 40 ),
    //STONES   = new Service(BC, FORM_COMMODITY, "Stones"  , 70 ),
    //HIDES    = new Service(BC, FORM_COMMODITY, "Hides"   , 150),
  OUTFITS  = new Item.Type(C, UNIQUE, "Outfits", 0),
  DEVICES  = new Item.Type(C, UNIQUE, "Devices", 0),
  ARMOURS  = new Item.Type(C, UNIQUE, "Armours", 0),
  
  GENE_SEED  = new Item.Type(C, UNIQUE, "Gene Sample", 50 ),
  GHOST_LINE = new Item.Type(C, UNIQUE, "Neural Scan", 500),
  TALISMAN   = new Item.Type(C, UNIQUE, "Talisman"   , 250)
  //*/



public interface BuildConstants extends ActorConstants {
  
  
  final public static int
    FORM_COMMODITY      = 0,
    FORM_PROVISION      = 1,
    FORM_UNIQUE         = 2,
    FORM_DEVICE         = 3,
    FORM_OUTFIT         = 4,
    FORM_SERVICE        = 5 ;
  
  final static Class BC = BuildConstants.class ;
  final static int FC = FORM_COMMODITY, FP = FORM_PROVISION ;
  
  final public static Service
    
    CARBS       = new Service(BC, "Carbs"     , "carbs.gif"      , FC, 10 ),
    PROTEIN     = new Service(BC, "Protein"   , "protein.gif"    , FC, 20 ),
    GREENS      = new Service(BC, "Greens"    , "greens.gif"     , FC, 50 ),
    SPICE       = new Service(BC, "Spice"     , "spices.gif"     , FC, 100),
    
    ORES        = new Service(BC, "Ores"      , "ores.gif"       , FC, 15 ),
    P_CARBONS   = new Service(BC, "P-Carbons" , "carbons.gif"    , FC, 35 ),
    FUEL_CORES  = new Service(BC, "Fuel Cores", "fuel rods.gif"  , FC, 60 ),
    
    PARTS       = new Service(BC, "Parts"     , "parts.gif"      , FC, 50 ),
    PLASTICS    = new Service(BC, "Plastics"  , "plastics.gif"   , FC, 75 ),
    PRESSFEED   = new Service(BC, "Pressfeed" , "pressfeed.gif"  , FC, 110),
    CIRCUITS    = new Service(BC, "Circuits"  , "inscription.gif", FC, 140),
    
    STIM_KITS   = new Service(BC, "Stim Kit"  , "stimkit.gif"    , FC, 40 ),
    SOMA        = new Service(BC, "Soma"      , "soma.gif"       , FC, 70 ),
    MEDICINE    = new Service(BC, "Medicine"  , "medicines.gif"  , FC, 200),
    
    ALL_FOOD_TYPES[] = { CARBS, PROTEIN, GREENS },
    ALL_CARRIED_ITEMS[] = Service.typesSoFar(),
    
    GENE_SEED   = new Service(BC, FORM_UNIQUE, "Gene Seed", 200 ),
    GHOSTLINE   = new Service(BC, FORM_UNIQUE, "Ghostline", 200 ),
    
    TROPHIES    = new Service(BC, FORM_UNIQUE, "Trophy"   , 400 ),
    DECOR       = new Service(BC, FORM_UNIQUE, "Decor"    , 400 ),
    RELICS      = new Service(BC, FORM_UNIQUE, "Relic"    , 400 ),
    
    ATOMICS     = new Service(BC, FORM_UNIQUE, "Atomic"   , 1000),
    
    ALL_UNIQUE_ITEMS[] = Service.typesSoFar(),
    
    WATER        = new Service(BC, "Water"       , "water.png"    , FP, 0),
    LIFE_SUPPORT = new Service(BC, "Life Support", "life_S.png"   , FP, 0),
    POWER        = new Service(BC, "Power"       , "power.png"    , FP, 0),
    DATALINKS    = new Service(BC, "Datalinks"   , "datalinks.png", FP, 0),
    
    ALL_PROVISIONS[] = Service.typesSoFar() ;
  
  
  final public static Service
    SERVICE_ADMIN   = new Service(BC, FORM_SERVICE, "Admin"      , 0),
    SERVICE_TREAT   = new Service(BC, FORM_SERVICE, "Treatment"  , 0),
    SERVICE_PERFORM = new Service(BC, FORM_SERVICE, "Performance", 0),
    SERVICE_DEPOT   = new Service(BC, FORM_SERVICE, "Depot"      , 0),
    
    VENUE_SERVICES[] = Service.typesSoFar(),
    
    SERVICE_ARMAMENT = new Service(BC, FORM_SERVICE, "Armament", 0),
    SERVICE_SHIPPING = new Service(BC, FORM_SERVICE, "Shipping", 0),
    SERVICE_CAPTIVES = new Service(BC, FORM_SERVICE, "Captives", 0),
    SERVICE_CONSORTS = new Service(BC, FORM_SERVICE, "Consorts", 0),
    
    PLANET_SERVICES[] = Service.typesSoFar() ;
  
  
  final public static int
    NONE     = 0,
    //
    //  These are properties of equipped weapons-
    MELEE    = 1,
    RANGED   = 2,
    ENERGY   = 4,
    PHYSICAL = 8,
    STUN     = 16,
    POISON   = 32,
    //
    //  These are properties of natural weapons or armour-
    GRAPPLE      = 64,
    CAUSTIC      = 128,
    TRANSMORPHIC = 256,
    ENERGY_DRAIN = 512 ;
  
  //
  //  TODO:  These prices are far too low next to the value of the raw
  //  materials.  Also, you need medical devices and so on.
  final public static DeviceType
    MANIPLES = new DeviceType(BC, "Maniples",
      2, GRAPPLE | MELEE | PHYSICAL, 10,
      new Conversion(3, PARTS, 5, ASSEMBLY),
      "maniples"
    ),
    MODUS_LUTE = new DeviceType(BC, "Modus Lute",
      0, RANGED | STUN, 40,
      new Conversion(1, PARTS, 10, ASSEMBLY),
      "modus lute"
    ),
    BICORDER = new DeviceType(BC, "Bicorder",
      0, MELEE, 55,
      new Conversion(2, PARTS, 15, ASSEMBLY),
      "bicorder"
    ),
    STUN_PISTOL = new DeviceType(BC, "Stun Pistol",
      10, RANGED | PHYSICAL | STUN, 35,
      new Conversion(1, PARTS, 10, ASSEMBLY),
      "pistol"
    ),
    PHASE_BLASTER = new DeviceType(BC, "Phase Blaster",
      15, RANGED | ENERGY, 25,
      new Conversion(1, PARTS, 10, ASSEMBLY),
      "pistol"
    ),
    CARVED_SPEAR = new DeviceType(BC, "Carved Spear",
      5, RANGED | MELEE | PHYSICAL, 5,
      new Conversion(5, ASSEMBLY),
      "spear"
    ),
    SHOCK_STAFF = new DeviceType(BC, "Shock Staff",
      15, MELEE | RANGED | PHYSICAL | STUN, 40,
      new Conversion(2, PARTS, 10, ASSEMBLY),
      "staff"
    ),
    ARC_SABRE = new DeviceType(BC, "Arc Sabre",
      25, MELEE | ENERGY, 100,
      new Conversion(3, PARTS, 15, ASSEMBLY),
      "sabre"
    ),
    KONOCHE = new DeviceType(BC, "Konoche",
      20, MELEE | PHYSICAL, 45,
      new Conversion(2, PARTS, 5, ASSEMBLY),
      "heavy blade"
    ),
    
    INTRINSIC_MELEE = new DeviceType(
      BC, "Intrinsic Melee", 0, MELEE | PHYSICAL, 0, null, null
    ),
    INTRINSIC_ENERGY_WEAPON = new DeviceType(
      BC, "Intrinsic Energy Weapon", 0, RANGED | ENERGY, 0, null, null
    ) ;
  final public static Service
    ALL_IMPLEMENTS[] = Service.typesSoFar() ;
  
  
  //
  //  TODO:  Should have skins associated with these?
  final public static OutfitType
    FINERY         = new OutfitType(
      BC, "Finery"        , 2 , 100,
      new Conversion(3, PLASTICS, Fabricator.class, 15, GRAPHIC_MEDIA)
    ),
    OVERALLS       = new OutfitType(
      BC, "Overalls"      , 2, 25,
      new Conversion(1, PLASTICS, Fabricator.class, 5, ASSEMBLY)
    ),
    CAMOUFLAGE     = new OutfitType(
      BC, "Camouflage"    , 3 , 35,
      new Conversion(2, PLASTICS, Fabricator.class, 10, GRAPHIC_MEDIA)
    ),
    SEALSUIT       = new OutfitType(
      BC, "Sealsuit"      , 4 , 75,
      new Conversion(2, PLASTICS, 1, PARTS, Fabricator.class, 15, ASSEMBLY)
    ),
    
    SHIELD_BELT = new OutfitType(
      BC, "Shield Belt"   , 5 , 25,
      new Conversion(2, PARTS, Foundry.class, 5, ASSEMBLY)
    ),
    PARTIAL_ARMOUR = new OutfitType(
      BC, "Partial Armour", 10, 50,
      new Conversion(3, PARTS, Foundry.class, 10, ASSEMBLY)
    ),
    BODY_ARMOUR    = new OutfitType(
      BC, "Body Armour"   , 15, 75,
      new Conversion(5, PARTS, Foundry.class, 15, ASSEMBLY)
    ),
    GOLEM_ARMOUR   = new OutfitType(
      BC, "Golem Armour"  , 20, 150,
      new Conversion(12, PARTS, Foundry.class, 20, ASSEMBLY)
    ),
    
    INTRINSIC_ARMOUR = new OutfitType(BC, "Intrinsic Armour", 0, 0, null) ;
  final public static Service
    ALL_OUTFITS[] = Service.typesSoFar() ;
  
  final public static Service
    ALL_ITEM_TYPES[] = Service.allTypes() ;
  
  
  
  
  final public static Object TO = new Object() ;
  
  final public static Conversion
    
    /*
    TIMBER_TO_CARBONS = new Conversion(
      1, TIMBER, TO, 2, CARBONS,
      CuringShed.class, SIMPLE_DC, CHEMISTRY
    ),
    
    STARCHES_TO_CARBONS = new Conversion(
      1, STARCHES, TO, 1, CARBONS,
      CuringShed.class, SIMPLE_DC, CHEMISTRY
    ),
    
    ISOTOPES_TO_POWER = new Conversion(
      1, ISOTOPES, TO, 100, POWER,
      Reactor.class, DIFFICULT_DC, CHEMISTRY, ROUTINE_DC, FIELD_THEORY
    ),
    //*/
    METALS_TO_PARTS = new Conversion(
      1, ORES, TO, 2, PARTS,
      Foundry.class, TRICKY_DC, ASSEMBLY, SIMPLE_DC, CHEMISTRY
    ),
    
    CARBONS_TO_PLASTICS = new Conversion(
      1, P_CARBONS, TO, 2, PLASTICS,
      Fabricator.class, TRICKY_DC, CHEMISTRY, SIMPLE_DC, GRAPHIC_MEDIA
    ),
    
    PLASTICS_TO_PRESSFEED = new Conversion(
      1, PLASTICS, TO, 5, PRESSFEED,
      AuditOffice.class, SIMPLE_DC, ADMINISTRATION, DIFFICULT_DC, GRAPHIC_MEDIA
    ),
    
    
    NIL_TO_SOMA = new Conversion(
      TO, 1, SOMA,
      CultureVats.class, ROUTINE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    ),
    
    NIL_TO_STARCHES = new Conversion(
      TO, 1, CARBS,
      CultureVats.class, SIMPLE_DC, CHEMISTRY
    ) ;
}















