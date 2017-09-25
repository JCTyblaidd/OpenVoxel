package net.openvoxel.api.mods;

import net.openvoxel.api.PublicAPI;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by James on 25/08/2016.
 *
 * Marks a class as a value mod
 */
@PublicAPI
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mod {

	/**
	 * @return Unique Identifier for Your Mod
	 * */
	String id();

	/**
	 * @return the actual name of the mod
	 */
	String name() default "";

	/**
	 * @return Version String : (Must be Parseable from {@link net.openvoxel.api.util.Version}::parseVersion
	 */
	String version() default "0.0.1-Alpha";

	/**
	 * @return the Minimum Accepteable Version of OpenVoxel for which this mod can be Run
	 */
	String minimumOpenVoxelVersion() default "0.0.1-Alpha";

	/**
	 * Each Mod Must Be Parsable by ModDependency::parseDependency()
	 *      Valid Formats:
	 *          modId                       (modID)
	 *          modID&minVerStr             (modID and version >= minVerStr)
	 *          modID&minVerStr->maxVerStr  (modID and version >= minVerStr and version <= maxVerStr)
	 *
	 * @return Mods That Must Exist For This To Load
	 */
	String[] requiredMods() default {"vanilla"};

	/**
	 *  Must Only Contain Mod IDs
	 *
	 *  IMPORTANT: this will not error if the modID doesn't exist
	 *
	 * @return Mods IDs For This Mod Should Load After
	 */
	String[] loadAfter() default {"vanilla"};

	/**
	 *  Must Only Contain Mod IDs
	 *
	 *  IMPORTANT: this will not error if the modID doesn't exist
	 *
	 * @return Mod IDs That Should Load After This Mod
	 */
	String[] loadBefore() default {};

	/**
	 * @return Does the client need this mod if the server has it
	 */
	boolean requiresClient() default true;
	/**
	 * @return Does the server need this mod if the client has it
	 */
	boolean requiresServer() default true;

	/**
	 * Used to bring similar mods together
	 * @return the name of any parent mod
	 */
	String parent() default "";
}