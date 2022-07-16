
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
     return null;        
    }
    

            
    public MMAX2Attribute[] getAttributesByNameAndType(String name, int type)
    {        
    	ArrayList tempResult = new ArrayList();
        MMAX2Attribute[] temp = getAttributesByType(type);
        for (int b=0;b<temp.length;b++)
        {
            if (temp[b].getDisplayName().toLowerCase().matches(name))
            {
            	tempResult.add(temp[b]);
            }
        }
        MMAX2Attribute realresult[] = new MMAX2Attribute[tempResult.size()];
        for (int z=0;z<tempResult.size();z++)
        {
            realresult[z] = (MMAX2Attribute) tempResult.get(z);
        }
        return realresult;        
    }

    public MMAX2Attribute getUniqueAttributeByNameAndType(String name, int type)
    {        
        MMAX2Attribute[] temp = getAttributesByType(type);
        for (int b=0;b<temp.length;b++)
        {
            if (temp[b].getDisplayName().toLowerCase().matches(name))
            {
            	return temp[b];
            }
        }
        return null;
    }
    
    
    public MMAX2Attribute[] getAttributesByNameAndType(String name, int type1, int type2)
    {        
    	ArrayList tempResult = new ArrayList();
        MMAX2Attribute[] temp = getAttributesByType(type1, type2);
        for (int b=0;b<temp.length;b++)
        {
            if (temp[b].getDisplayName().toLowerCase().matches(name))
            {
            	tempResult.add(temp[b]);
            }
        }
        MMAX2Attribute realresult[] = new MMAX2Attribute[tempResult.size()];
        for (int z=0;z<tempResult.size();z++)
        {
            realresult[z] = (MMAX2Attribute) tempResult.get(z);
        }
        return realresult;        
    }

    public MMAX2Attribute getUniqueAttributeByNameAndType(String name, int type1, int type2)
    {        
        MMAX2Attribute[] temp = getAttributesByType(type1, type2);
        for (int b=0;b<temp.length;b++)
        {
            if (temp[b].getDisplayName().toLowerCase().matches(name))
            {
            	return temp[b];
            }
        }
        return null;
    }
    
    
    ///// 
    
    
    public final void setMMAX2(MMAX2 _mmax2)
    {
        mmax2 = _mmax2;
    }
    
    /** This method produces an ArrayList of those MMAX2Attributes in annotation scheme order that do not depend
        on any other attribute. Independence is determined by checking if the attributes's dependsOn list is empty. */
    public final ArrayList<MMAX2Attribute> getIndependentAttributes(boolean enable)
    {
        // Create list to accept result
        ArrayList<MMAX2Attribute> tempresult = new ArrayList<MMAX2Attribute>();       
        MMAX2Attribute currentAttribute=null; 
        
        // Iterate over all Attributes defined in this scheme, in annotation scheme order
        for (int p=0;p<attributes.size();p++)
        {
            // Get current attribute
            currentAttribute = (MMAX2Attribute) attributes.get(p);            
            /* Reset to Default (this will also set 'empty' for relations now!)*/
            currentAttribute.toDefault();
            
            if (currentAttribute.isIndependent())
            {
                if (enable)
                {
                    // Enable if required
                    currentAttribute.setEnabled(!(currentAttribute.getIsReadOnly()));
                }
                // Add at any rate to list of independent attributes
                tempresult.add(currentAttribute);
            }
        }
        return tempresult;
    }
    
    public MMAX2AttributePanel getAttributePanel()
    {
        return attributepanel;
    }
    
    /** This method is called from the mmaxattribute callingAttribute when the user changed 
        some value on it by clicking a button or changing the selection in a listbox. 
        This method also calls itself recursively. */
    public void valueChanged(MMAX2Attribute callingAttribute, MMAX2Attribute topCallingAttribute, MMAX2Attribute[] oldRemovedAttributes, int position, ArrayList requestedAttributesAsList)
    {                
        // If the call to this method occurs, some actual change has happened
        // So set attribute window to dirty
        attributepanel.setHasUncommittedChanges(true);
        // Any callingAttribute has been set to frozen == false at this point
        MMAX2Attribute[] removedAttributes = null;
        
        // Check if oldRemovedAttributes have been handed in
        if (oldRemovedAttributes != null)
        {            
            // Yes, so we are in a recursion
            removedAttributes = oldRemovedAttributes;
        }
        else
        {
            // There is no recursion yet
            // Remove and store those Attributes (if any) from display that depended on the one whose 
            // value was changed.
            // Some or all of these attributes may be valid again, because they can be either 
            // _identical_ to ones that are requested, or they may be different, but cover the same 
            // attributes. 
            // Something can be removed only if current attribute is branching
            if (callingAttribute.getIsBranching())
            {        
                // This removes all directly and indirectly dependent attributes
                removedAttributes = attributepanel.removeTrailingDependentAttributes(callingAttribute);
            }
        }
        
        // Get SchemeLevels that have to be displayed as a result of click, but do not reset them to 
        // default, because this could alter the values of removedLevels.
        // New: This now also covers MARKABLE_POINTER relations
        MMAX2Attribute[] requestedAttributes = callingAttribute.getNextAttributes(false);        
                                            
        if (removedAttributes != null && requestedAttributes.length != 0)
        {
            // Some levels were added and some were removed */
            // So transfer those selections in removedLevels that are valid for requestedLevels to the latter. */
            // There may be references to identical objects in the two arrays, if a level is removed and added at the same time. */
            // Frozen levels will be catered for in mapSelections !!
            mapSelections(removedAttributes, requestedAttributes);   
            
            // Now, attributes in requestedAttributes have either been set to the value they had in 
            // removedAttributes, or to the value some attribute of the same name in removedAttributes 
            // had or to default, in case either the setting failed or the requestedAttribute was not 
            // in removedAttributes at all, in which case it was defaulted from the beginning.            
            
            // The markable may still have some (temporarily undefined or 'extra' attributes which could now be applicable                                    
            // The current markable may have values for the requested levels which are NOT in the set retrieved above
            // Iterate over all requestedAttributes. These might also have been in removedAttributes
            for (int o=0;o<requestedAttributes.length;o++)
            {
                // Get current requested attribute
                MMAX2Attribute currentSchemeLevel = (MMAX2Attribute) requestedAttributes[o];
                // Get current markable value from Markable's attribute collection (may be different 
                // from Attribute window!!)
                // For 1.15: This now returns display attributename
                String currentMarkableValue = attributepanel.currentMarkable.getAttributeValue(currentSchemeLevel.getDisplayName());
                
                if (currentMarkableValue != null && currentMarkableValue.equals("")==false)
                {
                    // The currently selected Markable has some non-null value for the current attribute
                    // This can only be the case if the user earlier opted to keep some invalid value,
                    // and if the attribute was not also removed:
                    // I.E.: An attribute becomes newly available for which the markable still has some old value
                    // This can be an attribute that is still available after value selection
                    if (currentSchemeLevel.isDefined(currentMarkableValue)==false)
                    {
                        
                        // Remove mechanism for keeping invalid values
                        // New strategy: Just overwrite
                        currentSchemeLevel.setIsFrozen(false,"");                                                
                        
                    }// isDefined==false
                    else
                    {
                        // The current markable has a value which is defined for the current 
                        // Attribute, but which was not set from removedLevels
                        if (currentSchemeLevel.oldValue.equals("")==false)
                        {
                            currentSchemeLevel.setSelectedValue(currentSchemeLevel.oldValue, true);
                            currentSchemeLevel.oldValue="";
                        }
                        else
                        {
                            currentSchemeLevel.setSelectedValue(currentMarkableValue, true);                            
                        }
                    }
                }
            }// for Requestedlevels                                    
        }
        else if (removedAttributes != null)
        {
            // Levels are removed, but none are requested (so nothing to be added later) 
            // Make sure requested is valid
            requestedAttributes = new MMAX2Attribute[0];

            // The following is obsolete in the new strategy, since
            // frozen attributes are not possible any more (in new strategy)
            
            // Make sure frozen ones among the removed ones are retained            
            for (int p=0;p<removedAttributes.length;p++)
            {                
                if (((MMAX2Attribute) removedAttributes[p]).getIsFrozen())
                {
                    System.err.println("Frozen: "+((MMAX2Attribute) removedAttributes[p]).getDisplayName());
                    if (attributepanel.keepables.contains(((MMAX2Attribute) removedAttributes[p]).getDisplayName())==false)
                    {
                        attributepanel.keepables.add(((MMAX2Attribute) removedAttributes[p]).getDisplayName());
                    }
                    System.err.println("Keeping in keepables. Size:"+attributepanel.keepables.size());
                }
            }                
        }
        else if (requestedAttributes != null)
        {
            // Levels are requested, but none are removed                                        
            // The current markable may have values for the requested levels which are NOT in the set retrieved above
            for (int o=0;o<requestedAttributes.length;o++)
            {
                MMAX2Attribute currentSchemeLevel = (MMAX2Attribute) requestedAttributes[o];
                String currentMarkableValue = attributepanel.currentMarkable.getAttributeValue(currentSchemeLevel.getDisplayName());
                if (currentMarkableValue != null && currentMarkableValue.equals("")==false) // && currentMarkableValue.equalsIgnoreCase(this._attributeWindow.currentMarkable.getDefaultValue())==false)
                {
                    //hasValue
                    // The currently selected Markable has some non-null value for the current attribute
                    if (currentSchemeLevel.isDefined(currentMarkableValue)==false)
                    {
                        
                        // Remove mechanism for keeping invalid values
                        // New strategy: Just overwrite
                        currentSchemeLevel.setIsFrozen(false,"");
                        
                    }// isDefined==false
                    else
                    {
                        // The current markable has a value which is defined for the current SchemeLevel, but which was not set from removedLevels
                        currentSchemeLevel.setSelectedValue(currentMarkableValue, true);
                    }
                }//HasValue
            }// for Requestedlevels            
        }// requestedLevels != null
        else
        {
            // Nothing was requested or removed
            // Do this to also set callingAttribute.currentIndex
            callingAttribute.setSelectedIndex(position);
        }
                 
        if (requestedAttributesAsList.size()==0)
        {
            // There was no recursion yet, so simply add current requested attributes at top
            // of list of attributes to display
            requestedAttributesAsList.addAll(java.util.Arrays.asList(requestedAttributes));
        }
        else
        {
            // There already is a recursion, so add current requested attributes directly after
            // attribute that triggered the recursion, i.e. callingAttribute
            int movingIndex = 0;
            for (int o=0;o<requestedAttributes.length;o++)
            {
                if (requestedAttributesAsList.contains(requestedAttributes[o])==false)
                {
                    requestedAttributesAsList.add(requestedAttributesAsList.indexOf(callingAttribute)+movingIndex+1, requestedAttributes[o]);       
                    movingIndex++;
                }
            }
        }
        
        //for (int n=0;n<requestedAttributesAsList.size();n++)
        for (int n=0;n<requestedAttributes.length;n++)
        {
            //MMAX2Attribute current = (MMAX2Attribute) requestedAttributesAsList.get(n);
            MMAX2Attribute current = requestedAttributes[n];
            if (current.getIsBranching() && current.getNextAttributes(false).length !=0)
            {
                valueChanged(current,topCallingAttribute,removedAttributes,current.getSelectedIndex(),requestedAttributesAsList);
            }
        }
                
        // Now, add attributes below the one whose value was changed 
        attributepanel.addAttributesAfter(((MMAX2Attribute[])requestedAttributesAsList.toArray(new MMAX2Attribute[0])),topCallingAttribute);                
        try
        {
            /* A change has occurred, which must be applicable and undoable */ 
            if (attributepanel.getContainer().isAutoApply() == false)
            {
                attributepanel.getContainer().setApplyEnabled(true);
                attributepanel.getContainer().setUndoEnabled(true);
            }
            else
            {
                System.out.println("---> Auto-Apply!");
                // NEW 25th February 2005: Always clear keepables on apply
                attributepanel.keepables.clear();
                attributepanel.setMarkableAttributes(attributepanel.currentMarkable,true);
                // NEW: FIX 'dirty after auto-apply'
                attributepanel.currentMarkable.getMarkableLevel().setIsDirty(true,true);
            }
            attributepanel.rebuild();
        }
        catch (java.lang.NullPointerException ex)
        {    
            
        }
        
        attributepanel.getScrollPane().scrollRectToVisible(attributepanel.getLastAttribute().getVisibleRect());
        attributepanel.invalidate();
        attributepanel.repaint();
        attributepanel.getContainer().invalidate();
        attributepanel.getContainer().repaint();
    }// end Method    
    
    /** This method transfers those selections in removedLevels that are valid for requestedLevels to the latter. 
        If a requestedLevel cannot be set according to a removedLevel, it is set to default. 
        // wrong: This method will not alter attributes of type MARAKABLE_SET!
        New: removed levels of type MARKABLE_POINTER and MARKABLE_SET are now correctly mapped / removed
        The two parameter arrays may contain references to the same objects ! */
    public void mapSelections(MMAX2Attribute[] removedLevels, MMAX2Attribute[] requestedLevels)  
    {  
        int remLen = removedLevels.length;
        int reqLen = requestedLevels.length;
        
        MMAX2Attribute currentRequestedLevel = null;
        String currentRequestedAttribute = "";
        MMAX2Attribute currentRemovedLevel = null;        
        String currentRemovedAttribute = "";   
        String currentRemovedValue = "";   
        boolean found = false;
        
        /* Iterate over all requested SchemeLevels, which have NOT been set to default yet. */
        for (int z=0;z<reqLen;z++)
        {
            /* Get current requestedLevel */
            currentRequestedLevel = requestedLevels[z];
            
            /* Iterate over all removed SchemeLevels */
            for(int u=0;u<remLen;u++)
            {
                /* Get current removedLevel */
                currentRemovedLevel = removedLevels[u];                
                
                if (currentRemovedLevel.getID().equals(currentRequestedLevel.getID()))
                {
                    /* The same (identical) SchemeLevel has been removed and requested at the same time */
                    /* Do nothing, most recent value will be kept (somehow) */
                    // Set preferred selected value
                    currentRemovedLevel.oldValue=currentRemovedLevel.getSelectedValue();
                    found = true;
                }                
                if (currentRemovedLevel.getIsFrozen()) 
                {
                    System.err.println("Frozen: "+currentRemovedLevel.getDisplayName());
                    if (attributepanel.keepables.contains(currentRemovedLevel.getDisplayName())==false) 
                    {
                        attributepanel.keepables.add(currentRemovedLevel.getDisplayName());
                        System.err.println("Keeping in keepables. Size:"+attributepanel.keepables.size());
                    }
                }                
            }// for all removed levels
            
            /* Here, all removed levels have been checked against the current requestedLevel */
            if (found == false)
            {
                /* The current requested level is NOT removed at the same time. */
                /* Check if there is a level with the same _attribute name_ removed at the same time. */
                
                /* Again iterate over all removed SchemeLevels */
                for(int u=0;u<remLen;u++)
                {
                    /* Get current removedLevel */
                    currentRemovedLevel = removedLevels[u];
                
                    /* Get Attribute on current requestedLevel */
                    // This returns the cased name now (=DisplayName)
                    currentRequestedAttribute = currentRequestedLevel.getDisplayName();
                    /* Get Attribute on current removedLevel */
                    // This returns the cased name now
                    currentRemovedAttribute = currentRemovedLevel.getDisplayName();
                                                                    
                    //if(currentRequestedAttribute.equals(currentRemovedAttribute))
                    // For 1.15
                    if(currentRequestedAttribute.equalsIgnoreCase(currentRemovedAttribute))
                    {
                        /* We are about to remove a level which has the attribute name of the current requested one, but is NOT identical. */
                        /* Check whether its value can be copied to requestedLevel */
                        /* This is true if the value in removedLevel exists in requestedLevel */
                        currentRemovedValue = currentRemovedLevel.getSelectedValue();
                        /* Try to copy the value from removed to requested level */                        
                        // oldValue has preference over currentRemovedValue, because current could be 'none' or some other meta-value that we do not want to map

                        if (currentRequestedLevel.oldValue.equals("")==false)
                        {
                            // currentRequestedlevel has a preferred reset value, so try to set that. If it fails, currentRequested will be set to default
                            currentRequestedLevel.setSelectedValue(currentRequestedLevel.oldValue,true);
                        }
                        /* If this does fail, currentRequestedLevel keeps its former value */
                        else if (currentRequestedLevel.setSelectedValue(currentRemovedValue,true)==false)