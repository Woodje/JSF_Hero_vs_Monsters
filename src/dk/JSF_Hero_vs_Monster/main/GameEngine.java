package dk.JSF_Hero_vs_Monster.main;

import javax.faces.bean.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * GameEngine - Used for controlling the basic logic of the game.
 * @author Simon Jon Pedersen
 * @author Kristoffer Broch Møller
 * @version 2.0 19/02-2015.
 */

@ManagedBean(name = "GameEngine")
@ViewScoped
public class GameEngine {

    /** This is the list used for storing all characters in the game. */
    private ArrayList<Character> characters;

    /** This is used for map operations. */
    private Map map;

    /** This is used for user interfacing. */
    private UserInterface userInterface;

    /** This is used for the communication with the database. */
    private GameDatabase gameDatabase;

    /** Enumerations used for stating the different kinds of states that the game can be in. */
    private enum state { INITIALIZE, LISTMAPSHOWONLY, LISTMAP, STARTGAME, MAPERROR, GAMELOOP, COMBATSCENE, ENDCOMBATSCENE };

    /** This is used for handling the games states. */
    private state gameState;

    /** Booleans needed for controlling some of the games logic. */
    private boolean firstTimeListMaps, firstTimeCombatScene = true, resetHero, restartGame;

    /** This is used for holding the users key codes*/
    private int keyCode;

    /** These strings represents the users input and the output that is presented for the user */
    private String userInput, outputString;

    /**
     * Constructor.
     * Instantiating of: userInterface, map, characters and the starting value of the output string.
     */
    public GameEngine() {

        gameDatabase = new GameDatabase();

        userInterface = new UserInterface();

        map = new Map();

        characters = new ArrayList<Character>();

        gameState = state.INITIALIZE;

        outputString = "  Welcome\n  --------------\n" + userInterface.loadMenu(UserInterface.menu.FIRST, "");

    }

    /** Convert the current keycode to a string and determines what function to call based on the games state. */
    public void updateUserInput() {

        if (keyCode >= 96 && keyCode <= 105 )
            keyCode -= 48;

        userInput = String.valueOf((char) keyCode);

        switch (gameState) {

            case INITIALIZE: initializeGame();
                 break;
            case LISTMAPSHOWONLY: listMaps(true);
                 break;
            case LISTMAP: listMaps(false);
                 break;
            case STARTGAME: startGame();
                 break;
            case MAPERROR: listMaps(false);
                 break;
            case GAMELOOP: gameLoop();
                 break;
            case COMBATSCENE: gameLoop();
                 break;

        }

    }

    /** Represent the user for the first menu and reacts on the defined inputs. */
    public void initializeGame() {

        if (restartGame)
            userInput = "0";
        else if (resetHero)
            userInput = "1";

        restartGame = false;

        gameState = state.INITIALIZE;

        firstTimeListMaps = true;

        gameDatabase.createTables();

        switch (convertToInteger(userInput.substring(userInput.length() - 1))) {

            case 1:  startGame();
                     break;

            case 2:  listMaps(true);
                     break;

            case 3:  exitGame();
                     break;

            case -1: outputString = "  Welcome\n  --------------\n" + userInterface.loadMenu(UserInterface.menu.FIRST, "");
                     break;

            default: outputString = "  Welcome\n  --------------\n" + userInterface.loadMenu(UserInterface.menu.FIRST, "");

        }

    }

    /**
     * Starts up the necessary precautions before starting the games loop.
     * Minimum one hero and one monster is created.
     * The games loop will be started when characters and a map is loaded.
     */
    private void startGame() {

        if (gameState == state.INITIALIZE && gameDatabase.getHero("HERO") == null || gameState == state.INITIALIZE && resetHero)
            listMaps(false);

        if (gameState != state.LISTMAP && gameState != state.MAPERROR) {

            createCharacter(true);

            if (gameState != state.MAPERROR)
                createCharacter(false);

        }

        if (gameState == state.MAPERROR)
            outputString += "\n  Select a new Map\n  -------------\n" + userInterface.loadMenu(UserInterface.menu.SHOWMAP, map.getMaps());

        if (characters.size() > 1) {

            outputString = "  HERO loaded for map: " + map.getMapFileName();

            outputString += "\n\n  Press any key to continue...";

            gameState = state.GAMELOOP;

            resetHero = false;

        }

    }

    /**
     *  This is the games loop from where the actual basic logic of the game takes place.
     *  Step 1: A user is prompted for an input for where to go.
     *  Step 2: If no fight has occurred, then move the monsters.
     *  Step 3: If a fight has occurred, then enter a combat scene.
     *  Step 4: If a monster won the combat, the send the hero back to his previous location.
     *  Step 5: If the monster lost, then delete him from the game, reward the hero with experience and update the database.
     *  Step 6: If there are no more monster left in the map, then spawn as many monsters as the hero's level. (Max 5)
     *  Step 7: Start over by prompting for an input for where to go.
     *  The output string is updated for each run through.
     */
    private void gameLoop() {

        boolean isInCombat = false;

        outputString = getStats() + map.getMap() + userInterface.loadMenu(UserInterface.menu.MOVEMENT, "Where to go?  ");

        processUserInput(userInput.substring(userInput.length() - 1));

        Character[] charactersFighting = getCharactersFighting();

        if (charactersFighting == null)
            moveMonsters();

        outputString = getStats() + map.getMap() + userInterface.loadMenu(UserInterface.menu.MOVEMENT, "Where to go?  ");

        charactersFighting = getCharactersFighting();

        if (charactersFighting != null) {

            CombatScene combatScene = new CombatScene(charactersFighting[0], charactersFighting[1]);

            outputString = getStats() + map.getMap() + "You have entered combat, press any key to start the fight!";

            if (combatScene.getWinner() == null && gameState == state.COMBATSCENE) {

                isInCombat = true;

                if (charactersFighting[0].getHealth() == charactersFighting[0].getMaxHealth())
                    outputString = combatScene.getCombatScene() + combatScene.getTextures(0);
                else
                    outputString = combatScene.getCombatScene() + combatScene.getTextures(3);

                outputString += userInterface.loadMenu(UserInterface.menu.COMBAT, "");

                if (!firstTimeCombatScene) {

                    String result = combatScene.attackWithSkill(userInput.substring(userInput.length() - 1), true);

                    if (combatScene.getWinner() == null) {

                        result += combatScene.attackWithSkill("1", false);

                        if (combatScene.getWinner() != null) {

                            result += "\n  " + combatScene.getWinner().getName() + " wins the fight!\n";

                            outputString = combatScene.getCombatScene() + combatScene.getTextures(2);

                            gameState = state.ENDCOMBATSCENE;

                        } else
                            outputString = combatScene.getCombatScene() + combatScene.getTextures(3);

                    } else {

                        outputString = combatScene.getCombatScene() + combatScene.getTextures(1);

                        result +=   "\n  " + combatScene.getWinner().getName() + " wins the fight!\n" +
                                    "\n  You gained " + (combatScene.getWinner().getHealth() * 5) + " experience!\n";

                        gameState = state.ENDCOMBATSCENE;

                    }

                    outputString += result + "\n  Press any key to continue...";

                }

                firstTimeCombatScene = false;

            }

            if (combatScene.getWinner() != null) {

                Character winner = combatScene.getWinner(), loser = combatScene.getLoser();

                if (winner instanceof Hero) {

                    ((Hero) winner).setExperience((((Hero) winner).getExperience() + winner.getHealth() * 5));

                    winner.setTexture(map.heroTexture);

                    winner.setHealth(winner.getMaxHealth());

                    characters.remove(loser);

                    gameDatabase.setHero((Hero) winner);

                    for (Character character : characters)
                        if (!(character instanceof Hero))
                            character.setLevel(winner.getLevel());

                } else {

                    winner.setTexture(map.monsterTexture);

                    winner.setHealth(winner.getMaxHealth());

                    loser.setHealth(loser.getMaxHealth());

                    loser.setTexture(map.heroTexture);

                    loser.setLocation(loser.getPreviousLocation());

                    map.setTextureLocation(loser.getTexture(), loser.getLocation());

                }

                map.setTextureLocation(winner.getTexture(), winner.getLocation());

                if (characters.size() == 1)
                    spawnExtraMonsters(winner.getLevel(), winner.getLevel());

            }

            if (gameState == state.ENDCOMBATSCENE) {

                gameState = state.GAMELOOP;

                firstTimeCombatScene = true;

            } else
                gameState = state.COMBATSCENE;

        }

        if (!isInCombat) {

            for (int i = 0; i < 2; i++)
                outputString = outputString.replaceAll(map.monsterTexture[i], "<span id=\"monsterTexture\">" + map.monsterTexture[i] + "</span>");

            outputString = outputString.replaceAll("_/ \\\\_", "<span id=\"monsterTexture\">_/ \\\\_</span>");

            for (int i = 0; i < 2; i++)
                outputString = outputString.replaceAll(map.heroTexture[i], "<span id=\"heroTexture\">" + map.heroTexture[i] + "</span>");

            outputString = outputString.replaceAll(" / \\\\ ", "<span id=\"heroTexture\">" + " / \\\\ " + "</span>");

            outputString = outputString.replaceAll(map.wallTexture[0], "<span id=\"wallTexture\">=====</span>");

            outputString = outputString.replaceAll(map.floorTexture[0], "<span id=\"floorTexture\">     </span>");

            outputString = outputString.replaceAll("\\" + map.fightTexture[0], "<span id=\"fightTexture\">" + "\\" + map.fightTexture[0] + "</span>");

            outputString = outputString.replaceAll(map.fightTexture[1], "<span id=\"fightTexture\">" + map.fightTexture[1] + "</span>");

            outputString = outputString.replaceAll(map.fightTexture[2] + "\\", "<span id=\"fightTexture\">" + map.fightTexture[2] + "\\" + "</span>");

        }
        else
            outputString = outputString.replaceAll("\\$", "<span id=\"combatScene\">\\$</span>");

        if (gameState == state.ENDCOMBATSCENE)
            isInCombat = false;

    }

    /**
     * Return a Character array containing the two fighting characters.
     * If nothing is found, null is returned.
     * The first character in the returned Character array represents the hero character.
     */
    private Character[] getCharactersFighting() {

        Character[] charactersFighting = new Character[2];

        int x = 0;

        for (int i = 0; i < characters.size(); i++)
            if (characters.get(i).getTexture() == map.fightTexture) {

                charactersFighting[x] = characters.get(i);

                x++;

            }

        if (x < 1)
            return null;
        else {

            if (charactersFighting[0] instanceof Monster) {

                Character monster = charactersFighting[0];

                charactersFighting[0] = charactersFighting[1];

                charactersFighting[1] = monster;

            }

        }

        return charactersFighting;

    }

    /** Return a string representing the stats for each character in the game. */
    public String getStats() {

        String statsIndicator = "";

        for (Character character : characters)
            if (character instanceof Hero) {

                statsIndicator +=   "  (" + character.getName() + ") - " +
                        "Level: " + character.getLevel() + " - " +
                        "Health: " + character.getHealth() + "/" + character.getMaxHealth() + " - " +
                        "XP: " + ((Hero) character).getExperience() + "/" + ((Hero) character).getMaxExperience() + "\n";
            }
            else {

                statsIndicator +=   "  (" + character.getName() + ") - " +
                        "Level: " + character.getLevel() + " - " +
                        "Health: " + character.getHealth() + "/" + character.getMaxHealth() + "\n";
            }

        return statsIndicator;

    }

    /**
     * Do the appropriate hero movement according to the users input.
     * @param input - This should be the the users input.
     */
    private void processUserInput(String input) {

        if (input.toCharArray().length == 1) {

            switch (input.toCharArray()[0]) {

                case 'W':   for (Character hero : characters)
                                if (hero instanceof Hero)
                                    moveCharacter(hero, new Point(0, -1));

                            break;

                case 'S':   for (Character hero : characters)
                                if (hero instanceof Hero)
                                    moveCharacter(hero, new Point(0, 1));

                            break;

                case 'A':   for (Character hero : characters)
                                if (hero instanceof Hero)
                                    moveCharacter(hero, new Point(-1, 0));

                            break;

                case 'D':   for (Character hero : characters)
                                if (hero instanceof Hero)
                                    moveCharacter(hero, new Point(1, 0));

                            break;

            }

        }

    }

    /**
     * Move all monsters within the characters list.
     * X and Y movements are randomly chosen from -1 to 1.
     */
    private void moveMonsters() {

        for (Character monster : characters)
            if (monster instanceof Monster) {

                int xMovement = (int) (Math.random() * 3) - 1,
                    yMovement = (int) (Math.random() * 3) - 1;

                moveCharacter(monster, new Point(xMovement, yMovement));

            }

    }

    /** Print a little kind message */
    private void exitGame() {

        outputString += "Thank you for playing...\n";

    }

    /**
     * Move the given character with the amount of values from the provided point.
     * This is only done if a success is returned.
     * @param character - This is the character that should be moved.
     * @param point - This is for how much the characters location should be moved.
     */
    private void moveCharacter(Character character, Point point) {

        Point oldLocation = character.getLocation();

        Point newLocation = new Point(point.x + character.getLocation().x, point.y + character.getLocation().y);

        String result = map.moveTextureLocation(oldLocation, newLocation);

        if (result.contains("Success")) {

            character.setLocation(newLocation);

            if (result.contains("Hero") || result.contains("Monster")) {

                for (Character characterCollided : characters)
                    if (character.getLocation().equals(characterCollided.getLocation()))
                        characterCollided.setTexture(map.fightTexture);

            }

        }

    }

    /**
     * Create either a user defined character (hero) or create one or more monsters depending on the map.
     * If the specified hero name already exist in the database, then load settings for that hero name.
     * If the used have asked for a reset of the hero, then settings will be reset and updated in the database.
     * @param userDefined - True if the user should define a hero. False if monsters should be created.
     */
    private void createCharacter(boolean userDefined) {
        
        if (userDefined) {

            Hero hero = new Hero("HERO", 3);

            boolean heroExists = false;

            if (gameDatabase.getHero(hero.getName()) != null)
                heroExists = true;

            if (!resetHero & heroExists) {

                hero = gameDatabase.getHero(hero.getName());

                map = gameDatabase.getMap(hero.getName());
                
                if (Arrays.equals(map.heroTexture, hero.getTexture()))
                    hero.setTexture(map.heroTexture);
                else if (Arrays.equals(map.fightTexture, hero.getTexture()))
                    hero.setTexture(map.fightTexture);

                for (Point point : map.getTextureLocations(map.heroTexture))
                        map.setTextureLocation(map.floorTexture, point);
                
                map.setTextureLocation(hero.getTexture(), hero.getLocation());

                characters.add(hero);

                resetHero = false;
                
            }
            else {

                hero.setLevel(1);

                hero.setSkillArray(new Skill("Basic", 1, 10), 0);
                hero.setSkillArray(new Skill("Medium", 3, 6), 1);
                hero.setSkillArray(new Skill("High", 6, 8), 2);

                hero.setTexture(map.heroTexture);

                gameDatabase.setMap(map.getMapDirectory(), map.getMapFileName(), hero.getName());

                if (map.getTextureLocations(hero.getTexture()).size() == 0) {

                    if (map.getTextureLocations(map.floorTexture).size() == 0) {

                        outputString += "\n  Error using map, no floor textures detected.\n";

                        characters.clear();

                        gameState = state.MAPERROR;

                        firstTimeListMaps = true;

                    } else {

                        hero.setLocation(map.getTextureLocations(map.floorTexture).get(0));

                        hero.setLocation(map.getTextureLocations(map.floorTexture).get(0));

                        characters.add(hero);

                        map.setTextureLocation(hero.getTexture(), hero.getLocation());

                    }

                } else {

                    hero.setLocation(map.getTextureLocations(hero.getTexture()).get(0));

                    hero.setLocation(map.getTextureLocations(hero.getTexture()).get(0));

                    characters.add(hero);

                }

                gameDatabase.setHero(hero);
                
            }

        }
        else {

            MonsterSettings monsterSettings = new MonsterSettings(map.getMapDirectory(), map.getMapFileName());
            
            Monster monster = new Monster("MONSTER1", 1);
            
            monster.setSkillArray(new Skill("Basic", 1, 10), 0);
            
            Monster newMonsterSettings = monsterSettings.getMonster(monster.getName());
            
            if (newMonsterSettings != null)
                monster = newMonsterSettings;
            
            monster.setLevel(characters.get(0).getLevel());

            monster.setTexture(map.monsterTexture);

            if (map.getTextureLocations(monster.getTexture()).size() == 0) {

                if (map.getTextureLocations(map.floorTexture).size() == 0){

                    outputString += "\n  Error using map, no floor textures detected.\n";

                    characters.clear();

                    gameState = state.MAPERROR;

                    firstTimeListMaps = true;

                }
                else {

                    monster.setLocation(map.getTextureLocations(map.floorTexture).get(map.getTextureLocations(map.floorTexture).size() - 1));

                    characters.add(monster);

                    map.setTextureLocation(monster.getTexture(), monster.getLocation());

                }

            }
            else {

                for (int i = 0; i < map.getTextureLocations(map.monsterTexture).size(); i++) {

                    monster = new Monster("MONSTER" + String.valueOf(i + 1), 1);
                    
                    monster.setSkillArray(new Skill("Basic", 1, 10), 0);

                    newMonsterSettings = monsterSettings.getMonster(monster.getName());

                    if (newMonsterSettings != null)
                        monster = newMonsterSettings;
                    
                    monster.setLevel(characters.get(0).getLevel());

                    monster.setTexture(map.monsterTexture);

                    monster.setLocation(map.getTextureLocations(monster.getTexture()).get(i));

                    characters.add(monster);

                }

            }

        }
        
    }

    /**
     * Spawn the specified amount of monsters of the provided level.
     * A maximum of five monsters will be spawned.
     * @param amount - This is the amount of monsters to create.
     * @param level - This is of what level the monsters should be.
     */
    private void spawnExtraMonsters(int amount, int level) {

        if (amount > 5)
            amount = 5;

        for (int i = 0; i < amount; i++) {

            MonsterSettings monsterSettings = new MonsterSettings(map.getMapDirectory(), map.getMapFileName());
            
            Character monster = new Monster("MONSTER" + String.valueOf(i + 1), 1);
            
            monster.setSkillArray(new Skill("Basic", 1, 10), 0);

            Monster newMonsterSettings = monsterSettings.getMonster(monster.getName());

            if (newMonsterSettings != null)
                monster = newMonsterSettings;
            
            monster.setLevel(level);

            monster.setTexture(map.monsterTexture);

            monster.setLocation(map.getTextureLocations(map.floorTexture).get(map.getTextureLocations(map.floorTexture).size() - 1));

            characters.add(monster);

            map.setTextureLocation(monster.getTexture(), monster.getLocation());

        }

    }

    /**
     * Represent the user for either a menu displaying the maps or a menu for selecting a map.
     * @param showOnly - True if a displaying menu should be shown. False if a selection menu should be shown.
     */
    private void listMaps(boolean showOnly) {

        int input = convertToInteger(userInput.substring(userInput.length() - 1));

        if (showOnly) {

            gameState = state.LISTMAPSHOWONLY;

            outputString = "  Display Maps\n  -------------\n" + userInterface.loadMenu(UserInterface.menu.SHOWMAP, map.getMaps());

        }
        else {

            gameState = state.LISTMAP;

            outputString = "  Select Map\n  ------------\n" + userInterface.loadMenu(UserInterface.menu.SELECTMAP, map.getMaps());

        }

        if (!firstTimeListMaps)
            switch (input) {

                case 0:  if (showOnly) {

                            initializeGame();

                            break;

                         }
                default: if (input <= map.getMapFiles().length && input >= 0) {

                            map.setMap(map.getMapFileName(input));

                            if (showOnly)
                                outputString =  map.getMap() + "\n  Display Maps\n  -------------\n" +
                                                userInterface.loadMenu(UserInterface.menu.SHOWMAP, map.getMaps());
                            else {

                                gameState = state.STARTGAME;

                                startGame();

                            }

                         }
                         else
                            if (showOnly)
                                listMaps(true);
                            else
                                listMaps(false);

            }

        firstTimeListMaps = false;

    }

    /**
     * Convert a string to an integer if possible, if not possible then a value of -1 is returned instead.
     * @param string - This is the string to be converted.
     */
    private int convertToInteger(String string) {

        int value;

        try {

            value = Integer.parseInt(string);

        } catch(NumberFormatException e) {

            return -1;

        }

        return value;

    }

    /** Return the current keycode. */
    public int getKeyCode() {
        return keyCode;
    }

    /** Set the keycode with the provided value.
     *  @param keyCode - This is the key code.
     */
    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    /** Return the output string. */
    public String getOutputString() {
        return outputString;
    }

    /** Set the output string to with the provided string.
     *  @param outputString - This is the string to use.
     */
    public void setOutputString(String outputString) {
        this.outputString = outputString;
    }

    /** Resets the hero's stats. */
    public void resetHero() {

        resetHero = true;

        characters.clear();

        initializeGame();

    }

    /** Redirect the user to the main menu. */
    public void showMainMenu() {

        restartGame = true;

        characters.clear();

        initializeGame();

    }

}
