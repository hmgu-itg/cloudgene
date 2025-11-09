package cloudgene.mapred.api.v2.users;

import java.util.Date;
import java.util.List;

import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cloudgene.mapred.core.Country;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.database.CountryDao;
import cloudgene.mapred.representations.JSONAnswer;
import cloudgene.mapred.util.BaseResource;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.MailUtil;
import cloudgene.mapred.util.Template;

import java.util.Arrays;

public class RegisterUser extends BaseResource {
	private static final Log log = LogFactory.getLog(RegisterUser.class);

	public static final String DEFAULT_ROLE = "User";

	public static final String DEFAULT_ANONYMOUS_ROLE = "Anonymous_User";

	@Post
	public Representation post(Representation entity) {

		Log log = LogFactory.getLog(RegisterUser.class);

		String hostname = getSettings().getServerUrl();

		Form form = new Form(entity);
		String username = form.getFirstValue("username");
		String fullname = form.getFirstValue("full-name");
		String mail = form.getFirstValue("mail");
		String newPassword = form.getFirstValue("new-password");
		String confirmNewPassword = form.getFirstValue("confirm-new-password");
		String instituteEmail = form.getFirstValue("institute-mail");
		String instituteName = form.getFirstValue("institute-name");
		String instituteAddress1 = form.getFirstValue("institute-address1");
		String instituteAddress2 = form.getFirstValue("institute-address2");
		String instituteCity = form.getFirstValue("institute-city");
		String institutePostCode = form.getFirstValue("institute-postcode");
		String instituteCountry = form.getFirstValue("institute-country");
		String termsAndConditions = form.getFirstValue("accept-terms-and-conditions");
		String termsAndConditionsCountry = form.getFirstValue("accept-eu");
		String enabled_2fa = form.getFirstValue("select-2fa");

		// check user accepted terms of service
		if (!termsAndConditions.equals("on")) {
			return new JSONAnswer("Must accept Terms of Service.", false);
		}
		if (!termsAndConditionsCountry.equals("on")) {
			return new JSONAnswer("Must agree to only use within EU-/EEA-country.", false);
		}

		// check username
		String error = User.checkUsername(username);
		if (error != null) {
			return new JSONAnswer(error, false);
		}
		UserDao dao = new UserDao(getDatabase());
		if (dao.findByUsername(username) != null) {
			return new JSONAnswer("Username already exists.", false);
		}

		boolean mailProvided = (mail != null && !mail.isEmpty());
		log.debug("Email required: " + getSettings().isEmailRequired());
		if (getSettings().isEmailRequired() || mailProvided) {
			// check email
			error = User.checkMail(mail);
			if (error != null) {
				return new JSONAnswer(error, false);
			}
			if (dao.findByMail(mail) != null) {
				return new JSONAnswer("E-Mail is already registered.", false);
			}
		}

		CountryDao countryDao = new CountryDao(getDatabase());
		List<Country> countries = countryDao.findByQuery(instituteCountry);
		Country country = countries.get(0);
		String[] roles;
		if (country.getAllowed()) {
			roles = new String[] { DEFAULT_ROLE };
		} else {
			roles = new String[] { "" };
		}

		// check password
		error = User.checkPassword(newPassword, confirmNewPassword);
		if (error != null) {
			return new JSONAnswer(error, false);
		}

		// check name
		error = User.checkName(fullname);
		if (error != null) {
			return new JSONAnswer(error, false);
		}

		User newUser = new User();
		newUser.setUsername(username);
		newUser.setFullName(fullname);
		newUser.setMail(mail);
		newUser.setRoles(roles);
		newUser.setPassword(HashUtil.hashPassword(newPassword));
		newUser.setInstituteEmail(instituteEmail);
		newUser.setInstituteName(instituteName);
		newUser.setInstituteAddress1(instituteAddress1);
		newUser.setInstituteAddress2(instituteAddress2);
		newUser.setInstituteCity(instituteCity);
		newUser.setInstitutePostCode(institutePostCode);
		newUser.setInstituteCountry(instituteCountry);
		newUser.setAcceptedTandC(new Date());
		newUser.setAcceptedCountry(new Date());

		log.debug("New user '" + username + "' being created");
		try {

			// if email server configured, send mails with activation link. Else
			// activate user immediately.

			if (getSettings().getMail() != null && mailProvided) {

				String activationKey = HashUtil.getActivationHash(newUser);
				newUser.setActive(false);
				newUser.setActivationCode(activationKey);

				// send email with activation code
				String application = getSettings().getName();
				String subject = "[" + application + "] Signup activation";
				String activationLink = hostname + "/#!activate/" + username + "/" + activationKey;
				String body;
				if (enabled_2fa.equals("on")){
				    // QRcode=
				    //body = getWebApp().getTemplate(Template.REGISTER_MAIL_2FA, fullname, application, activationLink,QRcode);
				    body = getWebApp().getTemplate(Template.REGISTER_MAIL, fullname, application, activationLink);
				}
				else{
				    body = getWebApp().getTemplate(Template.REGISTER_MAIL, fullname, application, activationLink);
				}
				log.debug("Sending email with activation code");
				MailUtil.send(getSettings(), mail, subject, body);

			} else {

				newUser.setActive(true);
				newUser.setActivationCode("");

			}

			log.info(String.format("Registration: New user %s (ID %s - email %s - roles %s)", newUser.getUsername(),
					newUser.getId(), newUser.getMail(), Arrays.toString(newUser.getRoles())));
			MailUtil.notifySlack(getSettings(), "Hi! say hello to " + username + " (" + mail + ") :hugging_face:");

			dao.insert(newUser);

			return new JSONAnswer("User sucessfully created.", true);

		} catch (Exception e) {

			return new JSONAnswer(e.getMessage(), false);

		}

	}
}
