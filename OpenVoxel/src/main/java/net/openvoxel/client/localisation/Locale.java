package net.openvoxel.client.localisation;

import java.util.HashMap;

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
	static {
		englishInternational = new Locale("int");
		englishUnitedKingdom = new Locale("uk");
		englishUnitedStates = new Locale("usa");
		german = new Locale("det");
		spanish = new Locale("est");
		french = new Locale("frt");
		italian = new Locale("itt");
		korean = new Locale("kot");
		czech = new Locale("tct");
		russian = new Locale("rut");
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
