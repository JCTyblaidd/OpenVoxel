package net.openvoxel.client.localisation;

import net.openvoxel.api.PublicAPI;
import net.openvoxel.api.logger.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by James on 23/09/2016.
 *
 * Locale Reference:
 *
 * int  => English(International)
 * uk   => English(United Kingdom)
 * usa  => English(United Stated)
 * det  => German
 * est  => Spanish
 * frt  => French
 * itt  => Italian
 * kot  => Korean
 * tct  => Czech
 * rut  => Russian
 */
public class Locale {

	private static final Locale englishInternational;
	private static Map<String,Locale> localeMap;

	@PublicAPI
	public static Locale getLocaleFromName(String name) {
		return localeMap.get(name);
	}

	@PublicAPI
	public static Locale registerNewLocale(String localeName,String... altNames) {
		Locale exist_locale = getLocaleFromName(localeName);
		if(exist_locale == null) {
			return _create(localeName,altNames);
		}else{
			Logger.getLogger("Locale").Warning("Error registering locale: " + localeName);
			return null;
		}
	}

	private static Locale _create(String name,String... altNames) {
		Locale locale = new Locale(name);
		localeMap.put(name,locale);
		for(String alt : altNames) {
			localeMap.put(alt,locale);
		}
		return locale;
	}

	static {
		localeMap = new HashMap<>();
		englishInternational = _create("int","english","international");
		_create("uk","eng_uk","english_uk");
		_create("usa","us","eng_usa","english_usa");
		_create("det","german");
		_create("est","spanish");
		_create("frt","french","fr");
		_create("itt","italian");
		_create("kot","korean");
		_create("tct","czech");
		_create("rut","russian");
	}

	private static Locale currentLocale = englishInternational;

	@PublicAPI
	public static Locale getLocale() {
		return currentLocale;
	}

	@PublicAPI
	public static void setLocale(Locale locale) {
		currentLocale = locale;
	}

	@PublicAPI
	public static String getLocalizedValue(String k,Object... objs) {
		return getLocale().localize(k,objs);
	}

	private String type;
	private HashMap<String,String> cache;
	private Locale(String type) {
		this.type = type;
		cache = new HashMap<>();
	}

	public String getType() {
		return "locale."+type;
	}

	@PublicAPI
	public void registerLocalisation(String a,String b) {
		cache.put(a,b);
	}

	private String getValue(String key,Object[] data) {
		String v = cache.get(key);
		return v == null ? null : data.length == 0 ? v : String.format(v,data);
	}

	@PublicAPI
	public String localize(String k,Object... objs) {
		String str = getValue(k,objs);
		if(str != null) {
			return String.format(str,objs);
		}else if((str = englishInternational.getValue(k,objs)) != null) {
			return String.format(str,objs);
		}else {
			return k;
		}
	}

}
