package net.openvoxel.client.localisation;

import net.openvoxel.api.logger.Logger;

/**
 * Created by James on 23/09/2016.
 *
 * Loads Locale Data
 */
public class LocaleLoader {

	private static Logger localeLogger = Logger.getLogger("Locale Loader");

	private static Locale load_locale(String id) {
		Locale locale = Locale.getLocaleFromName(id);
		if(locale == null) {
			localeLogger.Warning("Invalid Locale: " + id);
		}
		return locale;
	}

	/**
	 * Loads Locale File, with the format
	 * locale_eng:
	 *  op.a.b.e.txt = hello i am the %X% value that exists
	 * @param fileSource this is the best innit @FEF
	 */
	public static void loadLocaleFile(String fileSource) {
		String[] arrs = fileSource.split("\n");
		Locale currentLocale = null;
		for(String str : arrs) {
			if(str.startsWith("locale_") && str.endsWith(":")) {
				String locale_id = str.substring(7,str.length() - 1);
				currentLocale = load_locale(locale_id);
			}else if(str.length() > 0 && str.contains("=")) {
				String[] obj_list = str.split("=");
				String pre = obj_list[0].trim();
				String post = obj_list[1].trim();
				if(currentLocale != null) {
					currentLocale.registerLocalisation(pre, post);
				}else{
					localeLogger.Warning("Attempting to Load - No Locale: " + str);
				}
			}else if(str.length() > 0) {
				localeLogger.Warning("Invalid Line: " + str);
			}
		}
	}
}
