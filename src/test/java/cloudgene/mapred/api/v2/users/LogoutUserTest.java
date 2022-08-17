package cloudgene.mapred.api.v2.users;

import java.io.IOException;
import java.util.Date;

import org.json.JSONException;
import org.restlet.resource.ClientResource;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.JobsApiTestCase;
import cloudgene.mapred.util.LoginToken;
import cloudgene.mapred.util.TestServer;
import genepi.db.Database;

public class LogoutUserTest extends JobsApiTestCase {

	@Override
	protected void setUp() throws Exception {
		TestServer.getInstance().start();

		// insert two dummy users
		Database database = TestServer.getInstance().getDatabase();
		UserDao userDao = new UserDao(database);

		User testUser1 = new User();
		testUser1.setUsername("testuser99");
		testUser1.setFullName("test1");
		testUser1.setMail("testuser1@test.com");
		testUser1.setRoles(new String[] { "User" });
		testUser1.setActive(true);
		testUser1.setActivationCode("");
		testUser1.setPassword(HashUtil.hashPassword("testuser99"));
		testUser1.setAcceptedTandC(new Date());
		userDao.insert(testUser1);

	}

	public void testLogout() throws JSONException, IOException {

		LoginToken token = login("testuser99", "testuser99");

		// test protected resource
		ClientResource resource = createClientResource("/api/v2/users/testuser99/profile", token);
		try {
			resource.get();
		} catch (Exception e) {

		}
		assertEquals(200, resource.getStatus().getCode());
		resource.release();

		// logout
		resource = createClientResource("/logout");
		try {
			resource.get();
		} catch (Exception e) {

		}
		assertEquals(200, resource.getStatus().getCode());
		resource.release();

		// test protected resource again
		resource = createClientResource("/api/v2/users/testuser99/profile");
		try {
			resource.get();
		} catch (Exception e) {

		}
		assertNotSame(200, resource.getStatus().getCode());
		resource.release();
	}

}
