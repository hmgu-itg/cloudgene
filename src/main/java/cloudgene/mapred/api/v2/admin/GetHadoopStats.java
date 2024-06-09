package cloudgene.mapred.api.v2.admin;

import java.util.List;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.HadoopDao;
import cloudgene.mapred.util.BaseResource;
import net.sf.json.JSONArray;

public class GetHadoopStats extends BaseResource {

    /**
     * Resource to get Hadoop statistics
     */
    
    @Get
    public Representation getHadoopStats() {
	User user = getAuthUser();
	String day = getQueryValue("day");
		
	if (user == null) {
	    setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
	    return new StringRepresentation("The request requires user authentication");
	}
	if (!user.isAdmin()) {
	    setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
	    return new StringRepresentation("The request requires administrative rights");
	}

	HadoopDao dao = new HadoopDao(getDatabase());
	// if (day.isEmpty()){
	//     List<String> days=getAllDates();
	//     day=days[days.size()-1]
	// }
	List<Map<String, String>> stats = dao.getData(day);
	JSONArray jsonArray = JSONArray.fromObject(stats);
	return new StringRepresentation(jsonArray.toString());
    }
}
