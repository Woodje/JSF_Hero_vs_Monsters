package dk.JSF_Hero_vs_Monster.test;

import dk.JSF_Hero_vs_Monster.main.UserInterface;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import dk.JSF_Hero_vs_Monster.main.UserInterface.menu;

import static org.junit.Assert.*;

public class UserInterfaceTest {

    @Test
    public void testLoadMenu() throws Exception {

        UserInterface userInterface = new UserInterface();

        String input = userInterface.loadMenu(menu.FIRST, "");

        assertEquals("Must be the same value as the first menu.",  "  1 - Start game\n" +
                                                                    "  2 - Show maps\n" +
                                                                    "  3 - Exit game\n" +
                                                                    "\n" +
                                                                    "  ", input);

    }

}