package com.atakmap.android.icons;

import java.util.Locale;

public class Icon2525bTypeResolver {

	public static String mil2525bFromCotType(String type) {
		if (type != null && type.indexOf("a")==0 && type.length() > 2) {
			String s2525B="s";

			switch(type.charAt(2)) {
				case 'f':
				case 'a':
					s2525B="sf";
					break;
				case 'n':
					s2525B="sn";
					break;
				case 's':
				case 'j':
				case 'k':
				case 'h':
					s2525B="sh";
					break;
				case 'u':
				default:
					s2525B="su";
					break;
			}
			StringBuilder s2525BSB = new StringBuilder(s2525B);
			for (int x=4; x<type.length(); x+=2) {
				char t[] = {
						type.charAt(x)
				};
				String s=new String(t);
				s2525BSB.append(s.toLowerCase(Locale.US));
				if (x==4) {
					s2525BSB.append("p");
				}
			}
			for (int x = s2525BSB.length(); x < 15; x++) {
				if (x == 10 && s2525BSB.charAt(2) == 'g'
						&& s2525BSB.charAt(4) == 'i') {
					s2525BSB.append("h");
				} else {
					s2525BSB.append("-");
				}
			}
			return s2525BSB.toString();
		}

		return "";
	}

}
