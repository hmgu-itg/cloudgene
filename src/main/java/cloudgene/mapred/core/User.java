package cloudgene.mapred.core;

import java.util.Date;
import java.util.regex.Pattern;

public class User {

	public static final String ROLE_SEPARATOR = ",";

	public static final String ROLE_ADMIN = "admin";

	private String username;

	private String password;

	private int id;

	private String fullName = "";

	private String mail;

	private String[] roles = new String[0];

	private boolean active = true;

	private String activationKey = null;

	private String apiToken = "";

	private Date lastLogin;

	private Date lockedUntil;

	private int loginAttempts;

    private int maxRunningJobs=2;

	private Date apiTokenExpiresOn = null;

	private String instituteEmail;

	private String instituteName;

	private String instituteAddress1;

	private String instituteAddress2;

	private String instituteCity;

	private String institutePostCode;

	private String instituteCountry;

	private Date acceptedTandC = new Date();

	private Date acceptedCountry = new Date();

	private Date acceptedPermission = new Date();

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(String pwd) {
		this.password = pwd;
	}

	public String getPassword() {
		return password;
	}

	public int getMaxRunningJobs() {
		return maxRunningJobs;
	}

	public void setMaxRunningJobs(int n) {
		this.maxRunningJobs=n;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getFullName() {
		return fullName;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getMail() {
		return mail;
	}

	public void setRoles(String[] roles) {
		this.roles = roles;
	}

	public String[] getRoles() {
		return roles;
	}

	public boolean hasRole(String role) {
		if (roles == null) {
			return false;
		}
		for (int i = 0; i < roles.length; i++) {
			if (roles[i].equalsIgnoreCase(role)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasRole(String[] roles) {
		if (this.roles == null || roles == null) {
			return false;
		}
		for (int i = 0; i < roles.length; i++) {
			if (hasRole(roles[i])) {
				return true;
			}
		}
		return false;
	}

	public boolean isAdmin() {
		return hasRole(ROLE_ADMIN);
	}

	public void makeAdmin() {
		setRoles(new String[] { ROLE_ADMIN });
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getActivationCode() {
		return activationKey;
	}

	public void setActivationCode(String activationKey) {
		this.activationKey = activationKey;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	public String getApiToken() {
		return apiToken;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLockedUntil(Date lockedUntil) {
		this.lockedUntil = lockedUntil;
	}

	public Date getLockedUntil() {
		return lockedUntil;
	}

	public void setLoginAttempts(int loginAttempts) {
		this.loginAttempts = loginAttempts;
	}

	public int getLoginAttempts() {
		return loginAttempts;
	}
	
	public void setApiTokenExpiresOn(Date apiTokenExpiresOn) {
		this.apiTokenExpiresOn = apiTokenExpiresOn;
	}
	
	public Date getApiTokenExpiresOn() {
		return apiTokenExpiresOn;
	}

	public void setInstituteEmail(String instituteEmail) {
		this.instituteEmail = instituteEmail;
	}

	public String getInstituteEmail() {
		return instituteEmail;
	}

	public void setInstituteName(String instituteName) {
		this.instituteName = instituteName;
	}

	public String getInstituteName() {
		return instituteName;
	}

	public void setInstituteAddress1(String instituteAddress1) {
		this.instituteAddress1 = instituteAddress1;
	}

	public String getInstituteAddress1() {
		return instituteAddress1;
	}

	public void setInstituteAddress2(String instituteAddress2) {
		this.instituteAddress2 = instituteAddress2;
	}

	public String getInstituteAddress2() {
		return instituteAddress2;
	}

	public void setInstituteCity(String instituteCity) {
		this.instituteCity = instituteCity;
	}

	public String getInstituteCity() {
		return instituteCity;
	}

	public void setInstitutePostCode(String institutePostCode) {
		this.institutePostCode = institutePostCode;
	}

	public String getInstitutePostCode() {
		return institutePostCode;
	}

	public void setInstituteCountry(String instituteCountry) {
		this.instituteCountry = instituteCountry;
	}

	public String getInstituteCountry() {
		return instituteCountry;
	}

	public void setAcceptedTandC(Date acceptedTandC) {
		this.acceptedTandC = acceptedTandC;
	}

	public Date getAcceptedTandC() {
		return acceptedTandC;
	}

	public void setAcceptedCountry(Date acceptedCountry) {
		this.acceptedCountry = acceptedCountry;
	}

	public Date getAcceptedCountry() {
		return acceptedCountry;
	}

	public static String checkUsername(String username) {

		if (username == null || username.isEmpty()) {
			return "The username is required.";
		}

		if (username.length() < 4) {
			return "The username must contain at least four characters.";

		}

		if (!Pattern.matches("^[a-zA-Z0-9]+$", username)) {
			return "Your username is not valid. Only characters A-Z, a-z and digits 0-9 are acceptable.";
		}

		return null;

	}

	public static String checkPassword(String password, String confirmPassword) {

		if (password == null || confirmPassword == null || password.isEmpty() || !password.equals(confirmPassword)) {
			return "Please check your passwords.";
		}

		if (password.length() < 6) {
			return "Password must contain at least six characters!";
		}

		if (!Pattern.compile("[0-9]").matcher(password).find()) {
			return "Password must contain at least one number (0-9)!";
		}

		if (!Pattern.compile("[a-z]").matcher(password).find()) {
			return "Password must contain at least one lowercase letter (a-z)!";
		}

		if (!Pattern.compile("[A-Z]").matcher(password).find()) {
			return "Password must contain at least one uppercase letter (A-Z)!";
		}

		return null;

	}

	private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

	public static String checkMail(String mail) {

		if (mail == null || mail.isEmpty()) {
			return "E-Mail is required.";
		}

		if (!Pattern.matches(EMAIL_PATTERN, mail)) {
			return "Please enter a valid mail address.";
		}

		return null;
	}

	public static String checkName(String name) {

		if (name == null || name.isEmpty()) {
			return "The full name is required.";
		}

		return null;
	}

	@Override
	public boolean equals(Object obj) {
		return ((User) obj).getUsername().equals(username);
	}

	public void replaceRole(String oldRole, String newRole) {
		for (int i = 0; i < roles.length; i++) {
			if (roles[i].equalsIgnoreCase(oldRole)) {
				roles[i] = newRole;
				return;
			}
		}
	}
}
