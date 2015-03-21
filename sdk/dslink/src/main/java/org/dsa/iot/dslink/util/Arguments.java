package org.dsa.iot.dslink.util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Command line arguments to be parsed.
 * @author Samuel Grenier
 */
public class Arguments {

    @Parameter(names = { "--broker", "-b" },
                description = "Sets the broker host to perform a handshake connection to",
                required = true,
                arity = 1)
    private String broker;

    @Parameter(names = { "--log", "-l" },
                description = "Sets the logging level",
                arity = 1)
    private String log = "info";

    @Parameter(names = { "--nodes", "-n" },
                description = "File path for node serialization and deserialization",
                arity = 1)
    private String nodes = ".nodes.json";

    @Parameter(names = { "--key", "-k" },
                description = "File path for the link public/private key pair")
    private String key = ".key";

    @Parameter(names = { "--help", "-h" },
                description = "Displays the help menu",
                help = true)
    private boolean help;

    /**
     * @return Broker host to perform a handshake connection to.
     */
    public String getBrokerHost() {
        return broker;
    }

    /**
     * @return Global logging level
     */
    public String getLogLevel() {
        return log;
    }

    /**
     * @return Key path
     */
    public String getKeyPath() {
        return key;
    }

    /**
     * @return Nodes path for serialization/deserialization of nodes.
     */
    public String getNodesPath() {
        return nodes;
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
