
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

// XSL transformation
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultStyledDocument;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xerces.dom.DocumentImpl;
import org.eml.MMAX2.annotation.markables.AlphabeticMarkableComparator;
import org.eml.MMAX2.annotation.markables.DiscourseOrderMarkableComparator;
import org.eml.MMAX2.annotation.markables.EndingMarkableComparator;
import org.eml.MMAX2.annotation.markables.Markable;
import org.eml.MMAX2.annotation.markables.MarkableChart;
import org.eml.MMAX2.annotation.markables.MarkableIDComparator;
import org.eml.MMAX2.annotation.markables.MarkableLevel;
import org.eml.MMAX2.annotation.markables.MarkableLevelPositionComparator;
import org.eml.MMAX2.annotation.markables.StartingMarkableComparator;
import org.eml.MMAX2.api.DiscourseAPI;
import org.eml.MMAX2.core.MMAX2;
import org.eml.MMAX2.gui.document.MMAX2Document;
import org.eml.MMAX2.utils.MMAX2Constants;
import org.eml.MMAX2.utils.MMAX2Utils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MMAX2Discourse implements DiscourseAPI
{   
    private String nameSpace = null;
    
    private HashMap<String, String> hash = null;   
    
    private ArrayList<String> recentTextEntries = new ArrayList<String>();
    private ArrayList<String> recentAttributeEntries = new ArrayList<String>();
    
    private String commonBasedataPath="";
    
    protected DocumentImpl wordDOM = null;
    protected String wordFileName="";
    
    protected String[] styleSheetFileNames;
    protected String currentStyleSheet;

    /** Contains at position X the ID of the Discourse Element with Discourse position X */
    protected String[] discourseElementAtPosition = null;        
    protected ArrayList<String> temporaryDiscourseElementAtPosition=null;

    /** Maps IDs of DiscourseElements to their numerical Discourse positions. Used by getDiscoursePositionFromDiscourseElementId(String id). */
    protected HashMap<String, Integer> discoursePositionOfDiscourseElement=null;    
    
    /** Contains at position X the display start position (i.e. character position in display string) of the DE with Discourse position X. */   
    protected Integer[] displayStartPosition = null;    
    protected ArrayList<Integer> temporaryDisplayStartPosition=null;
    
    /** Contains at position X the display start position (i.e. character position in display string) of the DE with Discourse position X. */    
    protected Integer[] displayEndPosition = null;
    protected ArrayList<Integer> temporaryDisplayEndPosition=null;        
    
    protected StringWriter incrementalTransformationResult = null; 
    
    /** Position up to which the incrementalTransformationResult has already been processed, incremented by method getNextDocumentChunk(). */
    protected int lastStart = 0;
    
    protected MarkableChart chart;
    
    protected HashMap<Object, Object> markableDisplayAssociation=null;
    protected HashMap<Object, Object> hotSpotDisplayAssociation=null;
    
    public static StartingMarkableComparator STARTCOMP=null;
    public static EndingMarkableComparator ENDCOMP=null;
    public static AlphabeticMarkableComparator ALPHACOMP=null;
    public static DiscourseOrderMarkableComparator DISCOURSEORDERCOMP=null;
    public static MarkableLevelPositionComparator LEVELCOMP=null;
    public static MarkableIDComparator IDCOMP=null;
    
    protected MMAX2 mmax2 = null;
        
    protected boolean hasGUI = false;            
    
    /** Creates new Discourse */
    public MMAX2Discourse(boolean withGUI) 
    {    	
        hasGUI = withGUI;
        discoursePositionOfDiscourseElement = new HashMap<String, Integer>();
        chart = new MarkableChart(this);
        temporaryDisplayStartPosition = new ArrayList<Integer>();
        temporaryDisplayEndPosition = new ArrayList<Integer>();
        temporaryDiscourseElementAtPosition = new ArrayList<String>();
        markableDisplayAssociation = new HashMap<Object, Object>();
        hotSpotDisplayAssociation = new HashMap<Object, Object>();
        
        STARTCOMP = new StartingMarkableComparator();
        ENDCOMP = new EndingMarkableComparator();
        ALPHACOMP = new AlphabeticMarkableComparator();
        DISCOURSEORDERCOMP = new DiscourseOrderMarkableComparator();
        LEVELCOMP = new MarkableLevelPositionComparator();
        IDCOMP = new MarkableIDComparator();        
    }


    public MMAX2Discourse _buildDiscourse(String infile,String commonPathsFile)
    {
    	System.err.println("Warning: This is a dummy implementation only. Use the *static* method\n\tMMAX2Discourse buildDiscourse(String infile,String commonPathsFile)!");
    	return null;
    }
    
    public static MMAX2Discourse buildDiscourse(String infile,String commonPathsFile)
    {
    	boolean verbose = true;
    	
    	String verboseVar = System.getProperty("verbose");
    	if (verboseVar != null && verboseVar.equalsIgnoreCase("false"))
    	{
    		verbose = false;
    	}
    	
        long start = System.currentTimeMillis();
        if (verbose) System.err.print("   loading ... ");
        MMAX2DiscourseLoader loader = new MMAX2DiscourseLoader(infile, false,commonPathsFile);
        if (verbose)  System.err.println("("+(System.currentTimeMillis()-start)+")");
        MMAX2Discourse currentDiscourse = loader.getCurrentDiscourse();        
        
        start = System.currentTimeMillis();
        if (verbose)  System.err.print("   style sheet ... ");
        currentDiscourse.applyStyleSheet(loader.getCommonStylePath()+"generic_nongui_style.xsl");
        if (verbose)  System.err.println("("+(System.currentTimeMillis()-start)+")");
        start = System.currentTimeMillis();
        if (verbose)  System.err.print("   initializing ... ");
        currentDiscourse.performNonGUIInitializations();
        if (verbose)  System.err.println("("+(System.currentTimeMillis()-start)+")");
        return currentDiscourse;
    }
    
    public static MMAX2Discourse buildDiscourse(String infile)
    {
    	return buildDiscourse(infile,"");
    }
    
    public final void setNameSpace(String _nameSpace)
    {
        nameSpace = _nameSpace;
    }
        
    public final String getNameSpace()
    {
        return nameSpace;
    }
    
    public final void setWordFileName (String name)
    {
        wordFileName = name;
    }
    public final String getWordFileName()
    {
        return wordFileName;
    }        
    
    public final void addWithID(String id, Element node)
    {
        wordDOM.putIdentifier(id, node);
    }
        
    public final void destroyDependentComponents()
    {
        discoursePositionOfDiscourseElement.clear();
        discoursePositionOfDiscourseElement = null;
        discoursePositionOfDiscourseElement = new HashMap<String, Integer>();
        chart = null;
        temporaryDisplayStartPosition = null;
        temporaryDisplayEndPosition = null;
        temporaryDiscourseElementAtPosition = null;
        markableDisplayAssociation.clear();        
        markableDisplayAssociation = null;

        hotSpotDisplayAssociation.clear();        
        hotSpotDisplayAssociation = null;
        
        STARTCOMP =  null;
        ENDCOMP = null;
        DISCOURSEORDERCOMP = null;
        LEVELCOMP = null;        
        wordDOM = null;
        hash = null;

    }
    
    public final boolean getHasGUI()
    {
        return hasGUI;
    }
    
//    protected void finalize()
//    {
////        System.err.println("MMAX2Discourse is being finalized!");        
//        try
//        {
//            super.finalize();
//        }
//        catch (java.lang.Throwable ex)
//        {
//            ex.printStackTrace();
//        }        
//    }
    
        
    public final MMAX2Document getDisplayDocument()
    {
        return mmax2.getCurrentDocument();
    }
   
    public final void setMMAX2(MMAX2 _mmax2)
    {
        mmax2 = _mmax2;             
    }
    
    public final MMAX2 getMMAX2()
    {
        return mmax2;
    }
    
    public final Integer[] getAllDisplayAssociations()
    {
        if (markableDisplayAssociation.size()!=0)
        {
            return (Integer[]) (((Set)markableDisplayAssociation.keySet()).toArray(new Integer[1]));
        }
        else
        {
            return null;
        }
    }
    
    
    public final Markable getMarkableAtDisplayAssociation(int displayPosition)
    {
        // No active/inactive distinction necessary, because only active Markables will have MarkableHandles anyway
        // WRONG: Handles of deactivated layers will stay around until next re-application !!
        Markable result = (Markable) markableDisplayAssociation.get(displayPosition);
        return result;
    }
     
    public final String getHotSpotAtDisplayAssociation(int displayPosition)
    {
        String result = (String) hotSpotDisplayAssociation.get(displayPosition);
        return result;        
    }
    
    public final Integer[] removeDisplayAssociationsForMarkable(Markable removee)
    {
        Set all = markableDisplayAssociation.entrySet();
        Iterator it = all.iterator();
        ArrayList<Integer> positions = new ArrayList<Integer>();
        Integer currentPos = null;
        boolean added = false;
        while (it.hasNext())
        {
            Entry<?, ?> current = (Entry<?, ?>) it.next();
            if (current.getValue().equals(removee))
            {
                it.remove();
                // Get current position
                currentPos = (Integer) current.getKey();
                if (positions.size()==0)
                {
                    // The list is still empty, so simply add 
                    positions.add(currentPos);
                }
                else
                {
                    // Find insertion point
                    for (int p=0;p<positions.size();p++)
                    {
                        if ((((Integer)positions.get(p)).intValue())>currentPos.intValue())
                        {
                            positions.add(p,currentPos);
                            added=true;
                            break;
                        }
                    }
                }
                if(!added) positions.add(currentPos);
            }
        }
        return (Integer[]) positions.toArray(new Integer[0]);
    }
    
    public final DocumentImpl getWordDOM()
    {
        return wordDOM;
    }
    
    public final int getDisplayStartPositionFromDiscoursePosition(int discoursePosition)
    {
        int result = -1;
        try
        {
            result = displayStartPosition[discoursePosition].intValue();
        }        
        catch (java.lang.ArrayIndexOutOfBoundsException ex)
        {
            ex.printStackTrace();
        }
        return result;
    }
    
    public final int getDisplayEndPositionFromDiscoursePosition(int discoursePosition)
    {        
        int result = -1;
        try
        {
            result = displayEndPosition[discoursePosition].intValue();
        }  
        catch (java.lang.ArrayIndexOutOfBoundsException ex)
        {
            ex.printStackTrace();
        }        
        return result;        
    }
        
    public final int getDiscoursePositionAtDisplayPosition(int displayPosition)
    {
        int DiscPos = -1;
        int startPos = 0;
        int endPos = 0;
//        Integer displayPosition = new Integer(_displayPosition);
        // Try if pos is the exact beginning of a Discourse Element
        startPos = Arrays.binarySearch(displayStartPosition,displayPosition);
        if (startPos >= 0 && startPos < displayStartPosition.length)
        {
        	try
        	{
        		if (displayStartPosition[startPos].equals(displayPosition))
        		{
        			// The user clicked the first character
        			return startPos;
        		}
        	}
        	catch (java.lang.ArrayIndexOutOfBoundsException ex)
        	{
        		ex.printStackTrace();
        	}
        }
                
        // Try if pos is the exact end of a Discourse Element        
        endPos = Arrays.binarySearch(displayEndPosition,displayPosition);
        if (endPos >=0 && endPos < displayEndPosition.length)
        {
        	try
        	{
        		if (displayEndPosition[endPos].equals(displayPosition))