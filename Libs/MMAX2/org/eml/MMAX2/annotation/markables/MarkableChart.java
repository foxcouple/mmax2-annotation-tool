
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
