package com.bbn.marti.tests;

import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.servers.MutableServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * Created on 10/28/15.
 */
public class TakclSanityTest extends AbstractTestClass {

	private static final String className = "TakclSanityTest";

	@Test
	public void ImmutableUserEqualityValidatorTest() {
		Set<AbstractUser> immutableUsers = ImmutableUsers.valueSet();

		for (AbstractUser user : immutableUsers) {
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier().equals(user.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier().equals(user.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier().equals(user.toString()));
//            Assert.assertTrue(user.getConsistentUniqueReadableIdentifier().equals(user));
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier() == user.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier() == user.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier() == user.toString());


			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier().equals(user.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier().equals(user.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier().equals(user.toString()));
//            Assert.assertTrue(user.getConsistentUniqueReadableIdentifier().equals(user));
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier() == user.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier() == user.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(user.getConsistentUniqueReadableIdentifier() == user.toString());

			Assert.assertTrue(user.toString().equals(user.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(user.toString().equals(user.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(user.toString().equals(user.toString()));
//            Assert.assertTrue(user.toString().equals(user));
			Assert.assertTrue(user.toString() == user.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(user.toString() == user.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(user.toString() == user.toString());

			Assert.assertTrue(user.equals(user.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(user.equals(user.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(user.equals(user.toString()));
			Assert.assertTrue(user.equals(user));
		}
	}

	@Test
	public void ProtocolEqualityValidatorTest() {
		ProtocolProfiles[] protocolProfiles = ProtocolProfiles.values();

		for (ProtocolProfiles protocol : protocolProfiles) {
			Assert.assertTrue(protocol.name().equals(protocol.name()));
			Assert.assertTrue(protocol.name().equals(protocol.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(protocol.name().equals(protocol.toString()));
			Assert.assertTrue(protocol.name() == protocol.name());
			Assert.assertTrue(protocol.name() == protocol.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(protocol.name() == protocol.toString());
		}
	}

	@Test
	public void ServerEqualityValidatorTest() {
		for (ImmutableServerProfiles server : ImmutableServerProfiles.valueSet()) {
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier().equals(server.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier().equals(server.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier().equals(server.toString()));
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier() == server.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier() == server.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier() == server.toString());

			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier().equals(server.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier().equals(server.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier().equals(server.toString()));
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier() == server.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier() == server.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(server.getConsistentUniqueReadableIdentifier() == server.toString());

			Assert.assertTrue(server.toString().equals(server.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(server.toString().equals(server.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(server.toString().equals(server.toString()));
			Assert.assertTrue(server.toString() == server.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(server.toString() == server.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(server.toString() == server.toString());

			MutableServerProfile mutableServerProfile = server.getMutableInstance();
			Assert.assertTrue(mutableServerProfile.getConsistentUniqueReadableIdentifier().equals(server.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(mutableServerProfile.toString().equals(server.toString()));
			Assert.assertTrue(mutableServerProfile.getConsistentUniqueReadableIdentifier().equals(server.getConsistentUniqueReadableIdentifier()));
			Assert.assertTrue(mutableServerProfile.getConsistentUniqueReadableIdentifier() == server.getConsistentUniqueReadableIdentifier());
			Assert.assertTrue(mutableServerProfile.toString() == server.toString());
			Assert.assertTrue(mutableServerProfile.getConsistentUniqueReadableIdentifier() == server.getConsistentUniqueReadableIdentifier());
		}
	}
}
