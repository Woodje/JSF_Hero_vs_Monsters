package dk.JSF_Hero_vs_Monster.main;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.util.Scanner;

/**
 * UserInterface.java - Used for displaying the game and retrieving inputs.
 * @author Simon Jon Pedersen
 * @author Kristoffer Broch Møller
 * @version 1.0 05/02-2015.
 */

public class UserInterface {

    /** Enumerations used for representing the different kind of menu's that is needed. */
    public enum menu { FIRST, SHOWMAP, SELECTMAP, MOVEMENT, COMBAT };

    /**
     * Print the specified menu type to the screen and return the inputs given for this menu.
     * @param menuType - This is what type of menu to use.
     * @param additionalString - This is a string that can be used for creating a little more custom menu.
     */
    public String loadMenu(menu menuType, String additionalString) {

        String menuString = "";

        switch (menuType) {

            case FIRST: menuString = "  1 - Start game\n  2 - Show maps\n  3 - Exit game\n\n  ";
                break;

            case SHOWMAP:   menuString = "  0 - Exit menu\n" + additionalString;
                break;

            case SELECTMAP: menuString = additionalString;
                break;

            case MOVEMENT:  menuString = additionalString + "[w]UP [s]DOWN [a]LEFT [d]RIGHT\n  ";
                break;

            case COMBAT:  menuString = "  What skill to use? " + additionalString;
                break;

        }

        return menuString;

    }

}