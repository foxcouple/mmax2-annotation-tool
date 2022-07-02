
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
        