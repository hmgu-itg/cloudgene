package cloudgene.mapred.api.v2.users;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.resource.ClientResource;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.JobsApiTestCase;
import cloudgene.mapred.util.LoginToken;
import cloudgene.mapred.util.TestServer;

public class UserProfileTest extends JobsApiTestCase {

	@Override
	protected void setUp() throws Exception {
		TestServer.getInstance().start();

		// insert two dummy users
		Database database = TestServer.getInstance().getDatabase();
		UserDao userDao = new UserDao(database);

		User testUser1 = new User();
		testUser1.setUsername("test1");
		testUser1.setFullName("test1");
		testUser1.setMail("test1@test.com");
		testUser1.setRoles(new String[] { "User" });
		testUser1.setActive(true);
		testUser1.setActivationCode("");
		testUser1.setPassword(HashUtil.hashPassword("Test1Password"));
		testUser1.setInstituteEmail("itg-boss@helmholtz-munich.de");
		testUser1.setInstituteName("ITG");
		testUser1.setInstituteAddress1("Ingolstädter Landstraße 1");
		testUser1.setInstituteCity("Munich");
		testUser1.setInstitutePostCode("D-85764");
		testUser1.setInstituteCountry("Germany");
		userDao.insert(testUser1);

		User testUser2 = new User();
		testUser2.setUsername("test2");
		testUser2.setFullName("test2");
		testUser2.setMail("test2@test.com");
		testUser2.setRoles(new String[] { "User" });
		testUser2.setActive(true);
		testUser2.setActivationCode("");
		testUser2.setPassword(HashUtil.hashPassword("Test2Password"));
		testUser2.setInstituteEmail("itg-boss@helmholtz-munich.de");
		testUser2.setInstituteName("ITG");
		testUser2.setInstituteAddress1("Ingolstädter Landstraße 1");
		testUser2.setInstituteCity("Munich");
		testUser2.setInstitutePostCode("D-85764");
		testUser2.setInstituteCountry("Germany");
		userDao.insert(testUser2);

	}

	public void testGetWithWrongCredentials() throws JSONException, IOException {
		LoginToken token = new LoginToken();
		token.setCsrfToken("wrong-token");
		ClientResource resource = createClientResource("/api/v2/users/test1/profile", token);
		try {
			resource.get();
		} catch (Exception e) {

		}
		assertNotSame(200, resource.getStatus().getCode());
		resource.release();
	}

	public void testGetWithCorrectCredentials() throws JSONException, IOException {
		// login as user test1 and get profile. username is ignored, returns
		// allways auth user's profile. just for better urls
		LoginToken token = login("test1", "Test1Password");
		ClientResource resource = createClientResource("/api/v2/users/test1/profile", token);
		try {
			resource.get();
		} catch (Exception e) {

		}
		assertEquals(200, resource.getStatus().getCode());
		JSONObject object = new JSONObject(resource.getResponseEntity().getText());
		assertFalse(object.has("password"));
		assertEquals(object.get("username"), "test1");
		assertEquals(object.get("mail"), "test1@test.com");
		resource.release();
	}

	public void testUpdateWithCorrectCredentials() throws JSONException, IOException {

		// login as user test1
		LoginToken token = login("test2", "Test2Password");

		// try to update password for test2
		ClientResource resource = createClientResource("/api/v2/users/me/profile", token);
		Form form = new Form();
		form.set("username", "test2");
		form.set("full-name", "new full-name");
		form.set("mail", "test1@test.com");
		form.set("new-password", "new-Password27");
		form.set("confirm-new-password", "new-Password27");

		resource.post(form);
		assertEquals(200, resource.getStatus().getCode());
		JSONObject object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals(object.get("success"), true);
		assertTrue(object.get("message").toString().contains("User profile successfully updated"));
		resource.release();

		// try login with old password
		resource = createClientResource("/login");
		form = new Form();
		form.set("loginUsername", "test2");
		form.set("loginPassword", "Test2Password");
		resource.post(form);

		assertEquals(200, resource.getStatus().getCode());
		object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals("Login Failed! Wrong Username or Password.", object.getString("message"));
		assertEquals(false, object.get("success"));
		assertEquals(0, resource.getResponse().getCookieSettings().size());
		resource.release();

		// try login with new password
		resource = createClientResource("/login");
		form = new Form();
		form.set("loginUsername", "test2");
		form.set("loginPassword", "new-Password27");
		resource.post(form);

		assertEquals(200, resource.getStatus().getCode());
		object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals("Login successfull.", object.getString("message"));
		assertEquals(true, object.get("success"));
		assertEquals(1, resource.getResponse().getCookieSettings().size());
		resource.release();
	}

	public void testUpdateWithWrongCredentials() throws JSONException, IOException {

		// login as user test1
		LoginToken token = login("test1", "Test1Password");

		// try to update password for test2
		ClientResource resource = createClientResource("/api/v2/users/me/profile", token);
		Form form = new Form();
		form.set("username", "test2");
		form.set("full-name", "test2 test1");
		form.set("mail", "test1@test.com");
		form.set("new-password", "Password27");
		form.set("confirm-new-password", "Password27");

		resource.post(form);
		assertEquals(200, resource.getStatus().getCode());
		JSONObject object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals(object.get("success"), false);
		assertTrue(object.get("message").toString().contains("not allowed to change"));
		resource.release();
	}

	public void testUpdateWithMissingCSRFToken() throws JSONException, IOException {

		// login as user test1
		LoginToken token = login("test1", "Test1Password");

		// try to update password with missing csrf token

		token.setCsrfToken("");
		ClientResource resource = createClientResource("/api/v2/users/me/profile", token);

		Form form = new Form();
		form.set("username", "test1");
		form.set("full-name", "new full-name");
		form.set("mail", "test1@test.com");
		form.set("new-password", "new-Password27");
		form.set("confirm-new-password", "new-Password27");

		try {
			resource.post(form);
			assertFalse(true);
		} catch (Exception e) {

		}
		assertNotSame(200, resource.getStatus().getCode());

		resource.release();
	}

	public void testUpdateWithWrongConfirmPassword() throws JSONException, IOException {

		// login as user test1
		LoginToken token = login("test1", "Test1Password");

		ClientResource resource = createClientResource("/api/v2/users/me/profile", token);
		Form form = new Form();

		// try to update with wrong password
		form.set("username", "test1");
		form.set("full-name", "test1 new");
		form.set("mail", "test1@test.com");
		form.set("new-password", "aaa");
		form.set("confirm-new-password", "abbb");

		resource.post(form);
		assertEquals(200, resource.getStatus().getCode());
		JSONObject object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals(object.get("success"), false);
		assertTrue(object.get("message").toString().contains("check your passwords"));
		resource.release();
	}

	public void testUpdatePasswordWithMissingLowercase() throws JSONException, IOException {

		// login as user test1
		LoginToken token = login("test1", "Test1Password");

		ClientResource resource = createClientResource("/api/v2/users/me/profile", token);
		Form form = new Form();
		// try to update with wrong password
		form.set("username", "test1");
		form.set("full-name", "test1 new");
		form.set("mail", "test1@test.com");
		form.set("new-password", "PASSWORD2727");
		form.set("confirm-new-password", "PASSWORD2727");

		resource.post(form);
		assertEquals(200, resource.getStatus().getCode());
		JSONObject object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals(object.get("success"), false);
		assertTrue(object.get("message").toString().contains("least one lowercase"));
		resource.release();

	}

	public void testUpdatePasswordWithMissingNumber() throws JSONException, IOException {

		// login as user test1
		LoginToken token = login("test1", "Test1Password");

		ClientResource resource = createClientResource("/api/v2/users/me/profile", token);
		Form form = new Form();

		// try to update with wrong password
		form.set("username", "test1");
		form.set("full-name", "test1 new");
		form.set("mail", "test1@test.com");
		form.set("new-password", "PASSWORDpassword");
		form.set("confirm-new-password", "PASSWORDpassword");

		resource.post(form);
		assertEquals(200, resource.getStatus().getCode());
		JSONObject object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals(object.get("success"), false);
		assertTrue(object.get("message").toString().contains("least one number"));
		resource.release();
	}

	public void testUpdatePasswordWithMissingUppercase() throws JSONException, IOException {

		// login as user test1
		LoginToken token = login("test1", "Test1Password");

		ClientResource resource = createClientResource("/api/v2/users/me/profile", token);
		Form form = new Form();

		// try to update with wrong password
		form.set("username", "test1");
		form.set("full-name", "test1 new");
		form.set("mail", "test1@test.com");
		form.set("new-password", "passwordword27");
		form.set("confirm-new-password", "passwordword27");

		resource.post(form);
		assertEquals(200, resource.getStatus().getCode());
		JSONObject object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals(object.get("success"), false);
		assertTrue(object.get("message").toString().contains("least one uppercase"));
		resource.release();
	}

	public void testUpdateWithEmptyEmail() throws JSONException, IOException {

		// login as user test1
		LoginToken token = login("test1", "Test1Password");

		ClientResource resource = createClientResource("/api/v2/users/me/profile", token);
		Form form = new Form();

		// try to update with wrong password
		form.set("username", "test1");
		form.set("full-name", "test1 new");
		form.set("mail", "");

		resource.post(form);
		assertEquals(200, resource.getStatus().getCode());
		JSONObject object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals(object.get("success"), false);
		assertTrue(object.get("message").toString().contains("E-Mail is required."));
		resource.release();
	}

	public void testDowngradeAndUpgradeAccount() throws JSONException, IOException {

		TestServer.getInstance().getSettings().setEmailRequired(false);

		// login as user test1
		LoginToken token = login("test1", "Test1Password");

		ClientResource resource = createClientResource("/api/v2/users/me/profile", token);
		Form form = new Form();

		// downgrade by removing email
		form.set("username", "test1");
		form.set("full-name", "test1 new");
		form.set("mail", "");

		resource.post(form);
		assertEquals(200, resource.getStatus().getCode());
		JSONObject object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals(object.get("success"), true);
		assertTrue(object.get("message").toString().contains("downgraded"));
		resource.release();

		//check role
		UserDao dao = new UserDao(TestServer.getInstance().getDatabase());
		User user = dao.findByUsername("test1");
		assertEquals(1, user.getRoles().length);
		assertEquals(RegisterUser.DEFAULT_ANONYMOUS_ROLE, user.getRoles()[0]);

		//upgrade by adding email

		form = new Form();

		// downgrade by removing email
		form.set("username", "test1");
		form.set("full-name", "test1 new");
		form.set("mail", "test1@test.com");

		resource.post(form);
		assertEquals(200, resource.getStatus().getCode());
		object = new JSONObject(resource.getResponseEntity().getText());
		assertEquals(object.get("success"), true);
		assertTrue(object.get("message").toString().contains("upgraded"));
		resource.release();

		//check role
		user = dao.findByUsername("test1");
		assertEquals(1, user.getRoles().length);
		assertEquals(RegisterUser.DEFAULT_ROLE, user.getRoles()[0]);

		TestServer.getInstance().getSettings().setEmailRequired(true);

	}

}
