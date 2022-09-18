
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