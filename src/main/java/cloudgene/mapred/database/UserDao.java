package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import cloudgene.mapred.util.PublicUser;

public class UserDao extends JdbcDataAccessObject {

	private static final Log log = LogFactory.getLog(UserDao.class);

	public UserDao(Database database) {
		super(database);
	}

	public boolean insert(User user) {
		StringBuilder sql = new StringBuilder();
		// max_running has default value=2
		sql.append("insert into `user` (username, password, full_name, aws_key, aws_secret_key, save_keys, export_to_s3, s3_bucket, mail, role, export_input_to_s3, activation_code, active, api_token, last_login, locked_until, login_attempts, institute_email, institute_name, institute_address1, institute_address2, institute_city, institute_postcode, institute_country, accepted_t_c, accepted_eu_eea, api_token_expires_on)");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		try {
			Object[] params = new Object[27];
			params[0] = user.getUsername().toLowerCase();
			params[1] = user.getPassword();
			params[2] = user.getFullName();
			params[3] = null;
			params[4] = null;
			params[5] = false;
			params[6] = false;
			params[7] = null;
			params[8] = user.getMail();
			params[9] = String.join(User.ROLE_SEPARATOR, user.getRoles());
			params[10] = false;
			params[11] = user.getActivationCode();
			params[12] = user.isActive();
			params[13] = user.getApiToken();
			params[14] = user.getLastLogin();
			params[15] = user.getLockedUntil();
			params[16] = user.getLoginAttempts();

			params[17] = user.getInstituteEmail();
			params[18] = user.getInstituteName();
			params[19] = user.getInstituteAddress1();
			params[20] = user.getInstituteAddress2();
			params[21] = user.getInstituteCity();
			params[22] = user.getInstitutePostCode();
			params[23] = user.getInstituteCountry();

			params[24] = user.getAcceptedTandC();
			params[25] = user.getAcceptedCountry();
			params[26] = user.getApiTokenExpiresOn();

			int id = insert(sql.toString(), params);

			user.setId(id);

			log.debug("insert user '" + user.getUsername() + "' successful.");

		} catch (SQLException e) {
			log.error("insert user '" + user.getUsername() + "' failed.", e);
			return false;
		}

		return true;
	}

    // max_running is not affected here
	public boolean update(User user) {
		StringBuilder sql = new StringBuilder();
		sql.append(
				"update `user` set username = ?, password = ?, full_name = ?, aws_key = ?, aws_secret_key = ?, save_keys = ? , export_to_s3 = ?, s3_bucket = ?, mail = ?, role = ?, export_input_to_s3 = ?, active = ?, activation_code = ?, api_token = ?, last_login = ?, locked_until = ?, login_attempts = ?, api_token_expires_on = ? ");
		sql.append("where id = ?");

		try {
			// TODO: Update so that institute info can also be updated 
			Object[] params = new Object[19];
			params[0] = user.getUsername().toLowerCase();
			params[1] = user.getPassword();
			params[2] = user.getFullName();
			params[3] = null;
			params[4] = null;
			params[5] = false;
			params[6] = false;
			params[7] = null;
			params[8] = user.getMail();
			params[9] = String.join(User.ROLE_SEPARATOR, user.getRoles());
			params[10] = false;
			params[11] = user.isActive();
			params[12] = user.getActivationCode();
			params[13] = user.getApiToken();
			params[14] = user.getLastLogin();
			params[15] = user.getLockedUntil();
			params[16] = user.getLoginAttempts();
			params[17] = user.getApiTokenExpiresOn();
			params[18] = user.getId();

			update(sql.toString(), params);

			log.debug("update user '" + user.getUsername() + "' successful.");

		} catch (SQLException e) {
			log.error("update user '" + user.getUsername() + "' failed.", e);
			return false;
		}

		return true;
	}

    // sets max_running for all users
    public boolean setMaxRunningJobs(int n){
	StringBuilder sql = new StringBuilder();
	sql.append("update `user` set max_running = ?");

	try {
	    Object[] params = new Object[1];
	    params[0] = n;
	    update(sql.toString(), params);
	    log.debug("successfully set max_running to "+n);
	} catch (SQLException e) {
	    log.error("setting max_running to "+n+" failed", e);
	    return false;
	}

	return true;
    }

	public User findByUsername(String user) {

		StringBuffer sql = new StringBuffer();

		sql.append("select * ");
		sql.append("from `user` ");
		sql.append("where username = ?");

		Object[] params = new Object[1];
		params[0] = user.toLowerCase();

		User result = null;

		try {

			result = (User) queryForObject(sql.toString(), params, new UserMapper());

			log.debug("find user by username '" + user + "' successful.");

		} catch (SQLException e1) {

			log.error("find user by username " + user + "' failed.", e1);

		}
		return result;
	}

	public User findByMail(String mail) {

		StringBuffer sql = new StringBuffer();

		sql.append("select * ");
		sql.append("from `user` ");
		sql.append("where mail = ?");

		Object[] params = new Object[1];
		params[0] = mail.toLowerCase();

		User result = null;

		try {
			result = (User) queryForObject(sql.toString(), params, new UserMapper());

			log.debug("find user by mail '" + mail + "' successful.");

		} catch (SQLException e1) {

			log.error("find user by mail " + mail + "' failed.", e1);

		}
		return result;
	}

	public User findById(int id) {

		StringBuffer sql = new StringBuffer();

		sql.append("select * ");
		sql.append("from `user` ");
		sql.append("where id = ?");

		Object[] params = new Object[1];
		params[0] = id;

		User result = null;

		try {

			result = (User) queryForObject(sql.toString(), params, new UserMapper());

			log.debug("find user by id '" + id + "' successful.");

		} catch (SQLException e1) {

			log.error("find user by id failed.", e1);

		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<User> findAll() {

		StringBuffer sql = new StringBuffer();

		sql.append("select * ");
		sql.append("from `user` ");
		sql.append("order by username");

		List<User> result = new Vector<User>();

		try {
			result = query(sql.toString(), new UserMapper());

			log.debug("find all user successful. size = " + result.size());

		} catch (SQLException e1) {

			log.error("find all user failed.", e1);

		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public int countAll() {

		StringBuilder sql = new StringBuilder();
		sql.append("select count(*) ");
		sql.append("from `user` ");

		int result = 0;

		try {
			result = (Integer) queryForObject(sql.toString(), new IntegerMapper());
			log.debug("count all users successful. results: " + result);

			return result;
		} catch (SQLException e) {
			log.error("count all users failed", e);
			return 0;
		}
	}


	@SuppressWarnings("unchecked")
	public List<User> findByQuery(String query) {

		StringBuffer sql = new StringBuffer();

		sql.append("select * ");
		sql.append("from `user` ");
		sql.append("where mail like ? or username like ? or full_name like ? ");
		sql.append("order by username");

		Object[] params = new Object[3];
		params[0] = "%" + query + "%";
		params[1] = params[0];
		params[2] = params[0];
		
		List<User> result = new Vector<User>();

		try {
			result = query(sql.toString(), params, new UserMapper());

			log.debug("find all user successful. size = " + result.size());

		} catch (SQLException e1) {

			log.error("find all user failed.", e1);

		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<User> findAll(int offset, int limit) {

		StringBuffer sql = new StringBuffer();

		sql.append("select * ");
		sql.append("from `user` ");
		sql.append("order by username ");
		sql.append("limit ?,?");

		Object[] params = new Object[2];
		params[0] = offset;
		params[1] = limit;

		List<User> result = new Vector<User>();

		try {
			result = query(sql.toString(), params, new UserMapper());

			log.debug("find all user successful. size = " + result.size());

		} catch (SQLException e1) {

			log.error("find all user failed.", e1);

		}
		return result;
	}

    /* all unique roles from 'user' table, over all users */
	@SuppressWarnings("unchecked")
	public Set<String> findAllRoles() {
	    List<User> users=findAll();
	    Set<String> roles=new HashSet<String>();
	    for (User user:users){
		for (String role:user.getRoles()){
		    roles.add(role.toLowerCase());
		}
	    }
	    return roles;
	}

	public boolean delete(User user) {
		// update all older jobs
		User publicUser = PublicUser.getUser(database);

		JobDao jobDao = new JobDao(database);
		boolean result = jobDao.updateUser(user, publicUser);
		if (!result) {
			log.error("delete user failed");
			return false;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("delete from `user` ");
		sql.append("where id = ? ");
		try {

			Object[] params = new Object[1];
			params[0] = user.getId();

			update(sql.toString(), params);

			log.debug("delete user successful.");

		} catch (SQLException e) {
			log.error("delete user failed", e);
			return false;
		}

		return true;
	}

	public static class UserMapper implements IRowMapper {

		@Override
		public User mapRow(ResultSet rs, int row) throws SQLException {
			User user = new User();
			user.setId(rs.getInt("user.id"));
			user.setMaxRunningJobs(rs.getInt("user.max_running"));
			user.setUsername(rs.getString("user.username"));
			user.setPassword(rs.getString("user.password"));
			user.setFullName(rs.getString("user.full_name"));
			user.setMail(rs.getString("user.mail"));
			if (rs.getString("user.role") != null) {
				user.setRoles(rs.getString("user.role").split(User.ROLE_SEPARATOR));
			} else {
				user.setRoles(new String[0]);
			}
			user.setActivationCode(rs.getString("user.activation_code"));
			user.setActive(rs.getBoolean("user.active"));
			user.setApiToken(rs.getString("user.api_token"));
			user.setApiTokenExpiresOn(rs.getTimestamp("user.api_token_expires_on"));
			user.setLastLogin(rs.getTimestamp("user.last_login"));
			user.setLockedUntil(rs.getTimestamp("user.locked_until"));
			user.setLoginAttempts(rs.getInt("user.login_attempts"));
			return user;
		}

	}

}
