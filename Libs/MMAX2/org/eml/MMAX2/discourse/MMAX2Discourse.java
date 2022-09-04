
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
        		{
        			// The user clicked the first character
        			return endPos;
        		}
        	}
        	catch (java.lang.ArrayIndexOutOfBoundsException ex)
        	{
        		ex.printStackTrace();
        	}
        }
        // When we are here, the click occurred either in the middle of a Discourse Element, 
        // or in empty space
        if (startPos == endPos)
        {
            // The click occurred in empty space
            return DiscPos;
        }
        else
        {
            return (endPos * (-1))-1;
        }            
    }
    
    public final MMAX2DiscourseElement getDiscourseElementByID(String id)
    {
        MMAX2DiscourseElement result = null;
        Node temp = getDiscourseElementNode(id);
        if (temp != null)
        {
            result = new MMAX2DiscourseElement(temp.getFirstChild().getNodeValue(), temp.getAttributes().getNamedItem("id").getNodeValue(),this.getDiscoursePositionFromDiscourseElementID(temp.getAttributes().getNamedItem("id").getNodeValue()),MMAX2Utils.convertNodeMapToHashMap(temp.getAttributes(),null));        
        }
        return result;
        
    }
    
    
    /** This method receives the String id of a DiscourseElement (word_x) and returns the (0-based)
        discourse position, i.e. the running number of DiscourseElements that are registered in the current
        display. The discourse position is determined using the Hash DiscoursePositionOfDiscourseElement
        which has been filled by calls to this.registerDiscourseElement(String id) during style sheet
        execution. (DiscoursePositionOfDiscourseElement won't be filled until after stylesheet execution!!) 
        This will be filled incrementally during style sheet execution, and is thus available during that. */
    public final int getDiscoursePositionFromDiscourseElementID(String id)
    {               
        int result = -1;
        try
        {
            return ((Integer)discoursePositionOfDiscourseElement.get(id)).intValue();
        }
        catch (java.lang.NullPointerException ex)
        {
            System.err.println("No disc pos for "+id);
            // This should not happen any more, since now even supressed des have a discourse position
            ex.printStackTrace();
        }
        return result;
    }
    
    public final String getDiscourseElementIDAtDiscoursePosition(int pos)
    {
        String result = "";
        if (pos != -1)
        {
            try
            {
                result = discourseElementAtPosition[pos];
            }
            catch (java.lang.ArrayIndexOutOfBoundsException ex)
            {
                //ex.printStackTrace( );
            }
        }
        return result;
    }
            
     public final void registerAllDiscourseElements()
     {
         NodeList allWords = wordDOM.getElementsByTagName("word");
         for (int z=0;z<allWords.getLength();z++)
         {
             registerDiscourseElement(allWords.item(z).getAttributes().getNamedItem("id").getNodeValue());
         }
     }
     
    /** This method receives a DiscourseElement id (word_x), assigns it a discourse position (0-based) and
        stores both values in the Hash DiscoursePositionOfDiscourseElement. This Hash is later used by 
        this.getDiscoursePositionFromDiscourseElementID(String id). This method also adds word_x to the 
        list this.temporaryDiscourseElementAtPosition. This list's size is used as the discourse position
        determiner for the _NEXT_ element to be added. This method must be called for every de, incl. 
        those that are suppressed from the display! <b>Internal use only!</b>*/
     public final void registerDiscourseElement(String id)
     {    	          
         if (discoursePositionOfDiscourseElement == null)
         {
             discoursePositionOfDiscourseElement = new HashMap();
         }          

         if (temporaryDiscourseElementAtPosition == null)
         {
             temporaryDiscourseElementAtPosition = new ArrayList();
         }
        // Map id to discourse position
        discoursePositionOfDiscourseElement.put(id,new Integer(temporaryDiscourseElementAtPosition.size()));
        
        temporaryDiscourseElementAtPosition.add(id);                
     }
                 
    public final MMAX2DiscourseElement[] getDiscourseElements()
    {
        ArrayList result = new ArrayList();
        int pos = 0;
        MMAX2DiscourseElement currentElement = null;
        while(true)
        {
            currentElement = getDiscourseElementAtDiscoursePosition(pos);
            if (currentElement !=null)
            {
                result.add(currentElement);
                currentElement = null;
                pos++;
                continue;
            }
            else
            {
                break;
            }
        }
        return (MMAX2DiscourseElement[]) result.toArray(new MMAX2DiscourseElement[0]);
    }
    
    public final MMAX2DiscourseElement[] getDiscourseElements(Markable _markable)
    {
        ArrayList tempList = new ArrayList();
        String[] markablesDEIDs = _markable.getDiscourseElementIDs();
        for (int z=0;z<markablesDEIDs.length;z++)
        {
            Node temp = getDiscourseElementNode(markablesDEIDs[z]);
            tempList.add(new MMAX2DiscourseElement(temp.getFirstChild().getNodeValue(), temp.getAttributes().getNamedItem("id").getNodeValue(),this.getDiscoursePositionFromDiscourseElementID(temp.getAttributes().getNamedItem("id").getNodeValue()),MMAX2Utils.convertNodeMapToHashMap(temp.getAttributes(),null)));
        }        
        return (MMAX2DiscourseElement[]) tempList.toArray(new MMAX2DiscourseElement[0]);
    }
    
    public final MMAX2DiscourseElement getDiscourseElementAtDiscoursePosition(int discPos)
    {
        MMAX2DiscourseElement result = null;
        String id = getDiscourseElementIDAtDiscoursePosition(discPos);
        Node temp = getDiscourseElementNode(id);
        if (temp != null)
        {
            result = new MMAX2DiscourseElement(temp.getFirstChild().getNodeValue(), temp.getAttributes().getNamedItem("id").getNodeValue(),this.getDiscoursePositionFromDiscourseElementID(temp.getAttributes().getNamedItem("id").getNodeValue()),MMAX2Utils.convertNodeMapToHashMap(temp.getAttributes(),null));        
        }
        return result;
    }
    
    /*
    public final ArrayList getAllMatchingDiscourseElementSequences(MMAX2DiscourseElementSequence entireInputSequence, String regExp, String toMatch)
    {
        // Todo: This is not very efficient
        ArrayList tempResult = new ArrayList();
        // Create pattern to match only once
        Pattern pattern = Pattern.compile(regExp);
        // Iterate over entireINputSequence, one de at a time
        for (int leftBorder=0;leftBorder<entireInputSequence.getLength();leftBorder++)
        {
            // Create subsequence starting at pos leftBorder
            // 0 at the beginning, moving right with each iteration
            for (int len=entireInputSequence.getLength()-leftBorder;len>0;len--)
            {
                MMAX2DiscourseElementSequence inputSequence = entireInputSequence.getSubSequence(leftBorder,len);
                    
                // Get array version of current subsequence
                MMAX2DiscourseElement[] input = inputSequence.getContent();
                String temp = "";
                // Create list to accept mapping from string positions to DEs at these positions
                ArrayList stringPosToDiscourseElement = new ArrayList();
        
                // Iterate over all DiscourseElements in current subsequence
                for (int z=0;z<input.length;z++)
                {
                    // Get current DE
                    MMAX2DiscourseElement currentElement = input[z];
                        
                    if (toMatch.equalsIgnoreCase(""))
                    {
                        // toMatch is empty, so the de *text* is to be matched                
                        for (int p=0;p<=currentElement.toString().length();p++)
                        {
                            // put currentElement reference at each pos in temp string, plus trailing space
                            stringPosToDiscourseElement.add(currentElement);
                        }
                        // Create temp String to match
                        if (currentElement.getAttributeValue("ignore", "false").equals("true"))
                        {
                            String temp2 = currentElement.toString();
                            for (int i=0;i<=temp2.length();i++)
                            {
                                temp = temp + " ";
                            }                    
                        }
                        else
                        {
                            temp = temp +currentElement.toString()+" ";
                        }
                    }
                    else
                    {
                        // Some attribute value is to be used
                        // Get val of attribute to be used (e.g. uh)
                        String val = currentElement.getAttributeValue(toMatch, "+++");
                        // Iterate over length of value to be added to string
                        for (int p=0;p<val.length();p++)
                        {
                            // put currentElement reference at each pos in temp string
                            stringPosToDiscourseElement.add(currentElement);
                            // For a 3-character string, this fills pos's 0, 1 and 2
                        }
                
                        // Create temp String to match                
                        if (currentElement.getAttributeValue("ignore", "false").equals("true"))
                        {
                            // Iterate over length of value to be added to string + 1
                            for (int i=0;i<=val.length();i++)
                            {
                                temp = temp + " ";
                            }                    
                        }
                        else
                        {
                            temp = temp + val+" ";
                        }                                                
                        stringPosToDiscourseElement.add(null);
                    }
                }

                Matcher m = pattern.matcher(temp);                       
                ArrayList currentMatchedSequence = new ArrayList();
                if (m.matches())            
                {
                    // Some strict match has been found
                    // Retrieve start and end of it
                    int start = m.start();
                    int end = m.end();

                    // Iterate over match span
                    for (int q=start;q<end;q++)
                    {
                        // If this loop is entered, a match was found
                        if (stringPosToDiscourseElement.get(q)==null)
                        {
                            continue;
                        }
                        if (currentMatchedSequence.contains((MMAX2DiscourseElement)stringPosToDiscourseElement.get(q))==false)
                        {
                            //tempResult.add((MMAX2DiscourseElement)stringPosToDiscourseElement.get(q));
                            currentMatchedSequence.add((MMAX2DiscourseElement)stringPosToDiscourseElement.get(q));
                        }
                    }
                    // Now, the current match (if any) has been collected in currentMatchedSequence
                    if (currentMatchedSequence.size() > 0)
                    {
                        MMAX2DiscourseElementSequence newSequence = new MMAX2DiscourseElementSequence((MMAX2DiscourseElement[])currentMatchedSequence.toArray(new MMAX2DiscourseElement[0]));
                        tempResult.add(newSequence);
                    }        
                }
                if (currentMatchedSequence.size()>0)
                {
                    leftBorder = leftBorder + currentMatchedSequence.size()-1;
                    break;
                }            
                currentMatchedSequence = null;   
            }
        }
        return tempResult;
        
    }
*/

    public final MMAX2DiscourseElement getNextDiscourseElement(MMAX2DiscourseElement element)    
    {
        MMAX2DiscourseElement currentElement = null;
        int currentDiscPos = 0;
        if (element != null)
        {
            currentDiscPos = element.getDiscoursePosition()+1;
        }
        currentElement = getDiscourseElementAtDiscoursePosition(currentDiscPos);
        return currentElement;
    }
    
    public final MMAX2DiscourseElement getPreviousDiscourseElement(MMAX2DiscourseElement element)    
    {
        MMAX2DiscourseElement currentElement = null;
        int currentDiscPos = getDiscourseElementCount();
        if (element != null)
        {
            currentDiscPos = element.getDiscoursePosition()-1;
        }
        currentElement = getDiscourseElementAtDiscoursePosition(currentDiscPos);
        return currentElement;
    }
    
    /*
    public final MMAX2DiscourseElementSequence getPreceedingDiscourseElements(int currentDiscPos, int len)    
    {
        // The element at currentDiscPos is NOT itself retrieved!
        ArrayList tempresult = new ArrayList();
        MMAX2DiscourseElement currentElement = null;
        if (currentDiscPos > 0)
        {
            while(true)
            {
                // Move one position left
                currentDiscPos--;                
                currentElement = getDiscourseElementAtDiscoursePosition(currentDiscPos);
                // currentElement will be null if no element could be retrieved from pos currentDiscPos
                if (currentElement==null)
                {
                    // No more elements could be retrieved
                    break;
                }
                if (currentElement.getAttributeValue("ignore","+++").equalsIgnoreCase("true")==false)
                {
                    // Add current element to *beginning of* tempresult if it is not ignorable (we move backwards here!)
                    tempresult.add(0,currentElement);
                    currentElement=null;
                    if (tempresult.size()==len)
                    {
                        // len elements have been retrieved
                        break;
                    }
                    continue;
                }
            }
        }
        if (tempresult.size()!=len)
        {
            System.err.println("Warning: Could not retrieve required "+len+" elements ("+tempresult.size()+" only) !");
        }
        return new MMAX2DiscourseElementSequence((MMAX2DiscourseElement[])tempresult.toArray(new MMAX2DiscourseElement[0]));
    }
    */
    
/*    
    public final MMAX2DiscourseElement[] getMatchingDiscourseElementSequence(MMAX2DiscourseElementSequence inputSequence, String regExp, String startAfterDE, String toMatch)
    {
        MMAX2DiscourseElement[] input = inputSequence.getContent();
        boolean started = false;
        String temp = "";
        ArrayList stringPosToDiscourseElement = new ArrayList();
        
        // Iterate over all input DiscourseElements
        for (int z=0;z<input.length;z++)
        {
            // Get current DE
            MMAX2DiscourseElement currentElement = input[z];
            if (startAfterDE.equals("")==false)
            {
                // Only if some start offset was given at all
                if (!started && currentElement.getID().equalsIgnoreCase(startAfterDE)==false)
                {
                    // Move to next and keep ignoring
                    continue;
                }
                else
                {
                    // Ignore this one, but none afterwards
                    if (!started)
                    {
                        started = true;
                        continue;
                    }
                }                
            }
            
            if (toMatch.equalsIgnoreCase(""))
            {
                // The de text is to be matched                
                for (int p=0;p<=currentElement.toString().length();p++)
                {
                    // put currentElement reference at each pos in temp string, plus trailing space
                    stringPosToDiscourseElement.add(currentElement);
                }
                // Create temp String to match
                temp = temp + input[z].toString()+" ";
            }
            else
            {
                // Some attribute value is to be used
                // Get val of attribute to be used
                String val = input[z].getAttributeValue(toMatch, "+++");
                for (int p=0;p<val.length();p++)
                {
                    // put currentElement reference at each pos in temp string
                    stringPosToDiscourseElement.add(currentElement);
                }
                // Create temp String to match
                temp = temp + val+" ";
                stringPosToDiscourseElement.add(null);
            }            
        }
        temp = temp.trim();
//        System.out.println(temp);
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(temp);
            
        ArrayList tempResult = new ArrayList();
            
        //while(m.find())
        if(m.find())
        {
//            System.out.println("Match");
            int start = m.start();
//            System.out.println(start);
            int end = m.end();
//            System.out.println(end);
            for (int q=start;q<end;q++)
            {
                if (stringPosToDiscourseElement.get(q)==null)
                {
                    continue;
                }
//                System.out.println(((MMAX2DiscourseElement)stringPosToDiscourseElement.get(q)).getString());
                if (tempResult.contains((MMAX2DiscourseElement)stringPosToDiscourseElement.get(q))==false)
                {
                    tempResult.add((MMAX2DiscourseElement)stringPosToDiscourseElement.get(q));
                }
            }
        }                                
        return (MMAX2DiscourseElement[])tempResult.toArray(new MMAX2DiscourseElement[0]);            
    }
  
*/    
    
    /** Returns the Node representation of the discourse element with id ID.*/
    public final Node getDiscourseElementNode(String ID)
    {
        Node result = null;         
        try        
        {            
            result = wordDOM.getElementById(ID);
        }
        catch (java.lang.NullPointerException ex)
        {
        	ex.printStackTrace();
        }
        return result;        
    }
    
    protected final void setWordDOM(DocumentImpl dom)
    {
        wordDOM = dom;
    }
    
    public final void resetForStyleSheetReapplication()
    {
    	boolean verbose = false;
    	
    	String verboseVar = System.getProperty("verbose");
    	if (verboseVar != null && verboseVar.equalsIgnoreCase("true")) { verbose = true; }
    	
//        if (verbose) System.err.print("Resetting ... ");
//        long time = System.currentTimeMillis();
        
        temporaryDiscourseElementAtPosition = new ArrayList();
        temporaryDisplayEndPosition = new ArrayList();
        temporaryDisplayStartPosition = new ArrayList();
        lastStart = 0;
        markableDisplayAssociation.clear();
        markableDisplayAssociation = new HashMap();
        hotSpotDisplayAssociation.clear();
        hotSpotDisplayAssociation = new HashMap();
        
        hash = null;
        
        chart.resetMarkablesForStyleSheetReapplication();
        chart.resetHasHandles();
//        System.gc();        
//        if (verbose) System.err.println("done in "+(System.currentTimeMillis()-time)+" milliseconds");

    }
    
    /** This method is called when the deep refresh button on the MarkableLevelControlPanel is pressed. */
    public final void reapplyStyleSheet()
    {
    	boolean verbose = false;
    	
    	String verboseVar = System.getProperty("verbose");
    	if (verboseVar != null && verboseVar.equalsIgnoreCase("true")) { verbose = true;}
    	
        /* Reset currentDocument to new (empty) one .*/
        mmax2.setCurrentDocument(new MMAX2Document(mmax2.currentDisplayFontName,mmax2.currentDisplayFontSize));
        /* Set currentDocument's mmax2 reference. */
        mmax2.getCurrentDocument().setMMAX2(mmax2);
       
        resetForStyleSheetReapplication();
        
//        if (verbose) System.err.print("Reapplying stylesheet "+currentStyleSheet+" ... ");
//        long time = System.currentTimeMillis();
        applyStyleSheet("");
//        if (verbose) System.err.println("done in "+(System.currentTimeMillis()-time)+" milliseconds");     
        
//        if (verbose) System.err.print("Recreating Markable mappings ... ");
//        time = System.currentTimeMillis();        
        // Call to create e.g. DiscoursePositionToMarkableMappings
        chart.createDiscoursePositionToMarkableMappings();
        chart.setMarkableLevelDisplayPositions();
//        if (verbose) System.err.println("done in "+(System.currentTimeMillis()-time)+" milliseconds");
        chart.updateLabels();
        mmax2.getCurrentTextPane().setStyledDocument((DefaultStyledDocument) mmax2.getCurrentDocument());
        chart.initMarkableRelations(); 
        mmax2.requestRefreshDisplay();
        if (mmax2.getCurrentPrimaryMarkable()!= null)
        {
            chart.markableLeftClicked(mmax2.getCurrentPrimaryMarkable());
        }
    }
    
    /** Apply the XSL style sheet in this.styleSheetFileName to this.structureDOM. */ 
    public final void applyStyleSheet(String overrideStyleFileName)
    {        
    	boolean verbose = true;
    	
    	String verboseVar = System.getProperty("verbose");
    	if (verboseVar != null && verboseVar.equalsIgnoreCase("false"))
    	{
    		verbose = false;
    	}

    	
        if (overrideStyleFileName.equals(""))
        {
            overrideStyleFileName = currentStyleSheet;
        }
        
        /* Create string writer to accept XSL processor output */
        incrementalTransformationResult = new StringWriter();

        /* Create XSL processor */
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        
        try
        {            
            //transformer = tFactory.newTransformer(new StreamSource("FILE:"+overrideStyleFileName)); // This does not throw any errors!            
        	transformer = tFactory.newTransformer(new StreamSource(new File(overrideStyleFileName).toURI().toString())); // This does not throw any errors!
        }
        catch (javax.xml.transform.TransformerConfigurationException ex )
        {          
            String error = ex.toString();
            System.err.println(error);
            JOptionPane.showMessageDialog(null,error,"Discourse: "+overrideStyleFileName,JOptionPane.ERROR_MESSAGE);
        }
        catch (java.lang.Exception ex)
        {
        	ex.printStackTrace();
        }
                             
        try
        {
        	transformer.transform(new DOMSource(wordDOM ), new StreamResult(incrementalTransformationResult));
        }
        catch (javax.xml.transform.TransformerException ex)
        {
        	String error = ex.toString();
        	System.err.println(error);
        	System.err.println(ex.getMessageAndLocation());
        	JOptionPane.showMessageDialog(null,error,"Discourse: "+overrideStyleFileName,JOptionPane.ERROR_MESSAGE);
        	ex.printStackTrace();
        }
        
        discourseElementAtPosition = (String[]) temporaryDiscourseElementAtPosition.toArray(new String[1]);
        displayStartPosition = (Integer[]) temporaryDisplayStartPosition.toArray(new Integer[1]);
        displayEndPosition = (Integer[]) temporaryDisplayEndPosition.toArray(new Integer[1]);
        
        temporaryDiscourseElementAtPosition.clear();
        temporaryDisplayEndPosition.clear();
        temporaryDisplayStartPosition.clear();
        try
        {
            incrementalTransformationResult.close();
        }
        catch (java.io.IOException ex)
        {
            
        }
        
        System.gc();            
    }
    
    public final void setCurrentStyleSheet(String name)
    {
        currentStyleSheet = name;
        try
        {
            mmax2.setReapplyBarToolTip(name);
        }
        catch (java.lang.NullPointerException ex)
        {
            //
        }
    }
    
    public final String getCurrentStyleSheet()
    {
        return currentStyleSheet;
    }
    
    public final void setStyleSheetFileNames(String[] names)
    {
        styleSheetFileNames = names;
    }

    public final String[] getStyleSheetFileNames()
    {
        return styleSheetFileNames;
    }
    
       
    /** This method returns the current length of this.incrementalTransformationResult. It is used to associate Discourse Elements
        with display string positions during stylesheet execution. */
    public final int getCurrentDocumentPosition()
    {
        incrementalTransformationResult.flush();
        return incrementalTransformationResult.getBuffer().length();
    }   
    
    /** This method returns the next chunk of the incremental transformation result that has not yet been processed. */
    public final String getNextDocumentChunk()
    {        
        incrementalTransformationResult.flush();
        String temp = incrementalTransformationResult.toString();                
        String result =  temp.substring(lastStart);
        lastStart = temp.length();
        return result;                
    }
    
    public final MarkableLevel getMarkableLevelFromAbsoluteFileName(String absFileName)
    {
        MarkableLevel result = null;
        MarkableLevel[] levels = getCurrentMarkableChart().getActiveLevels();
        for (int z=0;z<levels.length;z++)
        {
            if(levels[z].getAbsoluteMarkableFileName().equals(absFileName))
            {
                result = levels[z];
                break;
            }
        }
        return result;
    }
    
    public final boolean isCurrentlyLoaded(String absoluteMarkableFileName)
    {
        boolean found = false;
        MarkableLevel[] levels = getCurrentMarkableChart().getActiveLevels();
        for (int z=0;z<levels.length;z++)
        {
            if(levels[z].getAbsoluteMarkableFileName().equals(absoluteMarkableFileName))
            {
                found = true;
                break;
            }
        }
        return found;
    }
    
    public final String getStyleSheetOutput()
    {
        incrementalTransformationResult.flush();
        return incrementalTransformationResult.toString();
    }
    
    public final void putInHash(String key, String value)
    {
        if (hash==null) hash = new HashMap();
        hash.put(key,value);
    }
    
    public final String getFromHash(String key)
    {
        String value="";
        if (hash != null)
        {
            value = (String) hash.get(key);            
            if (value == null)
            {
                value="";
            }
        }       
        return value;
    }
    
    
    public final MarkableLevel getMarkableLevelByName(String name, boolean interactive)
    {
        return getCurrentMarkableChart().getMarkableLevelByName(name,interactive);
    }
    
    public final MarkableChart getCurrentMarkableChart()
    {
        return chart;
    }
    
    public final String[] getAllDiscourseElementIDs()
    {
        return discourseElementAtPosition;
    }

    public final int getDiscourseElementCount()
    {
        return discourseElementAtPosition.length;
    }
    
    public final void performNonGUIInitializations()
    {                
        getCurrentMarkableChart().createDiscoursePositionToMarkableMappings();
        getCurrentMarkableChart().setMarkableLevelDisplayPositions();
        getCurrentMarkableChart().initMarkableRelations();        
        getCurrentMarkableChart().updateLabels();                        
    }
       
    public final void setCommonBasedataPath(String path)
    {
        commonBasedataPath = path;
    }
    
    public final String getCommonBasedataPath()
    {
        return commonBasedataPath;
    }
    
    public final void requestDeleteBasedataElement(Node deletee)
    {
        // Get string id of base data element to be removed
        String deleteesID = deletee.getAttributes().getNamedItem("id").getNodeValue();
        // Get list of markables started by it
        ArrayList startedMarkables = getCurrentMarkableChart().getAllStartedMarkables(deleteesID);
        // Get list of markables ended by it
        ArrayList endedMarkables = getCurrentMarkableChart().getAllEndedMarkables(deleteesID);
        
        ArrayList entireMarkables = new ArrayList();
        
        // Determine if there is a markable that consists of the deletee only
        // Iterate over all started markables backwards
        for (int b=startedMarkables.size()-1;b>=0;b--)
        {
            if (endedMarkables.contains((Markable)startedMarkables.get(b)))
            {
                // The current started markable is also finished by the same de
                entireMarkables.add((Markable)startedMarkables.get(b));
                // Remove
                endedMarkables.remove((Markable)startedMarkables.get(b));
                startedMarkables.remove(b);
            }
        }
        
        if (startedMarkables.size() > 0 || endedMarkables.size()>0 || entireMarkables.size()>0)
        {
            // The deletee is the left or right border of or identical to at least one markable
            String message = "The base data element to be deleted is contained in the following markable(s):\n";
            if (entireMarkables.size()>0)
            {
                message = message + "Completely:\n";
            }
            for (int z=0;z<entireMarkables.size();z++)
            {
                    message = message + ((Markable)entireMarkables.get(z)).toString()+"\n";
            }
            
            if (startedMarkables.size()>0)
            {
                message = message + "As first element:\n";
            }
            for (int z=0;z<startedMarkables.size();z++)
            {
                    message = message + ((Markable)startedMarkables.get(z)).toString()+"\n";
            }
            
            if (endedMarkables.size()>0)
            {
                message = message + "As last element:\n";
            }
            for (int z=0;z<endedMarkables.size();z++)
            {
                    message = message + ((Markable)endedMarkables.get(z)).toString()+"\n";
            }
            message = message + "\nPress 'OK' to delete anyway and adapt/delete these markables, or 'Cancel' to cancel deletion!";
            // Show dialogue
            int choice = JOptionPane.showConfirmDialog(this.getMMAX2(),message,"Confirm base data deletion",JOptionPane.OK_CANCEL_OPTION,JOptionPane.INFORMATION_MESSAGE);           
            // Do sth. only if user pressed OK
            if (choice != JOptionPane.OK_OPTION)
            {
                return;