package org.dsa.iot.dslink.config;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Command line arguments to be parsed.
 *
 * @author Samuel Grenier
 */
public class Arguments {

    @Parameter(names = { "--broker", "-b" },
                description = "Sets the broker host to perform a handshake connection to",
                arity = 1)
    private String broker;

    @Parameter(names = { "--dslink-json", "-d" },
                description = "Sets the location of the dslink.json file",
                arity = 1)
    private String dslinkJson = "dslink.json";

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
     * Overrides the broker host configured in the dslink.json file
     *
     * @return Broker host
     */
    public String getBrokerHost() {
        return broker;
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
