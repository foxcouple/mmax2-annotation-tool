
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

package org.eml.MMAX2.discourse;

// Display Attributes
import java.awt.Color;
import java.io.File;

import javax.swing.JOptionPane;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xpath.NodeSet;
import org.eml.MMAX2.annotation.markables.Markable;
import org.eml.MMAX2.annotation.markables.MarkableFileLoader;
import org.eml.MMAX2.annotation.markables.MarkableLevel;
import org.eml.MMAX2.utils.MMAX2Utils;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class MMAX2DiscourseLoader 
{
    protected String commonPathsFile 			= "common_paths.xml";    
    protected String commonSchemePath 			= "";
    protected String commonStylePath 			= "";
    protected String commonBasedataPath 		= "";
    protected String commonCustomizationPath 	= "";
    protected String commonMarkablePath			= "";
    protected String commonQueryPath			= "";    
    protected String rootPath 					= "";    
    protected String wordFileName 				= "";    

    protected String[] markableFileNames;
    protected String[] markableLevelNames;
    protected String[] schemeFileNames;    
    protected String[] customizationFileNames;
    protected String[] startupModes;
    
    /** Number of annotation levels (i.e. markable files) in .mmax file. */
    protected int levelCount = 0;
    
    protected String[] styleSheetFileNames = new String[0];
    protected String[] userSwitches = new String[0];        
        
    private String nameSpace="";    
    /** This has to be static so it can be called from static method getDiscourseElementNodes(String span). */
    protected static MMAX2Discourse currentDiscourse;

    boolean VERBOSE = false;
    boolean DEBUG = false;
    
    /** Creates new DiscourseLoader from the supplied .MMAX file. */
    public MMAX2DiscourseLoader(String mmaxFileName, boolean withGUI, String suppliedCommonPathsFile) 
    {
    	try { if (System.getProperty("verbose").equalsIgnoreCase("true")) {VERBOSE = true;} }
    	catch (java.lang.NullPointerException x) { }

    	try { if (System.getProperty("debug").equalsIgnoreCase("true")) {DEBUG = true;} }
    	catch (java.lang.NullPointerException x) { }

        // Suppose that default common paths is to be used
        boolean useDefault = true;
        if (suppliedCommonPathsFile.equals("")==false)
        {
            commonPathsFile = suppliedCommonPathsFile;
            useDefault = false;
        }
                
        // Extract root path, which is the path of the .mmax file
        rootPath = mmaxFileName.substring(0,mmaxFileName.lastIndexOf(java.io.File.separator)+1);
        // Set all paths to root by default; may be overwritten later
        commonSchemePath 		= rootPath;
        commonStylePath 		= rootPath;
        commonBasedataPath 		= rootPath;
        commonCustomizationPath = rootPath;
        commonMarkablePath 		= rootPath;
        commonQueryPath 		= rootPath;

        // This parser is used for different files
        DOMParser parser = new DOMParser();
                
        try { parser.setFeature("http://xml.org/sax/features/validation",false); }
        catch (org.xml.sax.SAXNotRecognizedException | org.xml.sax.SAXNotSupportedException ex)
        {
            ex.printStackTrace();            
            return;
        }
                
        // Parse .mmax file. In most cases, this provide the basedata file name only. 
        try { parser.parse(new InputSource(new File(mmaxFileName).toURI().toString())); }
        catch (org.xml.sax.SAXParseException exception)
        {
            String error = "Line: "+exception.getLineNumber()+" Column: "+exception.getColumnNumber()+"\n"+exception.toString();
            System.err.println(error);
            JOptionPane.showMessageDialog(null,error,"DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);
        }                
        catch (org.xml.sax.SAXException | java.io.IOException exception)
        {
            String error = exception.toString();
            System.err.println(error);
            JOptionPane.showMessageDialog(null,error,"DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);
        }
        
        DocumentImpl MMAXDOM = (DocumentImpl) parser.getDocument();
        NodeList temp = MMAXDOM.getElementsByTagName("words");
        if (temp.getLength()!=0)
        {
            //try { wordFileName = commonBasedataPath+temp.item(0).getFirstChild().getNodeValue(); } 
            try { wordFileName = temp.item(0).getFirstChild().getNodeValue(); }
            catch (java.lang.NullPointerException ex) { ex.printStackTrace(); }
        }
        MMAXDOM=null;
        
        String cpFileName = commonPathsFile;
        // Default: assume common_paths.xml at MMAX2 project root path
        if (useDefault) { cpFileName=rootPath+cpFileName; }
                                
        try { parser.parse(new InputSource(new File(cpFileName).toURI().toString()));}
        catch (org.xml.sax.SAXParseException exception)
        {   
            String error = "Line: "+exception.getLineNumber()+" Column: "+exception.getColumnNumber()+"\n"+exception.toString();
            System.err.println(error);
            JOptionPane.showMessageDialog(null,error,"DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);
        }                
        catch (org.xml.sax.SAXException exception)
        {
            String error = exception.toString();
            System.err.println(error);
            JOptionPane.showMessageDialog(null,error,"DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);
        }
        catch (java.io.FileNotFoundException exception)
        {   
        	String error = exception.toString();
            // The common path was not found
            if (!useDefault)
            {
                // and it was a supplied one
                System.err.println(error);
                JOptionPane.showMessageDialog(null,error,"DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);
                return;
            }
            else
            {
                // It was the default one that was not found
            	System.err.println(error);
                JOptionPane.showMessageDialog(null,"No default common_paths.xml found!","DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        catch (java.io.IOException exception)
        {
            String error = exception.toString();
            System.err.println(error);
            JOptionPane.showMessageDialog(null,error,"DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);
        }
                                
        if (parser.getDocument()!=null)
        {
        	if (isVerbose()) {System.err.println(" Reading common data from "+cpFileName);}
            DocumentImpl CPFDOM = null;
            CPFDOM = (DocumentImpl) parser.getDocument();
            NodeList currentCommonPathNodeList = null;
            
            currentCommonPathNodeList = CPFDOM.getElementsByTagName("scheme_path");
            if (currentCommonPathNodeList.getLength()!=0)
            {
                String tempSchemePath = "";
                try { tempSchemePath = toPlatformdependentPath(currentCommonPathNodeList.item(0).getFirstChild().getNodeValue()); }
                catch (java.lang.NullPointerException ex) { }

                if (new java.io.File(tempSchemePath).isAbsolute()) 	{ commonSchemePath = tempSchemePath; } 
                else 												{ commonSchemePath = commonSchemePath+tempSchemePath; }
                if (isVerbose()) {System.err.println("  Common annotation scheme path is "+commonSchemePath);}
            }
            
            currentCommonPathNodeList = CPFDOM.getElementsByTagName("style_path");
            if (currentCommonPathNodeList.getLength()!=0)
            {
                String tempStylePath = "";
                try { tempStylePath = toPlatformdependentPath(currentCommonPathNodeList.item(0).getFirstChild().getNodeValue()); }
                catch (java.lang.NullPointerException ex) { }
                
                if (new java.io.File(tempStylePath).isAbsolute()) 	{ commonStylePath = tempStylePath; }
                else 												{ commonStylePath = commonStylePath+tempStylePath; }
            }
            
            currentCommonPathNodeList = CPFDOM.getElementsByTagName("basedata_path");
            if (currentCommonPathNodeList.getLength()!=0)
            {
                String tempBasedataPath="";
                try { tempBasedataPath =toPlatformdependentPath(currentCommonPathNodeList.item(0).getFirstChild().getNodeValue()); }
                catch (java.lang.NullPointerException ex) { }
                
                if (new java.io.File(tempBasedataPath).isAbsolute())	{ commonBasedataPath = tempBasedataPath; }
                else 													{ commonBasedataPath = commonBasedataPath+tempBasedataPath; }
                wordFileName = commonBasedataPath+wordFileName; 
                if (isVerbose()) {System.err.println("  Basedata file is "+wordFileName);}
            }

            currentCommonPathNodeList = CPFDOM.getElementsByTagName("customization_path");
            if (currentCommonPathNodeList.getLength()!=0)
            {
                String tempCustomizationPath="";
                try { tempCustomizationPath = toPlatformdependentPath(currentCommonPathNodeList.item(0).getFirstChild().getNodeValue()); }
                catch (java.lang.NullPointerException ex) { }
                
                if (new java.io.File(tempCustomizationPath).isAbsolute()) { commonCustomizationPath = tempCustomizationPath; }
                else 													  { commonCustomizationPath = commonCustomizationPath+tempCustomizationPath; }
                if (isVerbose()) {System.err.println("  Common customization path is "+commonCustomizationPath);}
            }

            currentCommonPathNodeList = CPFDOM.getElementsByTagName("markable_path");
            if (currentCommonPathNodeList.getLength()!=0)
            {
                String tempMarkablePath="";
                try { tempMarkablePath =toPlatformdependentPath(currentCommonPathNodeList.item(0).getFirstChild().getNodeValue()); }
                catch (java.lang.NullPointerException ex) { }
                
                if (new java.io.File(tempMarkablePath).isAbsolute()) 	{ commonMarkablePath = tempMarkablePath; }
                else													{ commonMarkablePath = commonMarkablePath+tempMarkablePath; }                                     
                if (isVerbose()) {System.err.println("  Common markable path is "+commonMarkablePath);}
            }

//            currentCommonPathNodeList = CPFDOM.getElementsByTagName("query_path");
            // TODO What is this used for ??
//            if (currentCommonPathNodeList.getLength()!=0)
//            {
//                String tempQueryPath="";
//                try { tempQueryPath = toPlatformdependentPath(currentCommonPathNodeList.item(0).getFirstChild().getNodeValue()); }
//                catch (java.lang.NullPointerException ex) { ex.printStackTrace(); }
//                
//                if (new java.io.File(tempQueryPath).isAbsolute()) 	{ commonQueryPath = tempQueryPath; }
//                else												{ commonQueryPath = commonQueryPath+tempQueryPath; }                                                     
//            }
          
            currentCommonPathNodeList = CPFDOM.getElementsByTagName("views");
            if (currentCommonPathNodeList.getLength()!=0)
            {
                // Get all <views> elements
                NodeList views = CPFDOM.getElementsByTagName("stylesheet");
                if (views.getLength() !=0)
                {
                    // Get number of <level> elements.
                    int viewCount = views.getLength();                        
                    styleSheetFileNames = new String[viewCount];            
                    for (int z=0;z<viewCount;z++)
                    {
                        try { styleSheetFileNames[z] = commonStylePath+views.item(z).getFirstChild().getNodeValue(); }
                        catch (java.lang.NullPointerException ex) { JOptionPane.showMessageDialog(null,"Empty <stylesheet> entry in .mmax file!","DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE); }                       
                    }

                    if (isVerbose()) 
                    {
                    	System.err.println("  Common style sheet path is "+commonStylePath);                    	
                    	for (int z=0;z<viewCount;z++) 
                    	{                    		
                    		System.err.println("   "+z+" "+styleSheetFileNames[z].substring(styleSheetFileNames[z].lastIndexOf(java.io.File.separator)+1));
                    	}
                    }
                }                
            }
            
            currentCommonPathNodeList = CPFDOM.getElementsByTagName("user_switches");
            if (currentCommonPathNodeList.getLength()!=0)
            {
            	if (isVerbose()) { System.err.println("  User switches");}
                // Get all user switches
                NodeList switches = CPFDOM.getElementsByTagName("user_switch");
                // Create array to accept user switch strings
                userSwitches = new String[switches.getLength()];
                for (int z=0;z<switches.getLength();z++)
                {                    
                    String name="";
                    String desc=" ";
                    String def="off";
                    try { name=switches.item(z).getAttributes().getNamedItem("name").getNodeValue(); }
                    catch (java.lang.NullPointerException ex)
                    {
                        System.err.println("   User switch with empty 'name' attribute, ignored!");
                        continue;
                    }
                    
                    try { desc=switches.item(z).getAttributes().getNamedItem("description").getNodeValue(); }
                    catch (java.lang.NullPointerException ex) { }   

                    try { def=switches.item(z).getAttributes().getNamedItem("default").getNodeValue(); }
                    catch (java.lang.NullPointerException ex) { }
                    
                    userSwitches[z] = name+":::"+desc+":::"+def;
                    if (isVerbose()) { System.err.println("   "+z+" "+userSwitches[z]);}
                }                                
            }
           
            temp = CPFDOM.getElementsByTagName("annotations");
            if (temp.getLength()!=0)
            {
                // if (verbose) System.err.println("  Reading <annotation> tags from common paths file "+commonPathsFile);
                nameSpace = mmaxFileName.substring(mmaxFileName.lastIndexOf(java.io.File.separator)+1);
                nameSpace = nameSpace.substring(0,nameSpace.indexOf(".mmax"));                
            
                /** Get all <level> elements from common_path file. */
                NodeList annotations = CPFDOM.getElementsByTagName("level");
                if (annotations.getLength() !=0)
                {
                    /** Get number of <level> elements. */
                    levelCount = annotations.getLength();
                        
                    markableFileNames = new String[levelCount];
                    markableLevelNames = new String[levelCount];
                    schemeFileNames = new String[levelCount];
                    //attributeStrings = new String[levelCount];
                    customizationFileNames = new String[levelCount];
                    startupModes = new String[levelCount];
                
                    for (int z=0;z<levelCount;z++)
                    {
                        String tempMarkablePath = "";
                        try
                        {
                            // Get path as it is given in the level tag
                            tempMarkablePath = annotations.item(z).getFirstChild().getNodeValue();
                            if (new java.io.File(tempMarkablePath).isAbsolute()) { markableFileNames[z] = tempMarkablePath; }
                            else { markableFileNames[z] = commonMarkablePath+tempMarkablePath; }
                            // NEW: Substitute $ with actual name to be used for this document
                            markableFileNames[z] = markableFileNames[z].replaceAll("\\$",nameSpace);
                        }
                        catch (java.lang.NullPointerException ex)
                        {
                            JOptionPane.showMessageDialog(null,"Empty <level> entry in common paths file "+commonPathsFile+"!","DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);
                            System.exit(0);
                        }
                
                        try { markableLevelNames[z] = annotations.item(z).getAttributes().getNamedItem("name").getNodeValue(); }
                        catch (java.lang.NullPointerException ex)
                        {
                            JOptionPane.showMessageDialog(null,"Missing 'name' attribute for level "+markableFileNames[z],"DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);                
                            System.exit(0);                
                        }

                        try
                        { schemeFileNames[z] = commonSchemePath+annotations.item(z).getAttributes().getNamedItem("schemefile").getNodeValue(); }
                        catch (java.lang.NullPointerException ex)
                        {
                            JOptionPane.showMessageDialog(null,"Missing 'schemefile' attribute for level "+markableFileNames[z],"DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);                
                            System.exit(0);                                
                        }
                    
                        try { customizationFileNames[z] = commonCustomizationPath+annotations.item(z).getAttributes().getNamedItem("customization_file").getNodeValue(); }
                        catch (java.lang.NullPointerException ex) { customizationFileNames[z] = ""; }
                        
                        try { startupModes[z] = annotations.item(z).getAttributes().getNamedItem("at_startup").getNodeValue(); }
                        catch (java.lang.NullPointerException ex) { startupModes[z] = "active";}                              
                    }
                }
            }                                                        
        }
         
      if (nameSpace.equals("")==true)
      {
    	  System.err.println("Annotation data (e.g. markable level file names) is missing in common_paths.xml!\nUsing the .mmax file for data other than basedata file names is deprecated!");
    	  System.exit(0);
      }
           
//        if (nameSpace.equals("")==true)
//        {
//            // Read <annotations> from .mmax file only if they were not found in <common_paths> earlier
//            temp = MMAXDOM.getElementsByTagName("annotations");
//            if (temp.getLength()!=0)
//            {
//                /** Get all <level> elements. */
//                NodeList annotations = MMAXDOM.getElementsByTagName("level");
//                if (annotations.getLength() !=0)
//                {
//                    /** Get number of <level> elements. */
//                    levelCount = annotations.getLength();
//                        
//                    markableFileNames = new String[levelCount];
//                    markableLevelNames = new String[levelCount];
//                    schemeFileNames = new String[levelCount];
//                    //attributeStrings = new String[levelCount];
//                    customizationFileNames = new String[levelCount];
//                    startupModes = new String[levelCount];
//                    
//                    for (int z=0;z<levelCount;z++)
//                    {
//                        try
//                        {
//                            markableFileNames[z] =commonMarkablePath+ annotations.item(z).getFirstChild().getNodeValue();
//                        }
//                        catch (java.lang.NullPointerException ex)
//                        {
//                            JOptionPane.showMessageDialog(null,"Empty <level> entry in .mmax file!","DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);
//                            System.exit(0);                
//                        }
//                
//                        try
//                        {
//                            markableLevelNames[z] = annotations.item(z).getAttributes().getNamedItem("name").getNodeValue();
//                        }
//                        catch (java.lang.NullPointerException ex)
//                        {
//                            JOptionPane.showMessageDialog(null,"Missing 'name' attribute for level "+markableFileNames[z],"DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);                
//                            System.exit(0);                
//                        }
//
//                        try
//                        {
//                            schemeFileNames[z] = commonSchemePath+annotations.item(z).getAttributes().getNamedItem("schemefile").getNodeValue();
//                        }
//                        catch (java.lang.NullPointerException ex)
//                        {
//                            JOptionPane.showMessageDialog(null,"Missing 'schemefile' attribute for level "+markableFileNames[z],"DiscourseLoader: "+mmaxFileName,JOptionPane.ERROR_MESSAGE);                
//                            System.exit(0);                                
//                        }
//                    
//                        /*