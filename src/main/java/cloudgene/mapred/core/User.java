package cloudgene.mapred.core;

import java.util.regex.Pattern;

public class User {

	private String username;

	private String password;

	private int id;

	private String fullName = "";

	private String mail;

	private String role;

	private boolean active = true;

	private String activationKey = null;

	private String apiToken = "";

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

	public void setRole(String role) {
		this.role = role;
	}

	public String getRole() {
		return role;
	}

	public boolean isAdmin() {
		if (role != null) {
			return role.equalsIgnoreCase("admin");
		} else {
			return false;
		}
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
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

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

}
