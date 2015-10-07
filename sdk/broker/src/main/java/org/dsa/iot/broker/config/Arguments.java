package org.dsa.iot.broker.config;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

/**
 * @author Samuel Grenier
 */
@Parameters(separators = "= ")
public class Arguments {

    private JCommander jc;

    @Parameter(names = { "--server", "-s" },
               description = "Starts the broker server")
    private boolean server = false;

    @Parameter(names = { "--log", "-l"},
            description = "Sets the log level",
            arity = 1)
    private String log = "info";

    @Parameter(names = { "--help", "-h" },
            description = "Displays the help menu",
            help = true)
    private boolean help = false;

    public boolean runServer() {
        return server;
    }

    public String getLogLevel() {
        return log;
    }

    /**
     * Parses the arguments.
     *
     * @param args Arguments to parse.
     * @return Whether parsing was successful or not.
     */
    public boolean parse(String[] args) {
        try {
            jc = new JCommander(this, args);
            jc.setProgramName("<broker>");
            if (help) {
                jc.usage();
                return false;
            }
            return true;
        } catch (ParameterException pe) {
            System.out.println("Use --help or -h to get usage help");
            System.out.println(pe.getMessage());
        }
        return false;
    }

    public void displayHelp() {
        jc.usage();
    }
}
