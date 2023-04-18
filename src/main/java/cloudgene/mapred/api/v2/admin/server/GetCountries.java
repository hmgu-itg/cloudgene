package cloudgene.mapred.api.v2.admin.server;

import java.util.List;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import cloudgene.mapred.core.User;
import cloudgene.mapred.core.Country;
import cloudgene.mapred.database.CountryDao;
import cloudgene.mapred.util.BaseResource;
import cloudgene.mapred.util.PageUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class GetCountries extends BaseResource {
    
    public static final int DEFAULT_PAGE_SIZE = 100;
    
    @Get
    public Representation get() {
		User user = getAuthUser();

		if (user == null) {
			setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new StringRepresentation("The request requires user authentication.");
		}

		if (!user.isAdmin()) {
			setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return new StringRepresentation("The request requires administration rights.");
		}

		String page = getQueryValue("page");
		int pageSize = DEFAULT_PAGE_SIZE;

		int offset = 0;
		if (page != null) {

			offset = Integer.valueOf(page);
			if (offset < 1) {
				offset = 1;
			}
			offset = (offset - 1) * pageSize;
		}

		CountryDao dao = new CountryDao(getDatabase());

		List<Country> countries = null;
		int count = 0;
		String query = getQueryValue("query");
		if (query != null && !query.isEmpty()) {
			countries = dao.findByQuery(query);
			page = "1";
			count = countries.size();
			pageSize = count;
		} else {
			if (page != null) {
				countries = dao.findAll(offset, pageSize);
				count = dao.findAll().size();
			} else {
				countries = dao.findAll();
				page = "1";
				count = countries.size();
				pageSize = count;
			}
		}

		JSONArray jsonArray = new JSONArray();
        for (Country country : countries) {
            JSONObject object = new JSONObject();
            object.put("name", country.getName());
            object.put("display", country.getDisplay());
            object.put("allowed", country.getAllowed());
            jsonArray.add(object);
        }

		JSONObject object = PageUtil.createPageObject(Integer.parseInt(page), pageSize, count);
		object.put("data", jsonArray);

		return new StringRepresentation(object.toString());
    }
}
