package net.openvoxel.client.localisation;

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

	public static final Locale englishInternational;
	public static final Locale englishUnitedKingdom;
	public static final Locale englishUnitedStates;
	public static final Locale german;
	public static final Locale spanish;
	public static final Locale french;
	public static final Locale italian;
	public static final Locale korean;
	public static final Locale czech;
	public static final Locale russian;

	private static Map<String,Locale> localeMap;

	public static Locale getLocaleFromName(String name) {
		return localeMap.get(name);
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
		englishInternational = _create("int","ini","english","international");
		englishUnitedKingdom = _create("uk","eng_uk","english_uk");
		englishUnitedStates = _create("usa","us","eng_usa","english_usa");
		german = _create("det","german");
		spanish = _create("est","spanish");
		french = _create("frt","french","fr");
		italian = _create("itt","italian");
		korean = _create("kot","korean");
		czech = _create("tct","czech");
		russian = _create("rut","russian");
	}

	private static Locale currentLocale = englishInternational;

	public static Locale getLocale() {
		return currentLocale;
	}

	public void setLocale(Locale locale) {
		currentLocale = locale;
	}

	private String type;
	private HashMap<String,String> cache;
	public Locale(String type) {
		this.type = type;
		cache = new HashMap<>();
	}

	public String getType() {
		return "locale."+type;
	}

	public void registerLocalisation(String a,String b) {
		cache.put(a,b);
	}

	private String getValue(String key,Object[] data) {
		String v = cache.get(key);
		return v == null ? null : data.length == 0 ? v : String.format(v,data);
	}

	public String localize(String k,Object... objs) {
		String str = getValue(k,objs);
		if(str != null) {
			return str;
		}else if((str = englishInternational.getValue(k,objs)) != null) {
			return str;
		}else {
			return k;
		}
	}

}
