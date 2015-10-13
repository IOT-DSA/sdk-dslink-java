package org.dsa.iot.dslink.config;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

/**
 * Command line arguments to be parsed.
 *
 * @author Samuel Grenier
 */
@Parameters(separators = "= ")
public class Arguments {

    @Parameter(names = { "--broker", "-b" },
                description = "Sets the broker host to perform a handshake connection to",
                arity = 1,
                required = true)
    private String broker;

    @Parameter(names = { "--token", "-t" },
                description = "Sets the token used when connecting to the broker",
                arity = 1)
    private String token;

    @Parameter(names = { "--nodes", "-n" },
                description = "Sets the path for the serialized nodes",
                arity = 1)
    private String nodes;

    @Parameter(names = { "--key", "-k" },
                description = "Sets the path for the stored key",
                arity = 1)
    private String key;

    @Parameter(names = { "--log", "-l"},
                description = "Sets the log level",
                arity = 1)
    private String log;

    @Parameter(names = { "--dslink-json", "-d" },
                description = "Sets the location of the dslink.json file",
                arity = 1)
    private String dslinkJson = "dslink.json";

    @Parameter(names = { "--name" },
                description = "Sets the name of the dslink",
                arity = 1)
    private String name;

    @Parameter(names = { "--help", "-h" },
                description = "Displays the help menu",
                help = true)
    private boolean help = false;

    /**
     * Gets the Dslink JSON file location used to initialize
     * the configurations of the link.
     *
     * @return Dslink JSON location.
     */
    public String getDslinkJson() {
        return dslinkJson;
    }

    /**
     * Overrides the broker host configured in the dslink.json file.
     *
     * @return Broker host.
     */
    public String getBrokerHost() {
        return broker;
    }

    /**
     * The token used when connecting to the broker.
     *
     * @return Token
     */
    public String getToken() {
        return token;
    }

    /**
     * Overrides the nodes path configured in the dslink.json file.
     *
     * @return Nodes path.
     */
    public String getNodesPath() {
        return nodes;
    }

    /**
     * Overrides the key path configured in the dslink.json file.
     *
     * @return Key path.
     */
    public String getKeyPath() {
        return key;
    }

    /**
     * Overrides the log level configured in the dslink.json file.
     *
     * @return Log level.
     */
    public String getLogLevel() {
        return log;
    }

    /**
     * Overrides the name configured in the dslink.json file.
     *
     * @return Name of the DSLink.
     */
    public String getName() {
        return name;
    }

    /**
     * Parses the arguments.
     * @param args Arguments to parse
     * @return Populated arguments array
     */
    public static Arguments parse(String[] args) {
        try {
            Arguments parsed = new Arguments();
            JCommander jc = new JCommander(parsed, args);
            jc.setProgramName("<dslink>");
            if (parsed.help) {
                jc.usage();
                return null;
            }
            return parsed;
        } catch (ParameterException pe) {
            System.out.println("Use --help or -h to get usage help");
            System.out.println(pe.getMessage());
            return null;
        }
    }
}
