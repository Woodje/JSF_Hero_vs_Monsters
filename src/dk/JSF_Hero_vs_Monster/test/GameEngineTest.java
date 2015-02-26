package dk.JSF_Hero_vs_Monster.test;

import dk.JSF_Hero_vs_Monster.main.*;
import dk.JSF_Hero_vs_Monster.main.Character;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class GameEngineTest {

    @Test
    public void testGetStats() throws Exception {

        GameEngine gameEngine = new GameEngine();

        Field privateField = GameEngine.class.getDeclaredField("characters");

        privateField.setAccessible(true);

        Hero hero = new Hero("UnitTestHeroName", 0);

        ((ArrayList<Character>) privateField.get(gameEngine)).add(hero);

        assertNotEquals("Should not return an empty string if a character is added", "", gameEngine.getStats());

    }
}