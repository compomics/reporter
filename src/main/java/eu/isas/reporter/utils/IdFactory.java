package eu.isas.reporter.utils;

/**
 *
 * @author Marc
 */
public class IdFactory {

    private static IdFactory instance = null;

    private IdFactory() {

    }

    public static IdFactory getInstance() {
        if (instance == null) {
            instance = new IdFactory();
        }
        return instance;
    }


}
