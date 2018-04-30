package codechicken.aso.api;

/**
 * An aso configuration entry point should implement this class and have name "ASO<someting>Config"
 * loadConfig will only be called when ASO is installed.
 */
public interface IConfigureASO
{
    public void loadConfig();
    
    public String getName();
    public String getVersion();
}
