package cloudgene.mapred.api.v2.users;

import java.util.List;

import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import cloudgene.mapred.util.BaseResource;

import cloudgene.mapred.core.Country;
import cloudgene.mapred.database.CountryDao;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class GetAllowedCountries extends BaseResource {

    @Get
    public Representation get() {

        CountryDao dao = new CountryDao(getDatabase());

        List<Country> countries = dao.findAll();

        JSONArray jsonArray = new JSONArray();
        for (Country country : countries) {
            if (country.getDisplay() == true) {
                JSONObject object = new JSONObject();
                object.put("name", country.getName());
                jsonArray.add(object);
            }
        }
        JSONObject object = new JSONObject();
        object.put("data", jsonArray);

        return new StringRepresentation(object.toString());
    };
}
