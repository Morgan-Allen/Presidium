/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.SkillsAndTraits ;
import src.game.base.* ;



/*
SPYCE_A = new Item.Type(C, COMMODITY, "Spyce A (Tinerazine)", 400),
SPYCE_B = new Item.Type(C, COMMODITY, "Spyce B (Halebdynum)", 400),
SPYCE_C = new Item.Type(C, COMMODITY, "Spyce C (Natrizoral)", 400),
TALISMAN   = new Item.Type(C, UNIQUE, "Talisman"   , 250)
//*/

//
//  TODO:  What about Tensile materials?  Antimass?


public interface Economy extends SkillsAndTraits {
  
  
  final public static int
    FORM_COMMODITY      = 0, FC = 0,
    FORM_PROVISION      = 1, FP = 1,
    FORM_UNIQUE         = 2, FU = 2,
    FORM_DEVICE         = 3, FD = 3,
    FORM_OUTFIT         = 4, FO = 4,
    FORM_SERVICE        = 5, FS = 5 ;
  
  final static Class BC = Economy.class ;
  
  final public static Service
    //
    //  Food types-
    CARBS       = new Service(BC, "Carbs"     , "carbs.gif"      , FC, 10 ),
    PROTEIN     = new Service(BC, "Protein"   , "protein.gif"    , FC, 20 ),
    GREENS      = new Service(BC, "Greens"    , "greens.gif"     , FC, 50 ),
    SOMA        = new Service(BC, "Soma"      , "soma.gif"       , FC, 70 ),
    //
    //  Mineral wealth-
    METAL_ORE   = new Service(BC, "Metal Ore" , "ores.gif"       , FC, 15 ),
    PETROCARBS  = new Service(BC, "Petrocarbs", "carbons.gif"    , FC, 35 ),
    FUEL_CORES  = new Service(BC, "Fuel Cores", "fuel rods.gif"  , FC, 75 ),
    //
    //  Building materials-
    PARTS       = new Service(BC, "Parts"     , "parts.gif"      , FC, 50 ),
    PLASTICS    = new Service(BC, "Plastics"  , "plastics.gif"   , FC, 75 ),
    CIRCUITRY   = new Service(BC, "Circuitry" , "inscription.gif", FC, 140),
    DECOR       = new Service(BC, "Decor"     , "decor.gif"      , FC, 300),
    //
    //  Medical supplies-
    STIM_KITS   = new Service(BC, "Stim Kit"  , "stimkit.gif"    , FC, 40 ),
    SPICE       = new Service(BC, "Spice"     , "spices.gif"     , FC, 100),
    MEDICINE    = new Service(BC, "Medicine"  , "medicines.gif"  , FC, 200),
    
    ALL_FOOD_TYPES[] = { CARBS, PROTEIN, GREENS, SPICE },
    ALL_COMMODITIES[] = Service.typesSoFar() ;
  
  final public static Service
    SAMPLES     = new Service(BC, "Samples", "crates_big.gif", FU, -1),
    CREDITS     = new Service(BC, "Credits", null            , FU, -1),
    
    TROPHIES    = new Service(BC, FORM_UNIQUE, "Trophy"     , 100 ),
    RARITIES    = new Service(BC, FORM_UNIQUE, "Rarity"     , 100 ),
    
    GENE_SEED   = new Service(BC, "Gene Seed" , "gene_seed.gif" , FU, 200 ),
    REPLICANTS  = new Service(BC, "Replicants", "replicant.gif" , FU, 200 ),
    //GHOSTLINE   = new Service(BC, FORM_UNIQUE, "Ghostline"  , 200 ),
    PRESSFEED   = new Service(BC, "Pressfeed", "pressfeed.gif", FU, 200 ),
    
    ATOMICS     = new Service(BC, FORM_UNIQUE, "Atomic"   , 1000),
    
    ALL_UNIQUE_ITEMS[] = Service.typesSoFar() ;
  
  final public static Service
    WATER        = new Service(BC, "Water"       , "water.png"    , FP, 10),
    LIFE_SUPPORT = new Service(BC, "Life Support", "life_S.png"   , FP, 10),
    POWER        = new Service(BC, "Power"       , "power.png"    , FP, 10),
    DATALINKS    = new Service(BC, "Datalinks"   , "datalinks.png", FP, 10),
    
    ALL_PROVISIONS[] = Service.typesSoFar() ;
  
  final public static Service
    SERVICE_ADMIN    = new Service(BC, FORM_SERVICE, "Admin"      , 0),
    SERVICE_TREAT    = new Service(BC, FORM_SERVICE, "Treatment"  , 0),
    SERVICE_PERFORM  = new Service(BC, FORM_SERVICE, "Performance", 0),
    SERVICE_DEPOT    = new Service(BC, FORM_SERVICE, "Depot"      , 0),
    SERVICE_REFUGE   = new Service(BC, FORM_SERVICE, "Refuge"     , 0),
    SERVICE_ARMAMENT = new Service(BC, FORM_SERVICE, "Armament"   , 0),
    SERVICE_SHIPPING = new Service(BC, FORM_SERVICE, "Shipping"   , 0),
    SERVICE_CAPTIVES = new Service(BC, FORM_SERVICE, "Captives"   , 0),
    SERVICE_CONSORTS = new Service(BC, FORM_SERVICE, "Consorts"   , 0),
    
    ALL_ABSTRACT_SERVICES[] = Service.typesSoFar() ;
  
  
  
  
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
    
    TASE_STAFF = new DeviceType(BC, "Shock Staff",
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
  //  TODO:  You should have skins associated with some of these.
  final public static OutfitType
    FINERY         = new OutfitType(
      BC, "Finery"        , 2 , 200,
      new Conversion(2, PLASTICS, Fabricator.class, 15, GRAPHIC_MEDIA)
    ),
    OVERALLS       = new OutfitType(
      BC, "Overalls"      , 2, 50,
      new Conversion(1, PLASTICS, Fabricator.class, 5, ASSEMBLY)
    ),
    CAMOUFLAGE     = new OutfitType(
      BC, "Camouflage"    , 3 , 70,
      new Conversion(1, PLASTICS, Fabricator.class, 10, GRAPHIC_MEDIA)
    ),
    SEALSUIT       = new OutfitType(
      BC, "Sealsuit"      , 4 , 150,
      new Conversion(1, PLASTICS, 1, PARTS, Fabricator.class, 15, ASSEMBLY)
    ),
    
    SHIELD_BELT = new OutfitType(
      BC, "Shield Belt"   , 5 , 50,
      new Conversion(1, PARTS, Foundry.class, 5, ASSEMBLY)
    ),
    PARTIAL_ARMOUR = new OutfitType(
      BC, "Partial Armour", 10, 100,
      new Conversion(2, PARTS, Foundry.class, 10, ASSEMBLY)
    ),
    BODY_ARMOUR    = new OutfitType(
      BC, "Body Armour"   , 15, 150,
      new Conversion(5, PARTS, Foundry.class, 15, ASSEMBLY)
    ),
    POWER_ARMOUR   = new OutfitType(
      BC, "Power Armour"  , 20, 250,
      new Conversion(8, PARTS, Foundry.class, 20, ASSEMBLY)
    ),
    GOLEM_ARMOUR   = new OutfitType(
      BC, "Golem Armour"  , 25, 500,
      new Conversion(12, PARTS, Foundry.class, 25, ASSEMBLY)
    ),
    
    INTRINSIC_ARMOUR = new OutfitType(BC, "Intrinsic Armour", 0, 0, null) ;
  final public static Service
    ALL_OUTFITS[] = Service.typesSoFar() ;
  
  final public static Service
    ALL_ITEM_TYPES[] = Service.allTypes() ;
  
  
  
  
  final public static Object TO = new Object() ;
  
  final public static Conversion
    //
    //  Artificer conversions-
    METALS_TO_PARTS = new Conversion(
      1, METAL_ORE, TO, 1, PARTS,
      Foundry.class,
      MODERATE_DC, ASSEMBLY, SIMPLE_DC, CHEMISTRY
    ),
    
    PARTS_TO_CIRCUITRY = new Conversion(
      1, PARTS, TO, 5, CIRCUITRY,
      Foundry.class,
      STRENUOUS_DC, ASSEMBLY, MODERATE_DC, FIELD_THEORY
    ),
    
    //
    //  Fabricator conversions-
    PETROCARBS_TO_PLASTICS = new Conversion(
      1, PETROCARBS, TO, 1, PLASTICS,
      Fabricator.class,
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, GRAPHIC_MEDIA
    ),
    
    CARBS_TO_PLASTICS = new Conversion(
      5, CARBS, TO, 1, PLASTICS,
      Fabricator.class,
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, GRAPHIC_MEDIA
    ),
    
    PLASTICS_TO_DECOR = new Conversion(
      1, PLASTICS, TO, 2, DECOR,
      Fabricator.class,
      STRENUOUS_DC, GRAPHIC_MEDIA, MODERATE_DC, HANDICRAFTS
    ),
    
    //
    //  Audit Office conversions-
    PLASTICS_TO_CREDITS = new Conversion(
      1, PLASTICS, TO, 500, CREDITS,
      AuditOffice.class,
      MODERATE_DC, ACCOUNTING, MODERATE_DC, GRAPHIC_MEDIA
    ),
    
    PLASTICS_TO_PRESSFEED = new Conversion(
      1, PLASTICS, TO, 10, PRESSFEED,
      AuditOffice.class,
      SIMPLE_DC, ACCOUNTING, DIFFICULT_DC, GRAPHIC_MEDIA
    ),
    
    //
    //  Reactor conversions-
    METALS_TO_FUEL = new Conversion(
      5, METAL_ORE, TO, 1, FUEL_CORES,
      Reactor.class,
      MODERATE_DC, CHEMISTRY, MODERATE_DC, FIELD_THEORY
    ),
    
    //
    //  Culture Vats conversions-
    POWER_TO_CARBS = new Conversion(
      1, POWER, TO, 1, CARBS,
      CultureVats.class,
      SIMPLE_DC, CHEMISTRY
    ),
    
    POWER_TO_PROTEIN = new Conversion(
      2, POWER, TO, 1, PROTEIN,
      CultureVats.class,
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, GENE_CULTURE
    ),
    
    POWER_TO_SOMA = new Conversion(
      2, POWER, TO, 1, SOMA,
      CultureVats.class,
      ROUTINE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    ),
    
    POWER_TO_MEDICINE = new Conversion(
      5, POWER, TO, 1, MEDICINE,
      CultureVats.class,
      MODERATE_DC, CHEMISTRY, MODERATE_DC, PHARMACY, ROUTINE_DC, GENE_CULTURE
    ),
    
    SPICE_TO_SOMA = new Conversion(
      2, POWER, 1, SPICE, 1, GREENS, TO, 10, SOMA,
      CultureVats.class,
      ROUTINE_DC, CHEMISTRY, SIMPLE_DC, PHARMACY
    ),
    
    SPICE_TO_MEDICINE = new Conversion(
      5, POWER, 1, SPICE, 1, GREENS, TO, 5, MEDICINE,
      CultureVats.class,
      MODERATE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    ) ;
}




