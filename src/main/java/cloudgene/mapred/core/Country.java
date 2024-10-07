package cloudgene.mapred.core;

public class Country {

    private int id;
    private String name;
    private boolean display;
    private boolean allowed;
    private String alpha2code;
    private String alpha3code;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public boolean getAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getAlpha2Code() {
        return alpha2code;
    }

    public void setAlpha2Code(String alpha2code) {
        this.alpha2code = alpha2code;
    }

    public String getAlpha3Code() {
        return alpha3code;
    }

    public void setAlpha3Code(String alpha3code) {
        this.alpha3code = alpha3code;
    }

}