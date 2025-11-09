package cloudgene.mapred.api.v2.users;

import java.util.Date;
import java.util.List;
import java.util.Base64;
import java.util.Arrays;

import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.codec.binary.Base32;
import java.security.SecureRandom;

import cloudgene.mapred.core.Country;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.database.CountryDao;
import cloudgene.mapred.representations.JSONAnswer;
import cloudgene.mapred.util.BaseResource;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.MailUtil;
import cloudgene.mapred.util.Template;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;

import com.google.zxing.common.BitMatrix;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.awt.image.BufferedImage;
// import javax.imageio.ImageIO;

public class RegisterUser extends BaseResource {
	private static final Log log = LogFactory.getLog(RegisterUser.class);

	public static final String DEFAULT_ROLE = "User";

	public static final String DEFAULT_ANONYMOUS_ROLE = "Anonymous_User";

    public static String createQR(String ga_url,int height,int width) throws IOException, WriterException {
	BitMatrix matrix = new MultiFormatWriter().encode(ga_url,BarcodeFormat.QR_CODE,width,height);
	//BufferedImage bi=MatixToImageWriter.toBufferedImage(matrix);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	MatrixToImageWriter.writeToStream(matrix,"png",baos);
	//ImageIO.write(bi,"png",baos);
	return new String(Base64.getEncoder().encode(baos.toByteArray()));
    }
    
    public static String getGoogleAuthenticatorURL(String secretKey, String account, String issuer) throws IllegalStateException {
	try {
	    return "otpauth://totp/"
                + URLEncoder.encode(issuer + ":" + account, "UTF-8").replace("+", "%20")
                + "?secret=" + URLEncoder.encode(secretKey, "UTF-8").replace("+", "%20")
                + "&issuer=" + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
	} catch (UnsupportedEncodingException e) {
	    throw new IllegalStateException(e);
	}
    }
    
    public static String generateSecretKey() {
	SecureRandom random = new SecureRandom();
	byte[] bytes = new byte[20];
	random.nextBytes(bytes);
	Base32 base32 = new Base32();
	return base32.encodeToString(bytes);
    }

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

		String QR=null;
		String secret_key=null;
		User newUser = new User();
		newUser.setUsername(username);
		newUser.setFullName(fullname);
		newUser.setMail(mail);
		newUser.setRoles(roles);
		newUser.setPassword(HashUtil.hashPassword(newPassword));
		if (enabled_2fa.equals("on")){
		    secret_key=generateSecretKey();
		    log.debug("key: "+secret_key);
		    newUser.set2FA(HashUtil.hashPassword(secret_key));
		}
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
				    String GA_url=getGoogleAuthenticatorURL(secret_key,mail,"HMIS");
				    log.debug("GA_url: "+GA_url);
				    QR=createQR(GA_url,64,64);
				    log.debug("QR: "+QR);
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

			if (enabled_2fa.equals("on")){
			return new JSONAnswer("User sucessfully created. QR code: "+QR, true);
			}
			else{
			    return new JSONAnswer("User sucessfully created.", true);
			}

		} catch (Exception e) {

			return new JSONAnswer(e.getMessage(), false);

		}

	}
}
