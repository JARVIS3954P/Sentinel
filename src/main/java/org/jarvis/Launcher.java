package org.jarvis;

/**
 * This is a launcher "shim" class. Its sole purpose is to be the main entry
 * point for the shaded JAR. It does not extend Application, which avoids the
 * JavaFX module initialization issues with the java -jar command.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}