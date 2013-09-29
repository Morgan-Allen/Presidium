


package src.game.base ;



//
//  I've decided to put this functionality in a separate class for the sake of
//  de-cluttering and headspace.


public class HoldingUpgrades {
  
  
  
  //
  //  Requirements for housing upgrades come under a couple of main headings.
  //  1.  Building materials.
  //  2.  Food types and general health of inhabitants.
  //  3.  Safety and ambience.
  //  4.  Access to educational, representative and entertainment venues.
  
  //
  //  You'll need to return a list of requirements for each, say whether they've
  //  been satisfied, and, if not, return an error String saying what needs to
  //  be done to satisfy them.
  
  
  
  
  /**  Building materials/furnishings-
    */
  //
  //  All housing levels require life support in proportion to population, and
  //    more if the map has a low biomass rating.
  //  Pyon Shacks require:
  //    1 parts.
  //  Freeborn Holdings require:
  //    2 parts and 1 power.
  //  Citizen Apartments require:
  //    3 parts, 2 power and 1 plastics.
  //  Guilder Manses require:
  //    3 parts, 2 power, 2 plastics, 1 water, 1 circuitry and 1 datalink.
  //
  //  Scavenger slums require 1 parts.  Dreg towers require 3, and 1 power.
  //
  //  Knighted estates require:
  //    2 parts and 1 plastics.
  //  Highborn villas require:
  //    3 parts, 2 plastics, 1 water and 1 decor.
  //  Noble palaces require:
  //    4 parts, 3 plastics, 2 water, 2 decor,
  //    1 power, 1 datalink and 1 trophy.
  
  
  
  /**  Rations/foodstuffs-
    */
  //
  //  All housing levels will put out demand for all food types in proportion
  //    to population- enough for five days per resident.
  //  Pyon Shacks require 1 food type.
  //  Freeborn Holdings require 2 food types.
  //  Citizen Apartments require 2 food types and Soma.
  //  Guilder Manses require 3 food types (including Greens) and Soma.
  //
  //  Dreg Towers require at least 1 food type or Soma.
  //
  //  Knighted estates require 2 food types.
  //  Highborn villas require 3 food types and Spice or Soma.
  //  Noble palaces require 3 food types, Soma and Spice from 2 sources.
  
  
  
  /**  Venues access-
    */
  //
  //  Pyon Shacks require access to a Town Vault.
  //    (basic safety is the concern here.)
  //  Freeborn Holdings require access to a Stock Exchange or Free Market, and
  //  a Cantina.
  //    (free access to goods and services.)
  //  Citizen Apartments require access to an Archives or Creche.
  //    (education/upward mobility for children.)
  //  Guilder Manses require access to an Audit Office or Senate Chamber.
  //    (political/legal representation.)
  //
  //  Slum housing is indifferent to services.
  //
  //  Knighted estates require access to a Bastion.
  //  Highborn villas require access to a Senate Chamber or Arena.
  //  Noble palaces require access to a Pleasure Dome.
  
  
  
  /**  Ambience requirements-
    */
  //  Guilder manses require positive ambience.  (+5 or better.)
  //  Citizen apartments require non-negative ambience.  (0 or better.)
  //  Freeborn holdings and Pyon shacks require squalor no worse than 5.
  
  //  Field Tents/Scavenger slums/Dreg towers are indifferent to ambience, and
  //  allow for higher population densities.
  
  //  Knighted estates require non-negative ambience.  (0 or better.)
  //  Highborn villas require positive ambience.  (5 or better.)
  //  Noble palaces require perfect ambience.  (10 or better.)
}














