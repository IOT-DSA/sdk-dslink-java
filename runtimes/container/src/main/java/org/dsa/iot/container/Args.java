package org.dsa.iot.container;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

/**
 * @author Samuel Grenier
 */
@Parameters(separators = "= ")
public class Args {

    @Parameter(names = { "--dslinks", "-d" },
                description = "DSLinks folder to run as a standalone",
                arity = 1)
    private String dslinksFolder;

    @Parameter(names = { "--broker", "-b"},
                description = "Broker URL to connect to, only works in standalone mode",
                arity = 1)
    private String brokerUrl;

    @Parameter(names = { "--token", "-t" },
            description = "Sets the token used when connecting to the broker",
            arity = 1)
    private String token;

    @Parameter(names = { "--help", "-h" },
            description = "Displays the help menu",
            help = true)
    private boolean help = false;

    public String getDslinksFolder() {
        return dslinksFolder;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getToken() {
        return token;
    }

    public static Args parse(String[] args) {
        try {
            Args parsed = new Args();
            JCommander jc = new JCommander(parsed, args);
            jc.setProgramName("<container>");
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
