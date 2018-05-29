package nl.novadoc.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.EnumSet;

@SuppressWarnings("all")
public class Main {

	public static void main(String... args) {

		Normalizer.normalize("", Form.NFC);
		Normalizer.normalize("", Form.NFD);
		Normalizer.normalize("", Form.NFKC);
		Normalizer.normalize("", Form.NFKD);
		
		Map<String, List<Integer>> cat = new HashMap<>();
		for(int i = 0; i < Character.MAX_CODE_POINT; i++) {
			int type = Character.getType(i);
			switch(type) {
			case Character.UNASSIGNED:
				cat.put("Cn", get(cat, "Cn", i));
				break;
			case Character.UPPERCASE_LETTER:
				cat.put("Lu", get(cat, "Lu", i));
				break;
			case Character.LOWERCASE_LETTER:
				cat.put("Ll", get(cat, "Ll", i));
				break;
			case Character.TITLECASE_LETTER:
				cat.put("Lt", get(cat, "Lt", i));
				break;
			case Character.MODIFIER_LETTER:
				cat.put("Lm", get(cat, "Lm", i));
				break;
			case Character.OTHER_LETTER:
				cat.put("Lo", get(cat, "Lo", i));
				break;
			case Character.NON_SPACING_MARK:
				cat.put("Mn", get(cat, "Mn", i));
				break;
			case Character.ENCLOSING_MARK:
				cat.put("Me", get(cat, "Me", i));
				break;
			case Character.COMBINING_SPACING_MARK:
				cat.put("Mc", get(cat, "Mc", i));
				break;
			case Character.DECIMAL_DIGIT_NUMBER:
				cat.put("Nd", get(cat, "Nd", i));
				break;
			case Character.LETTER_NUMBER:
				cat.put("Nl", get(cat, "Nl", i));
				break;
			case Character.OTHER_NUMBER:
				cat.put("No", get(cat, "No", i));
				break;
			case Character.SPACE_SEPARATOR:
				cat.put("Zs", get(cat, "Zs", i));
				break;
			case Character.LINE_SEPARATOR:
				cat.put("Zl", get(cat, "Zl", i));
				break;
			case Character.PARAGRAPH_SEPARATOR:
				cat.put("Zp", get(cat, "Zp", i));
				break;
			case Character.CONTROL:
				cat.put("Cc", get(cat, "Cc", i));
				break;
			case Character.FORMAT:
				cat.put("Cf", get(cat, "Cf", i));
				break;
			case Character.PRIVATE_USE:
				cat.put("Co", get(cat, "Co", i));
				break;
			case Character.SURROGATE:
				cat.put("Cs", get(cat, "Cs", i));
				break;
			case Character.DASH_PUNCTUATION:
				cat.put("Pd", get(cat, "Pd", i));
				break;
			case Character.START_PUNCTUATION:
				cat.put("Ps", get(cat, "Ps", i));
				break;
			case Character.END_PUNCTUATION:
				cat.put("Pe", get(cat, "Pe", i));
				break;
			case Character.INITIAL_QUOTE_PUNCTUATION:
				cat.put("Pi", get(cat, "Pi", i));
				break;
			case Character.FINAL_QUOTE_PUNCTUATION:
				cat.put("Pf", get(cat, "Pf", i));
				break;
			case Character.CONNECTOR_PUNCTUATION:
				cat.put("Pc", get(cat, "Pc", i));
				break;
			case Character.OTHER_PUNCTUATION:
				cat.put("Po", get(cat, "Po", i));
				break;
			case Character.MATH_SYMBOL:
				cat.put("Sm", get(cat, "Sm", i));
				break;
			case Character.CURRENCY_SYMBOL:
				cat.put("Sc", get(cat, "Sc", i));
				break;
			case Character.MODIFIER_SYMBOL:
				cat.put("Sk", get(cat, "Sk", i));
				break;
			case Character.OTHER_SYMBOL:
				cat.put("So", get(cat, "So", i));
				break;
			default:
				System.out.printf("Unknown category %d\n", type);
			}
		}
		
		System.out.println("Controls");
		System.out.printf("CONTROL:      %d\n", cat.get("Cc").get(0));
		System.out.printf("FORMAT:       %d\n", cat.get("Cf").get(0));
		System.out.printf("PRIVATE:      %d\n", cat.get("Co").get(0));
		System.out.printf("SURROGATE:    %d\n", cat.get("Cs").get(0));
		System.out.printf("UNASSIGNED:   %d\n", cat.get("Cn").get(0));
		
		System.out.println("Letters");
		System.out.printf("UPPER:        %d\n", cat.get("Lu").get(0));
		System.out.printf("LOWER:        %d\n", cat.get("Ll").get(0));
		System.out.printf("TITLE:        %d\n", cat.get("Lt").get(0));
		System.out.printf("MODIFIER:     %d\n", cat.get("Lm").get(0));
		System.out.printf("OTHER:        %d\n", cat.get("Lo").get(0));

		System.out.println("Numbers");
		System.out.printf("DECIMAL:      %d\n", cat.get("Nd").get(0));
		System.out.printf("LETTER:       %d\n", cat.get("Nl").get(0));
		System.out.printf("OTHER:        %d\n", cat.get("No").get(0));

		System.out.println("Marks");
		System.out.printf("NON-SPACING:  %d\n", cat.get("Mn").get(0));
		System.out.printf("SPACING:      %d\n", cat.get("Mc").get(0));
		System.out.printf("ENCLOSING:    %d\n", cat.get("Me").get(0));

		System.out.println("Separators");
		System.out.printf("SPACE:        %d\n", cat.get("Zs").get(0));
		System.out.printf("LINE:         %d\n", cat.get("Zl").get(0));
		System.out.printf("PARAGRAPH:    %d\n", cat.get("Zp").get(0));

		System.out.println("Punctuation");
		System.out.printf("DASH:         %d\n", cat.get("Pd").get(0));
		System.out.printf("START:        %d\n", cat.get("Ps").get(0));
		System.out.printf("END:          %d\n", cat.get("Pe").get(0));
		System.out.printf("INITIAL:      %d\n", cat.get("Pi").get(0));
		System.out.printf("FINAL:        %d\n", cat.get("Pf").get(0));
		System.out.printf("CONNECTOR:    %d\n", cat.get("Pc").get(0));
		System.out.printf("OTHER:        %d\n", cat.get("Po").get(0));

		System.out.println("Symbols");
		System.out.printf("MATH:         %d\n", cat.get("Sm").get(0));
		List<Integer> sm = cat.get("Sm");
		for(int c : cat.get("Sm").subList(1, sm.size())) {			
			String s = new String(Character.toChars(c));
			System.out.print(s);
		}
		System.out.println();
		System.out.printf("CURRENCY:     %d\n", cat.get("Sc").get(0));
		System.out.printf("MODIFIER:     %d\n", cat.get("Sk").get(0));
		System.out.printf("OTHER:        %d\n", cat.get("So").get(0));
	}

	private static List<Integer> get(Map<String, List<Integer>> map, String key, int cp) {
		List<Integer> val = map.get(key);
		if(val == null) {
			val = new ArrayList<>();
			val.add(0);
		}
		val.set(0, val.get(0)+1);
		val.add(cp);
		return val;
	}

}
