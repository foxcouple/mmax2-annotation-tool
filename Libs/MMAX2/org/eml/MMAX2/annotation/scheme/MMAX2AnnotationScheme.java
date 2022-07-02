
/*
 * Copyright 2021 Mark-Christoph Müller
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

/*
 * Re-worked handling of casing for attribute names and values (1.15)
 */

package org.eml.MMAX2.annotation.scheme;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import org.apache.xerces.parsers.DOMParser;
import org.eml.MMAX2.annotation.markables.Markable;
import org.eml.MMAX2.annotation.markables.MarkableChart;
import org.eml.MMAX2.annotation.markables.MarkableHelper;
import org.eml.MMAX2.annotation.markables.MarkableRelation;
import org.eml.MMAX2.annotation.markables.MarkableSet;
import org.eml.MMAX2.api.AnnotationSchemeAPI;
import org.eml.MMAX2.api.AttributeAPI;
import org.eml.MMAX2.core.MMAX2;
import org.eml.MMAX2.gui.windows.MMAX2AttributePanel;
import org.eml.MMAX2.gui.windows.MMAX2AttributePanelContainer;
import org.eml.MMAX2.utils.MMAX2Constants;
import org.eml.MMAX2.utils.MMAX2Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class MMAX2AnnotationScheme implements AnnotationSchemeAPI
{        
    /** Maps IDs of the form level_n to MMAX2Attribute objects */
	/** IDs are read from the scheme file and are used as-is*/
    private Hashtable<String, MMAX2Attribute> attributesByID;

    /** Maps attribute name strings to MMAX2Attribute objects */
    // For the purpose of identification, lower-case attributes names when used as keys
    private Hashtable<String, MMAX2Attribute> attributesByLowerCasedAttributeName;
    
    /** Contains MMAX2Attribute objects in the sequence they appear in the scheme xml file */
    private ArrayList<MMAX2Attribute> attributes;

    /** Maps IDs of the form value_n to IDs of the form level_m */
    private Hashtable<String, String> valueIDsToAttributeIDs;

    /** Reference to AttributeWindow used to display this AnnotationScheme */
    private MMAX2AttributePanel attributepanel;

    /** List of those SchemeLevel IDs that are read only (read from .mmax file ) */
    /** TODO Is this used anywhere? */
    ArrayList<?> readOnlySchemeLevels;
 
    // No. of attributes in this scheme
    // TODO Is this used anywhere?
    private int size=0;
    
    MMAX2 mmax2 					= null;    
    boolean ignoreClick 			= false;
    boolean hintLocked 				= false;
    String currentAttributeHintedAt = "";        
    private String schemeFileName 	= "";
    UIMATypeMapping uimaTypeMapping;
    
    boolean VERBOSE = false;
    boolean DEBUG = false;
    
    public MMAX2AnnotationScheme (String schemefilename)
    {
    	try { if (System.getProperty("verbose").equalsIgnoreCase("true")) {VERBOSE = true;} }
    	catch (java.lang.NullPointerException x) { }

    	try { if (System.getProperty("debug").equalsIgnoreCase("true")) {DEBUG = true;} }
    	catch (java.lang.NullPointerException x) { }
    	
    	try { schemeFileName = new File(schemefilename).getCanonicalPath(); } 
    	catch (IOException e) {e.printStackTrace(); }
//    	System.err.println(schemefilename + " " + fs.toURI().toString());
    	
//        schemeFileName = schemefilename;
        /* Create generic DOMparser */
        DOMParser parser = new DOMParser();
        try													{ parser.setFeature("http://xml.org/sax/features/validation",false); }
        catch (org.xml.sax.SAXNotRecognizedException ex) 	{ ex.printStackTrace(); }
        catch (org.xml.sax.SAXNotSupportedException ex)  	{ ex.printStackTrace(); }        
        
        
        try										{ parser.parse(new InputSource (new File(schemeFileName).toURI().toString())); }        
        catch (org.xml.sax.SAXException ex)  	{ ex.printStackTrace(); }
        catch (java.io.IOException ex)       	{ ex.printStackTrace(); }
        
        attributesByID = new Hashtable<String, MMAX2Attribute>();
        attributes = new ArrayList<MMAX2Attribute>();
        attributesByLowerCasedAttributeName = new Hashtable<String, MMAX2Attribute>();
        valueIDsToAttributeIDs = new Hashtable<String, String>();
        
        int labelLength = 0;
        Document schemeDOM = parser.getDocument();        
        
        float fontSize = (float)11.0;
        
        NodeList root = schemeDOM.getElementsByTagName("annotationscheme");
        try 										{ fontSize = Float.parseFloat(root.item(0).getAttributes().getNamedItem("fontsize").getNodeValue());}
        catch (java.lang.NumberFormatException ex) 	{ }       
        catch (java.lang.NullPointerException ex)  	{ }
                 
        if (root.getLength() > 0 && root.item(0).getAttributes() != null && root.item(0).getAttributes().getNamedItem("uima_type_mapping") != null)
        { uimaTypeMapping = new UIMATypeMapping(root.item(0).getAttributes().getNamedItem("uima_type_mapping").getNodeValue());}
        else 
        { uimaTypeMapping = new UIMATypeMapping("","");}
        
        /* Get all level element nodes from scheme file dom */
        NodeList allAttributeNodes = schemeDOM.getElementsByTagName("attribute");        
        
        MMAX2Attribute currentAttribute = null;
        Node currentNode = null;
        
        // Determine max. attribute name length in entire AnnotationScheme
        for (int z=0;z<allAttributeNodes.getLength();z++)
        {            
            currentNode = allAttributeNodes.item(z);
            String currentNodeName = currentNode.getAttributes().getNamedItem("name").getNodeValue();
            if (currentNodeName.length() >= labelLength) labelLength = currentNodeName.length();
        }       
        
        // This is for *attribute-level* tool tips and hints
        String toolTipText = "";
        for (int z=0;z<allAttributeNodes.getLength();z++)
        {
            currentNode = allAttributeNodes.item(z);
            try											{ toolTipText = currentNode.getAttributes().getNamedItem("text").getNodeValue(); }
            catch (java.lang.NullPointerException ex) 	{ toolTipText = "";}
            
            String descriptionFileName = "";
            try 										{descriptionFileName = currentNode.getAttributes().getNamedItem("description").getNodeValue();}
            catch (java.lang.NullPointerException ex)	{ }
            
            String hintText="";
            if (descriptionFileName.equals("")==false) {hintText = readHTMLFromFile(schemeFileName.substring(0,schemeFileName.lastIndexOf(File.separator)+1)+descriptionFileName);}            
            
            String type = "";
            int typeInt = -1;
            try 										{ type = currentNode.getAttributes().getNamedItem("type").getNodeValue().trim(); }
            catch (java.lang.NullPointerException ex)	{ type = "nominal_list";/* Use type 'nominal_list' as default */}
                       
            String attributeToShowInPointerFlag="";
            type=type.toLowerCase();            
            if (type.equals("nominal_button")) typeInt = AttributeAPI.NOMINAL_BUTTON;
            else if (type.equals("nominal_list")) typeInt = AttributeAPI.NOMINAL_LIST;
            else if (type.equals("freetext")) typeInt = AttributeAPI.FREETEXT;
            else if (type.equals("markable_set")) typeInt = AttributeAPI.MARKABLE_SET;
            else if (type.equals("markable_pointer")) typeInt = AttributeAPI.MARKABLE_POINTER;                        
            else if (type.startsWith("markable_pointer:"))
            {
            	// Todo: Support more / other flags here
                typeInt = AttributeAPI.MARKABLE_POINTER;
                attributeToShowInPointerFlag=type.substring(type.indexOf(":")+1);
            }
            
            int lineWidth = 2;
            try								{ lineWidth = Integer.parseInt(currentNode.getAttributes().getNamedItem("width").getNodeValue()); }
            catch (java.lang.Exception ex)	{ }
            
            Color color = Color.black;
            try								{ color = MMAX2Utils.getColorByName((String)currentNode.getAttributes().getNamedItem("color").getNodeValue()); }
            catch (java.lang.Exception ex)	{ }

//            int alpha = 0;
//            try								{ alpha =Integer.parseInt(currentNode.getAttributes().getNamedItem("alpha").getNodeValue()); }
//            catch (java.lang.Exception ex)	{ }
//
//            if (alpha >0)
//            {
//                color = new Color(color.getRed(),color.getGreen(),color.getBlue(),alpha);
//            }
            
            String lineStyle="straight";
            int lineStyleInt=MMAX2Constants.STRAIGHT;
            try								{ lineStyle = (String) currentNode.getAttributes().getNamedItem("style").getNodeValue().toLowerCase(); }
            catch (java.lang.Exception ex)	{ }
            
            if      (lineStyle.equals("straight")) 		{ lineStyleInt = MMAX2Constants.STRAIGHT; }
            else if (lineStyle.equals("lcurve")) 		{ lineStyleInt = MMAX2Constants.LCURVE; }
            else if (lineStyle.equals("rcurve")) 		{ lineStyleInt = MMAX2Constants.RCURVE; }
            else if (lineStyle.equals("xcurve")) 		{ lineStyleInt = MMAX2Constants.XCURVE; }
            else if (lineStyle.equals("smartcurve"))	{ lineStyleInt = MMAX2Constants.SMARTCURVE; }
            
            // maxSize is the max. number of targets for a pointer attribute
            int maxSize = -1;
            try 							{ maxSize = Integer.parseInt(currentNode.getAttributes().getNamedItem("max_size").getNodeValue()); }
            catch (java.lang.Exception ex)	{ }
            
            String targetDomain = "";
            try 							{ targetDomain = (String) currentNode.getAttributes().getNamedItem("target_domain").getNodeValue(); }
            catch (java.lang.Exception ex)  { }

            boolean dashed=false;
            String dashAttrib;
            try								{ dashAttrib = (String) currentNode.getAttributes().getNamedItem("dashed").getNodeValue(); }
            catch (java.lang.Exception ex)	{ dashAttrib = "false"; }
            
            if (dashAttrib.equalsIgnoreCase("true")) { dashed=true; }
            
            String add_to_markableset_instruction = "ADD TO MARKABLE SET";
            try 							{ add_to_markableset_instruction = (String) currentNode.getAttributes().getNamedItem("add_to_markableset_text").getNodeValue(); }
            catch (java.lang.Exception ex)	{ }

            String remove_from_markableset_instruction = "REMOVE FROM MARKABLE SET";
            try								{ remove_from_markableset_instruction = (String) currentNode.getAttributes().getNamedItem("remove_from_markableset_text").getNodeValue(); }
            catch (java.lang.Exception ex)	{ }

            String adopt_into_markableset_instruction = "ADOPT INTO MARKABLE SET";
            try 							{ adopt_into_markableset_instruction = (String) currentNode.getAttributes().getNamedItem("adopt_into_markableset_text").getNodeValue(); }
            catch (java.lang.Exception ex)	{ }
            
            String merge_into_markableset_instruction = "MERGE INTO MARKABLE SET";
            try								{ merge_into_markableset_instruction = (String) currentNode.getAttributes().getNamedItem("merge_into_markableset_text").getNodeValue(); }
            catch (java.lang.Exception ex)	{ }
            
            String point_to_markable_instruction = "POINT TO MARKABLE";
            try								{ point_to_markable_instruction = (String) currentNode.getAttributes().getNamedItem("point_to_markable_text").getNodeValue(); }
            catch (java.lang.Exception ex) 	{ }

            String remove_pointer_to_markable_instruction = "REMOVE POINTER TO MARKABLE";
            try								{ remove_pointer_to_markable_instruction = (String) currentNode.getAttributes().getNamedItem("remove_pointer_to_markable_text").getNodeValue(); }
            catch (java.lang.Exception ex)	{ }
            
            UIMATypeMapping currentAttributeUIMAMapping = null;           
            if (currentNode.getAttributes().getNamedItem("uima_type_mapping") != null) 	{ currentAttributeUIMAMapping = new UIMATypeMapping(currentNode.getAttributes().getNamedItem("uima_type_mapping").getNodeValue()); }
            else 																		{ currentAttributeUIMAMapping = new UIMATypeMapping("",""); }                       

            
            /* Generate one MMAX2Attribute object for each attribute read from scheme file dom*/
            /* name for this attribute is taken directly from scheme file DOM, id is set to lower-case to be robust when using 'next' attributes.*/            
            currentAttribute = new MMAX2Attribute(	currentNode.getAttributes().getNamedItem("id").getNodeValue().toLowerCase(),
            										currentNode.getAttributes().getNamedItem("name").getNodeValue(),
            										typeInt,currentNode.getChildNodes(), this, 
            										labelLength, toolTipText, hintText, lineWidth, color, lineStyleInt, 
            										maxSize, targetDomain, 
            										add_to_markableset_instruction, remove_from_markableset_instruction, 
            										adopt_into_markableset_instruction, merge_into_markableset_instruction, 
            										point_to_markable_instruction, remove_pointer_to_markable_instruction,
            										fontSize, dashed, attributeToShowInPointerFlag, currentAttributeUIMAMapping);
            
            /* Map current attribute to its ID, which is sure to be lower-cased  */            
            attributesByID.put(currentAttribute.getID(), currentAttribute);
            
            /* Map current attribute to its (lower-cased) attribute name */
            /* LC is correct here because the name is used as hash key only. */
            attributesByLowerCasedAttributeName.put(currentNode.getAttributes().getNamedItem("name").getNodeValue().toLowerCase(),currentAttribute);
            attributes.add(currentAttribute);
            currentAttribute = null;
            size++;
        }
                
        for (int z=0;z<attributes.size();z++)
        {
            currentAttribute = (MMAX2Attribute) attributes.get(z);           
            MMAX2Attribute[] allDependentAttributes = currentAttribute.getDirectlyDependentAttributes();
            for (int b=0;b<allDependentAttributes.length;b++)
            {
                allDependentAttributes[b].addDependsOn(currentAttribute);
            }
        }        
        
        attributepanel = new MMAX2AttributePanel(this);
        attributepanel.create();            
    }
         
    public boolean isVerbose()
    {
    	return VERBOSE;
    }

    public boolean isDebug()
    {
    	return DEBUG;
    }
    
    /* This returns the unmodified name if no attribute of that name exists. */ 
    public String normalizeAttributeName(String name) {
    	//System.err.println("Java "+name.toLowerCase());
    	if (attributesByLowerCasedAttributeName.containsKey(name.toLowerCase()))
    	{
    		// System.err.println("Found "+name.toLowerCase());
    		return ((MMAX2Attribute)attributesByLowerCasedAttributeName.get(name.toLowerCase())).getDisplayName();
    	}
    	//else System.err.println("Not found "+name.toLowerCase());
    		
    	return name;
    }
	    
    public UIMATypeMapping[] getAllUIMAAttributeMappings()
    {
    	ArrayList<UIMATypeMapping> tempResult = new ArrayList<UIMATypeMapping>();
    	// Iterate over all attributes defined for this scheme (i.e. level)
    	for (int z=0;z<attributes.size();z++)
    	{
    		MMAX2Attribute currentAttribute = (MMAX2Attribute) attributes.get(z);
    		if (currentAttribute.getUIMATypeMapping()!=null)
    		{
    			tempResult.add(currentAttribute.getUIMATypeMapping());
    		}
    	}    	
    	return (UIMATypeMapping[])tempResult.toArray(new UIMATypeMapping[0]);
    }
    
    public static String readHTMLFromFile(String file)
    {
        String result="";
        BufferedReader abbrevReader = null;
        FileInputStream inStream = null;
        try
        {                
            inStream = new FileInputStream(file);
            abbrevReader = new BufferedReader(new InputStreamReader(inStream));//,"windows-1252"));
        }
        catch (java.io.FileNotFoundException ex)
        {
            System.err.println("Error: Couldn't find file "+file);
            return "File "+file+" could not be found!";
        }           
        String currentLine="";
        try
        {
            while((currentLine=abbrevReader.readLine())!=null)
            {
                result=result+"\n"+currentLine;
            }                        
        }
        catch (java.io.IOException ex)	{ ex.printStackTrace(); }

        try								{ abbrevReader.close(); }
        catch (java.lang.Exception ex) 	{ }
        return result;
    }
    
    public UIMATypeMapping getUIMATypeMapping()
    {
    	return uimaTypeMapping;
    }
    
    public final void setAttributePanelContainer(MMAX2AttributePanelContainer _container)
    {
        attributepanel.setAttributePanelContainer(_container);
    }

    public String getSchemeFileName()
    {
        return schemeFileName;
    }    
    
    public final void showAnnotationHint(String hint, boolean _lock, String _att)
    {      
        if (mmax2!=null && mmax2.getCurrentDiscourse().getCurrentMarkableChart().attributePanelContainer.getUseAnnotationHint())
        {
            if (hintLocked)
            {
                // The currently displayed hint is locked
            	// What was 'locking' again ... ???  :-| 
                // So a normal hint should not be displayed unless it locks as well
                if (_lock)
                {
                    // The new hint is to be locked
                    // If the new one is the same as the currentlocked one, just unlock
                    //if (_att.equals(currentAttributeHintedAt))
                	// For 1.15
                    if (_att.equalsIgnoreCase(currentAttributeHintedAt)) { hintLocked = false; }
                    else
                    {
                        // We want to lock another one
                        getCurrentAttributePanel().getContainer().hintToFront.setSelected(true);
                        mmax2.showAnnotationHint(hint,_att+" (locked)");
                        getCurrentAttributePanel().getContainer().hintToFront.setSelected(false);
                        hintLocked = true;
                        currentAttributeHintedAt = _att;
                    }
                }
            }
            else
            {
                // The current hint is not locked
                if (_lock)
                {
                    getCurrentAttributePanel().getContainer().hintToFront.setSelected(true);
                    mmax2.showAnnotationHint(hint,_att+" (locked)");
                    getCurrentAttributePanel().getContainer().hintToFront.setSelected(false);
                    hintLocked = true;
                    currentAttributeHintedAt = _att;
                }
                else
                {
                    mmax2.showAnnotationHint(hint,_att);
                    currentAttributeHintedAt = _att;                                        
                }
            }
        }
    }

    public final void hideAnnotationHint()
    {
        if (mmax2 != null && !hintLocked && mmax2.getCurrentDiscourse().getCurrentMarkableChart().attributePanelContainer.getUseAnnotationHint())
        {
            mmax2.hideAnnotationHint();
        }
    }

    public final void annotationHintToFront()
    {
        if (mmax2 != null) { mmax2.annotationHintToFront(); }
    }

    public final void annotationHintToBack()
    {
        if (mmax2 != null) { mmax2.annotationHintToBack(); }
    }
    
///////////      
    
    public MMAX2Attribute[] getAttributesByType(int type)
    {
        ArrayList tempresult = new ArrayList();
        MMAX2Attribute currentAttribute; 
        for (int p=0;p<attributes.size();p++)
        {
            currentAttribute = (MMAX2Attribute) attributes.get(p);
            if (currentAttribute.getType()==type) { tempresult.add(currentAttribute); }
        }
        MMAX2Attribute realresult[] = new MMAX2Attribute[tempresult.size()];
        for (int z=0;z<tempresult.size();z++) { realresult[z] = (MMAX2Attribute) tempresult.get(z); }
        return realresult;        
    }

    public MMAX2Attribute getUniqueAttributeByType(int type)
    {
    	// 'unique' means 'just return the first one' ...
        MMAX2Attribute currentAttribute; 
        for (int p=0;p<attributes.size();p++)
        {
            currentAttribute = (MMAX2Attribute) attributes.get(p);
            if (currentAttribute.getType()==type) { return currentAttribute; }
        }
        return null;
    }
        
    
    public MMAX2Attribute[] getAttributesByType(int type1, int type2)
    {
        ArrayList tempresult = new ArrayList();
        MMAX2Attribute currentAttribute; 
        for (int p=0;p<attributes.size();p++)
        {
            currentAttribute = (MMAX2Attribute) attributes.get(p);
            if (currentAttribute.getType()==type1 || currentAttribute.getType()==type2)
            {
                tempresult.add(currentAttribute);
            }
        }
        MMAX2Attribute realresult[] = new MMAX2Attribute[tempresult.size()];
        for (int z=0;z<tempresult.size();z++)
        {
            realresult[z] = (MMAX2Attribute) tempresult.get(z);
        }
        return realresult;        
    }

    public MMAX2Attribute getUniqueAttributeByType(int type1, int type2)
    {
        MMAX2Attribute currentAttribute; 
        for (int p=0;p<attributes.size();p++)
        {
            currentAttribute = (MMAX2Attribute) attributes.get(p);
            if (currentAttribute.getType()==type1 || currentAttribute.getType()==type2)
            {
                return currentAttribute;
            }
        }
        return null;
    }
    
    
    public MMAX2Attribute[] getAttributesByName(String nameRegExp)
    {
        ArrayList tempResult = new ArrayList();
        MMAX2Attribute currentAttribute; 
        for (int p=0;p<attributes.size();p++)
        {
            currentAttribute = (MMAX2Attribute) attributes.get(p);
            if (currentAttribute.getDisplayName().toLowerCase().matches(nameRegExp))
            {
            	tempResult.add(currentAttribute);
            }
        }
        MMAX2Attribute realresult[] = new MMAX2Attribute[tempResult.size()];
        for (int z=0;z<tempResult.size();z++)
        {
            realresult[z] = (MMAX2Attribute) tempResult.get(z);
        }
        return realresult;        
    }

    public MMAX2Attribute getUniqueAttributeByName(String nameRegExp)
    {
        MMAX2Attribute currentAttribute; 
        for (int p=0;p<attributes.size();p++)
        {
            currentAttribute = (MMAX2Attribute) attributes.get(p);
            if (currentAttribute.getDisplayName().toLowerCase().matches(nameRegExp))
            {
            	return currentAttribute;
            }
        }