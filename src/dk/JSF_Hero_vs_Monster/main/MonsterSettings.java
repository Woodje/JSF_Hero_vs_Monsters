package dk.JSF_Hero_vs_Monster.main;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;

/**
 * MonsterSettings - Used for reading the settings for monsters in a map.
 * @author Simon Jon Pedersen
 * @version 1.0 20/02-2015.
 */
public class MonsterSettings {

    /** These strings represent the path for the xml settings file. */
    private String mapDirectory, mapFileName;

    /**
     * Constructor.
     * @param mapDirectory - This is the directory of the maps, the xml settings file must be located here.
     * @param mapFileName - This is the name of the map file, the xml settings file must have the same name.
     */
    public MonsterSettings(String mapDirectory, String mapFileName) {
        
        this.mapDirectory = mapDirectory;
        
        this.mapFileName = mapFileName;
        
    }
    
    /**
     * Returns a document from a provided xml file.
     */
    private Document getDocument() {

        Document document;
        
        try {

            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            
            document = documentBuilder.parse(new File(mapDirectory + mapFileName.replaceAll(".map", ".xml")));

            document.getDocumentElement().normalize();
            
            
        } catch (Exception e) {

            return null;
            
        }
        
        return document;
        
    }

    /**
     * Return a monster based on the settings in the provided xml file.
     * If no monster settings is found, the a null will be returned.
     * @param monsterName - This is the name of which monster to find and return.
     */
    public Monster getMonster(String monsterName) {

        Monster monster = null;

        Document document = getDocument();
        
        if (document == null)
            return null;
        
        try {

            NodeList nodeList = document.getElementsByTagName("monster");

            for (int i = 0; i < nodeList.getLength(); i++) {

                Node node = nodeList.item(i);
                
                if(node.getNodeType() == Node.ELEMENT_NODE)
                    if(monsterName.equals(((Element) node).getAttribute("id"))) {
                        
                        NodeList subNodeList = ((Element) node).getElementsByTagName("skill");
                        
                        monster = new Monster(((Element) node).getElementsByTagName("monsterName").item(0).getTextContent(), subNodeList.getLength());

                        for (int x = 0; x < subNodeList.getLength(); x++) {

                            Node subNode = subNodeList.item(x);

                            if (subNode.getNodeType() == Node.ELEMENT_NODE)
                                monster.setSkillArray(new Skill(((Element) subNode).getElementsByTagName("skillName").item(0).getTextContent(),
                                        Integer.valueOf(((Element) subNode).getElementsByTagName("minDamage").item(0).getTextContent()),
                                        Integer.valueOf(((Element) subNode).getElementsByTagName("maxDamage").item(0).getTextContent())), x);

                        }
                        
                    }
                
            }
            
        } catch (Exception e) {

            return null;
            
        }
        
        return monster;
        
    }

}
