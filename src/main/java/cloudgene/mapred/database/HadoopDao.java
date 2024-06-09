package cloudgene.mapred.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.JdbcDataAccessObject;

public class HadoopDao extends JdbcDataAccessObject {
    private static final Log log = LogFactory.getLog(HadoopDao.class);
    public HadoopDao(Database database) {
	super(database);
    }

    public List<String> getAllDates(){
	StringBuilder sql = new StringBuilder();
	String stmt="SELECT DISTINCT(CAST(T AS DATE)) AS D FROM hadoop_stats ORDER BY D";
	sql.append(stmt);
	log.debug(stmt);

	List<String> res=new ArrayList <String>();
	try {
	    Connection connection = database.getDataSource().getConnection();
	    PreparedStatement statement = connection.prepareStatement(sql.toString());
	    ResultSet rs = statement.executeQuery();
	    while (rs.next()) {
		res.add(rs.getString(1));
	    }
	    rs.close();
	    connection.close();
	    log.debug("getAllDays successful. Results: "+ res.size());
	    return res;
	} catch (SQLException e) {
	    log.error("getAllDays failed", e);
	}

	return res;
    }
    
    public List<Map<String, String>> getData(String day) {
	StringBuilder sql = new StringBuilder();
	if (day.isEmpty()){
	    sql.append("SELECT * FROM hadoop_stats");
	}
	else{
	    String [] s=day.split("-");
	    String month=s[1].replaceFirst ("^0*","");
	    String d=s[2].replaceFirst ("^0*","");
	    String stmt="SELECT * FROM hadoop_stats WHERE YEAR(T)="+s[0]+" AND MONTH(T)="+month+" AND DAY(T)="+d;
	    sql.append(stmt);
	    log.debug(stmt);
	}

	List<Map<String, String>> result = new Vector<Map<String, String>>();

	try {
	    Connection connection = database.getDataSource().getConnection();
	    PreparedStatement statement = connection.prepareStatement(sql.toString());
	    ResultSet rs = statement.executeQuery();
	    ResultSetMetaData md = rs.getMetaData();
	    int columns = md.getColumnCount();
	    while (rs.next()) {
		HashMap<String,String> row = new HashMap<String,String>();
		for(int i=1; i<=columns; ++i){           
		    row.put(md.getColumnName(i),rs.getString(i));
		}
		result.add(row);
	    }
	    rs.close();
	    connection.close();
	    log.debug("Hadoop query successful. Results: "+ result.size());
	    return result;
	} catch (SQLException e) {
	    log.error("Hadoop query failed", e);
	}

	return result;
    }
}
