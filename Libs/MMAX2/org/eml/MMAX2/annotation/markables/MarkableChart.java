
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

package org.eml.MMAX2.annotation.markables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.xpath.NodeSet;
import org.eml.MMAX2.annotation.scheme.MMAX2Attribute;
import org.eml.MMAX2.api.AttributeAPI;
import org.eml.MMAX2.core.MMAX2;
import org.eml.MMAX2.discourse.MMAX2Discourse;
import org.eml.MMAX2.gui.display.MMAX2ActionSelector;
import org.eml.MMAX2.gui.display.MMAX2LevelSelector;
import org.eml.MMAX2.gui.display.MMAX2OneClickAnnotationSelector;
import org.eml.MMAX2.gui.display.MarkableLevelControlPanel;
import org.eml.MMAX2.gui.display.MarkableLevelRenderer;
import org.eml.MMAX2.gui.document.MMAX2Document;
import org.eml.MMAX2.gui.windows.MMAX2AttributePanelContainer;
import org.eml.MMAX2.gui.windows.MMAX2QueryWindow;
import org.eml.MMAX2.gui.windows.MarkableLevelControlWindow;
import org.eml.MMAX2.utils.MMAX2Constants;
import org.w3c.dom.Element;

public class MarkableChart 
{
    
    MMAX2ActionSelector selector = null;    
    MMAX2OneClickAnnotationSelector oneClickSelector = null;
        
    /** Reference to MarkableLayerControlPanel controlling this MarkableChart object. */
    public MarkableLevelControlPanel currentLevelControlPanel = null;        
    public MarkableLevelControlWindow currentLevelControlWindow = null;
    public MMAX2AttributePanelContainer attributePanelContainer = null;
    
    /** This HashMap contains all MarkableLevels in this MarkableChart, mapped to their level names as hash keys. */
    private HashMap<String, MarkableLevel> levels = null;   
    /** Array of MarkableLevels contained in this MarkableChart, in relevant order. */
    private MarkableLevel[] orderedLevels = null;
    /** Reference to MMAX2Discourse object that this MarkableChart belongs to. */
    private MMAX2Discourse currentDiscourse = null;    
    /** Number of MarkableLayers in this MarkableChart. */
    private int size = 0;

    private int nextFreeMarkableSetNum=0;
    private int nextFreeMarkableIDNum=0;
    
    private int selectionStart=0;
    private int selectionEnd=0;
    
    private int dotsDiscoursePosition=0;
    private int marksDiscoursePosition=0;
    
    /** Creates new MarkableChart */
    public MarkableChart(MMAX2Discourse _discourse)
    {
        currentLevelControlPanel = new MarkableLevelControlPanel(_discourse);
        currentLevelControlWindow = null;
        try { currentLevelControlWindow = new MarkableLevelControlWindow(currentLevelControlPanel); }
        catch (java.awt.HeadlessException ex) { }
        currentDiscourse = _discourse;
        levels = new HashMap<String, MarkableLevel>();
        orderedLevels = new MarkableLevel[0];
        size = 0;
        if (currentDiscourse.getHasGUI())  { attributePanelContainer = new MMAX2AttributePanelContainer(); }
    }

    public final int getSelectionStart()
    {
        return selectionStart;
    }
    
    public final int getSelectionEnd()
    {
        return selectionEnd;
    }

    
    public final void initAnnotationHints()
    {
        for (int z=0;z<size;z++)
        {
            ((MarkableLevel) orderedLevels[z]).getCurrentAnnotationScheme().setMMAX2(currentDiscourse.getMMAX2());
        }         
    }
    
    public final void saveAllMarkableLevels()
    {
        for (int z=0;z<size;z++)
        {
            ((MarkableLevel) orderedLevels[z]).saveMarkables("",false);
        } 
    }    

    /*
    public final void saveAllTransposedMarkableLevels(String oldLang, ArrayList absoluteWords)
    {
        for (int z=0;z<size;z++)
        {
            ((MarkableLevel) orderedLevels[z]).saveTransposedMarkables(oldLang,absoluteWords);
        } 
    }    
    */
    
    public final void updateAllMarkableLevels()
    {
        for (int z=0;z<this.size;z++)
        {
            ((MarkableLevel) orderedLevels[z]).updateMarkables();
        }  
    }    
    
    
    public final MMAX2Discourse getCurrentDiscourse()
    {
        return currentDiscourse;
    }
    
    public final Markable getMarkableByID(String nameSpacedID, MarkableLevel defaultLevel)
    {
        Markable result = null;
        if (nameSpacedID.indexOf(":")==-1)
        {
            result = defaultLevel.getMarkableByID(nameSpacedID);
        }
        else
        {
            String levelName = nameSpacedID.substring(0,nameSpacedID.indexOf(":"));
            String id = nameSpacedID.substring(nameSpacedID.indexOf(":")+1);
            result = getMarkableLevelByName(levelName, false).getMarkableByID(id);
        }                
        return result;
    }
    
    
    /** This method is called from the CaretListener when a caret event with unequal dot and mark values, i.e. a selection,
        has been detected. */
    public final void selectionOccurred(int dot, int mark)
    {        
        // Only if at least one level is available
        if (orderedLevels.length > 0)
        {
            MMAX2 mmax2 = this.currentDiscourse.getMMAX2();
            Rectangle tempRect = null;
        
            // Save position where mouse was released to display popup on
            int popUp=dot;
            if (dot > mark)
            {
                // Make sure dot is always smaller
                int temp=mark;
                mark=dot;
                dot=temp;                
            }
        
            mark--;
                    
            // Get some reference to mmax document
            MMAX2Document doc = this.orderedLevels[0].getCurrentDiscourse().getDisplayDocument();
        
            // Adjust dot and mark to cover fringe words completely
            String wordUnderDot = currentDiscourse.getDiscourseElementIDAtDiscoursePosition(currentDiscourse.getDiscoursePositionAtDisplayPosition(dot));
            while(wordUnderDot.equals("") && dot < mark)
            {
                dot++;                
                wordUnderDot = currentDiscourse.getDiscourseElementIDAtDiscoursePosition(currentDiscourse.getDiscoursePositionAtDisplayPosition(dot));
            }
            dotsDiscoursePosition = currentDiscourse.getDiscoursePositionAtDisplayPosition(dot);

            String wordUnderMark = currentDiscourse.getDiscourseElementIDAtDiscoursePosition(currentDiscourse.getDiscoursePositionAtDisplayPosition(mark));
            while(wordUnderMark.equals("") && mark > dot)
            {
                mark--;
                wordUnderMark = currentDiscourse.getDiscourseElementIDAtDiscoursePosition(currentDiscourse.getDiscoursePositionAtDisplayPosition(mark));
            }
            marksDiscoursePosition = currentDiscourse.getDiscoursePositionAtDisplayPosition(mark);
            if (wordUnderDot.equals("") || wordUnderMark.equals(""))
            {
                System.out.println("Invalid selection");
                return;
            }

            // Get start and end of selection in real display figures
            selectionStart = currentDiscourse.getDisplayStartPositionFromDiscoursePosition(dotsDiscoursePosition);
            selectionEnd = currentDiscourse.getDisplayStartPositionFromDiscoursePosition(marksDiscoursePosition);

            // Create shade
            doc.startChanges(selectionStart,(selectionEnd-selectionStart)+1);
            SimpleAttributeSet styleToUse = currentDiscourse.getMMAX2().getSelectionSpanStyle();
            StyleConstants.setFontSize(styleToUse, this.orderedLevels[0].getCurrentDiscourse().getMMAX2().currentDisplayFontSize);
            StyleConstants.setFontFamily(styleToUse, this.orderedLevels[0].getCurrentDiscourse().getMMAX2().currentDisplayFontName);        

            doc.bulkApplyStyleToDisplaySpanBackground(selectionStart, (selectionEnd-selectionStart)+1,styleToUse);
            doc.commitChanges();
        
            // Get current primary markable (if any)
            Markable currentPrimaryMarkable = currentDiscourse.getMMAX2().getCurrentPrimaryMarkable();
        
            if (currentPrimaryMarkable==null)
            {
                // No PrimaryMarkable currently selected, so only adding a new Markable to some level is possible
                String fragment = "";
            
                if (wordUnderDot.equals(wordUnderMark))
                {
                    fragment = wordUnderDot;
                }
                else
                {
                    fragment = wordUnderDot+".."+wordUnderMark;
                }
                MarkableLevel[] activeLevels = getActiveLevels();
                Markable newMarkable = null;
                if (activeLevels.length ==0)
                {
                    removeTemporarySelection();
                    return;
                }
                else if (activeLevels.length==1 && mmax2.getCreateSilently())
                {
                    newMarkable = ((MarkableLevel)activeLevels[0]).addMarkable(fragment);
                    if (getCurrentDiscourse().getMMAX2().getSelectAfterCreation())
                    {
                        markableLeftClicked(newMarkable);
                    }
                }
                else
                {
                    // Several levels are active, or silent creation is disabled, so use selector
                    MMAX2LevelSelector selector = new MMAX2LevelSelector(activeLevels, fragment, this);

                    // Some action is possible, so show selector                
                    try
                    {
                        tempRect = mmax2.getCurrentTextPane().modelToView(popUp);
                    }
                    catch (javax.swing.text.BadLocationException ex)
                    {
                        System.out.println("Error with display position determination of dot position "+dot);
                    }
                
                    int xPos = (int)tempRect.getX();
                    int yPos = (int)tempRect.getY()+MMAX2Constants.DEFAULT_FONT_SIZE+(MMAX2Constants.DEFAULT_FONT_SIZE/3);
                    int currentScreenWidth = mmax2.getScreenWidth();
                    int selectorWidth = selector.getWidth();
                    if ((xPos+mmax2.getX()) > currentScreenWidth/2)
                    {
                        xPos = xPos-selectorWidth;
                    }
                    selector.show(mmax2.getCurrentTextPane(),xPos,yPos );                        
                }            
            }
            else
            {
                // There is currently a PrimaryMarkable selected, so adding to its span and removing should be possible                
                ArrayList tempDEIDs = new ArrayList();
                int currentDiscPos = 0;
                String currentDEID = "";
                // Get List of DEs in  span
                // Iterate from dot to mark
                for (int z=dot;z<=mark;z++)
                {
                    // Get discpos at z
                    currentDiscPos = getCurrentDiscourse().getDiscoursePositionAtDisplayPosition(z);               
                    // Get de-id at discpos
                    currentDEID = getCurrentDiscourse().getDiscourseElementIDAtDiscoursePosition(currentDiscPos);
                    // Get DE element as well
                    // Collect ids and elements in list
                    if (currentDEID.equals("")==false)
                    {
                        if (tempDEIDs.contains(currentDEID)==false) 
                        {
                            tempDEIDs.add(currentDEID);
                        }
                    }
                }
                int mode =-1;
                // Get array representation of selected DEIDs 
                String[] modifiables = (String[])tempDEIDs.toArray(new String[0]);                
                
                // Get disc pos of first de in markable
                int currentMarkablesFirstDiscoursePosition = getCurrentDiscourse().getDiscoursePositionAtDisplayPosition(currentPrimaryMarkable.getLeftmostDisplayPosition());
                // Get disc pos of last de in markable
                int currentMarkablesLastDiscoursePosition = getCurrentDiscourse().getDiscoursePositionAtDisplayPosition(currentPrimaryMarkable.getRightmostDisplayPosition());
                // Determine relation of selection to current Markable, decide on mode accordingly
            
                // Get list of IDs of markable                
                ArrayList markablesIDs = new ArrayList(java.util.Arrays.asList(currentPrimaryMarkable.getDiscourseElementIDs()));
                if (markablesIDs.contains(modifiables[0])==false || markablesIDs.contains(modifiables[modifiables.length-1])==false)
                {
                    // The selection's first or last element is not part of markable, so assume adding
                    mode = MMAX2Constants.ADD_DES;
                }
                else
                {
                    // The selections first or last element (or both) is part of markable, so assume removing
                    mode = MMAX2Constants.REMOVE_DES;
                }
                