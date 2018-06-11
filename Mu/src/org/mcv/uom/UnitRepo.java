package org.mcv.uom;

import java.util.HashMap;
import java.util.Map;

public class UnitRepo {

	static final String[] SIPREFIXES = { "d", "c", "m", "µ", "n", "p", "f", "a", "z", "y", "da", "h", "k", "M", "G",
			"T", "P", "E", "Z", "Y" };

	public static Map<String, UnitDef> symtable = new HashMap<>();
	public static Map<String, String> catUnit = new HashMap<>();

	static {
		// empty unit
		unitDef("Unit", "1");

		// SI base units
		unitDefs("Time", "s");
		genTimeUnits();
		unitDefs("Distance", "m");
		unitDefs("Temperature", "K");
		unitDefs("ElectricCurrent", "A");
		unitDefs("LuminousIntensity", "cd");
		unitDefs("Mass", "kg");
		unitDefs("Mass", "g", 0, 1.0 / 1000);
		unitDefs("AmountOfSubstance", "mol");
		unitDef("Angle", "rad");
		unitDef("SolidAngle", "sr");

		// temperature
		unitDef("Temperature", "°C", 273.15, 1.0);
		unitDef("Temperature", "°F", 459.67, 5.0/9.0);
		
		// ???
		unitDef("Percent", "%", 0.0, 1.0 / 100);

		// derived
		unitDefs("Frequency", "Hz", "1/s");
		unitDefs("Force", "N", "m*kg/s^2");
		unitDefs("Pressure", "Pa", "N/m^2");
		unitDefs("Energy", "J", "N*m");
		unitDefs("Power", "W", "J/s");
		unitDefs("ElectricCharge", "C", "s*A");
		unitDefs("ElectricPotential", "V", "W/A");
		unitDefs("ElectricCapacitance", "F", "C/V");
		unitDefs("ElectricResistance", "Ω", "V/A");
		unitDefs("ElectricConductance", "S", "A/V");
		unitDefs("MagneticFlux", "Wb", "V*s");
		unitDefs("MagenticFluxDensity", "T", "Wb/m^2");
		unitDefs("ElectricInductance", "H", "Wb/A");
		unitDefs("LuminousFlux", "lm", "cd*sr");
		unitDefs("Illuminance", "lx", "lm/m^2");
		unitDefs("Radioactivity", "Bq", "1/s");
		unitDefs("RadiationDoseAbsorbed", "Gy", "J/kg");
		unitDefs("RadiationDoseEffective", "Sv", "J/kg");
		unitDefs("CatalyticActivity", "kat", "mol/s");
		unitDefs("Speed", "m/s", "m/s");
		unitDefs("Acceleration", "m/s²", "m/s^2");
		unitDefs("Area", "m²", "m^2");
		unitDefs("Volume", "m³", "m^3");
		unitDefs("Speed", "km/h", "km/h");
	}

	// add base unit
	public static void unitDef(String category, String unit) {
		UnitDef def = new UnitDef(category, unit, 0.0, 1.0);
		symtable.put(unit, def);
		catUnit.put(category, unit);
	}

	// add SI base unit
	public static void unitDefs(String category, String unit) {
		UnitDef def = new UnitDef(category, unit, 0.0, 1.0);
		symtable.put(unit, def);
		catUnit.put(category, unit);
		for (String pfx : SIPREFIXES) {
			def = new UnitDef(category, pfx + unit, 0.0, UnitValue.factor(pfx));
			symtable.put(pfx + unit, def);
		}
	}

	// add derived unit
	public static void unitDef(String category, String unit, String unitspec) {
		UnitDef def = new UnitDef(category, unit, UnitValue.unitSpec(unitspec));
		symtable.put(unit, def);
	}

	// add SI derived unit
	public static void unitDefs(String category, String unit, String unitspec) {
		UnitDef def = new UnitDef(category, unit, UnitValue.unitSpec(unitspec));
		symtable.put(unit, def);
		for (String pfx : SIPREFIXES) {
			def = new UnitDef(category, pfx + unit, 0.0, UnitValue.factor(pfx));
			symtable.put(pfx + unit, def);
		}
	}

	// add subunit
	public static void unitDef(String category, String unit, double offset, double factor) {
		UnitDef def = new UnitDef(category, unit, offset, factor);
		symtable.put(unit, def);
	}

	// add SI subunit
	public static void unitDefs(String category, String unit, double offset, double factor) {
		UnitDef def = new UnitDef(category, unit, offset, factor);
		symtable.put(unit, def);
		for (String pfx : SIPREFIXES) {
			def = new UnitDef(category, pfx + unit, offset, factor * UnitValue.factor(pfx));
			symtable.put(pfx + unit, def);
		}
	}

	private static void genTimeUnits() {
		// min
		UnitDef def = new UnitDef("Time", "min", 0.0, 60.0);
		symtable.put("min", def);
		// hour
		def = new UnitDef("Time", "h", 0.0, 60.0 * 60.0);
		symtable.put("h", def);
		// day
		def = new UnitDef("Time", "d", 0.0, 60.0 * 60.0 * 24.0);
		symtable.put("d", def);
		// week
		def = new UnitDef("Time", "wk", 0.0, 60.0 * 60.0 * 24.0 * 7.0);
		symtable.put("wk", def);
		// year
		def = new UnitDef("Time", "yr", 0.0, 60.0 * 60.0 * 24.0 * 365.254);
		symtable.put("yr", def);
	}

}
