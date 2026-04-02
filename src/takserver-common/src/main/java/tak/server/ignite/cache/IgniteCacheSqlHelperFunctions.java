package tak.server.ignite.cache;

import java.io.Serializable;
import java.math.BigInteger;

import org.apache.ignite.cache.query.annotations.QuerySqlFunction;

public class IgniteCacheSqlHelperFunctions implements Serializable {
	@QuerySqlFunction
	public static boolean groupsOverlap(String a, String b) {
		if (a == null || b == null)
			return false;
		if (a.length() != b.length())
			return false;

		BigInteger aBits = new BigInteger(a, 2);
		BigInteger bBits = new BigInteger(b, 2);

		return aBits.and(bBits).signum() != 0;
	}
}
