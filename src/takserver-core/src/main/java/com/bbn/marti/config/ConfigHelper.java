package com.bbn.marti.config;

import java.util.HashSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class used to do miscellaneous tasks to the generated Configuration file such as load default objects or check
 * content values
 * <p>
 * Created on 8/31/15.
 */
public class ConfigHelper {

    public final static String DEFAULT_SECURITY = "TLSv1.2";
    public final static boolean DEFAULT_SSL_ALLOW_128_CIPHER = true;
    public final static boolean DEFAULT_SSL_ALLOW_NON_SUITE_B = true;

    /**
     * Runs validation against some configuration scenarios that we know are invalid
     *
     * @return An error string if there is a problem, null if there are no problems
     */
    @SuppressWarnings("incomplete-switch")
	@Nullable
    public static String validateConfiguration(@NotNull Configuration configuration) throws InvalidConfigurationException {

        Auth auth = configuration.getAuth();
        if (auth != null) {
            boolean hasLdap = (auth.getLdap() != null);
            boolean hasFile = (auth.getFile() != null);

            HashSet<String> inputNameSet = new HashSet<>();

            List<Input> inputList = configuration.getNetwork().getInput();
            for (Input input : inputList) {
                // Using switch since the compiler will complain if another auth is added but not checked for here

                if (inputNameSet.contains(input.getName())) {
                    throw new InvalidConfigurationException("Multiple inputs with the name \"" + input.getName() + "\" provided! All input names must be unique or message routing will be impacted!");
                } else {
                    inputNameSet.add(input.getName());
                }

                List<String> inputGroups = input.getFiltergroup();

                switch (input.getAuth()) {
                    case LDAP:
                        if (!hasLdap) {
                            throw new InvalidConfigurationException("Input configured to use LDAP auth even though no LDAP auth is configured!");
                        } else if (!inputGroups.isEmpty()) {
                            throw new InvalidConfigurationException("Input configured to use LDAP auth even though groups are specified for the input!");
                        }
                        break;

                    case FILE:
                        if (!hasFile) {
                            throw new InvalidConfigurationException("Input configured to use FILE auth even though no FILE auth is configured!");
                        } else if (!inputGroups.isEmpty()) {
                            throw new InvalidConfigurationException("Input configured to use FILE auth even though groups are specified for the input!");
                        }
                        break;

                    case ANONYMOUS:
                        break;
                }
            }
            return null;
        }
        return null;
    }
}
