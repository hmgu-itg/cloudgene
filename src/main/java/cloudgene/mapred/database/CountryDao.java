package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cloudgene.mapred.core.Country;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;

public class CountryDao extends JdbcDataAccessObject {

	private static final Log log = LogFactory.getLog(CountryDao.class);

	public CountryDao(Database database) {
		super(database);
	}

	public boolean insert(Country country) {
		StringBuilder sql = new StringBuilder();
		sql.append(
				"insert into `country` (name, display, allowed) ");
		sql.append("values (?,?,?)");

		try {
			Object[] params = new Object[3];
			params[0] = country.getName();
			params[1] = false;
			params[2] = false;

			int id = insert(sql.toString(), params);

			country.setId(id);

			log.debug("insert country '" + country.getName() + "' successful.");
		} catch (SQLException e) {
			log.error("insert country '" + country.getName() + "' failed.", e);
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public List<Country> findAll() {

		StringBuffer sql = new StringBuffer();

		sql.append("select * ");
		sql.append("from `country` ");
		sql.append("order by name");

		List<Country> result = new Vector<Country>();

		try {
			result = query(sql.toString(), new CountryMapper());

			log.debug("find all country successful. size = " + result.size());

		} catch (SQLException e1) {

			log.error("find all country failed.", e1);

		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<Country> findByQuery(String query) {

		StringBuffer sql = new StringBuffer();

		sql.append("select * ");
		sql.append("from `country` ");
		sql.append("where name like ?");
		sql.append("order by name");

		Object[] params = new Object[1];
		params[0] = "%" + query + "%";

		List<Country> result = new Vector<Country>();
		try {
			result = query(sql.toString(), params, new CountryMapper());
			log.info("find country by query successful. size = " + result.size());

		} catch (SQLException e1) {

			log.error("find country by query failed.", e1);

		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<Country> findAll(int offset, int limit) {

		StringBuffer sql = new StringBuffer();

		sql.append("select * ");
		sql.append("from `country` ");
		sql.append("order by name ");
		sql.append("limit ?,?");

		Object[] params = new Object[2];
		params[0] = offset;
		params[1] = limit;

		List<Country> result = new Vector<Country>();

		try {
			result = query(sql.toString(), params, new CountryMapper());

			log.debug("find all country successful. size = " + result.size());

		} catch (SQLException e1) {

			log.error("find all country failed.", e1);

		}
		return result;
	}

	public static class CountryMapper implements IRowMapper {

		@Override
		public Country mapRow(ResultSet rs, int row) throws SQLException {
			Country country = new Country();
			country.setId(rs.getInt("country.id"));
			country.setName(rs.getString("country.name"));
			country.setDisplay(rs.getBoolean("country.display"));
			country.setAllowed(rs.getBoolean("country.allowed"));
			country.setAlpha2Code(rs.getString("country.alpha2code"));
			country.setAlpha3Code(rs.getString("country.alpha3code"));
			return country;
		}

	}

}
