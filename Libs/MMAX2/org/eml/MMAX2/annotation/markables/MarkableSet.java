
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.HashSet;

import org.eml.MMAX2.annotation.query.MMAX2QueryTree;
import org.eml.MMAX2.api.MarkableSetAPI;
import org.eml.MMAX2.gui.document.MMAX2Document;
import org.eml.MMAX2.utils.MMAX2Constants;
import org.eml.MMAX2.utils.MMAX2Utils;

public class MarkableSet implements Renderable, MarkableSetAPI
{
    private MarkableRelation markableRelation;
    /** Name of the attribute value all Markables in this MarkableSet share (e.g. 'set_4'). */
    private String attributeValue;

    /** Whether this set is ordered or unordered. */
    private boolean ordered;
    
    /** Used only if this.ordered. */
    private HashSet<Markable> unorderedSet;
    /** Used only if !this.ordered. */
    private ArrayList<Markable> orderedSet;

    /** Number of Markables in this set, either ordered or unordered. */
    private int size;
    /** Width of the line drawn when rendering this set. */
    private int lineWidth;
        
    /** TODO: Support MAXSIZE */
    // For 'classic' rendering
    int[] X_points=null;
    int[] Y_points=null;
    
    Point[][] rects=null;
    
    private int leftMostPosition = Integer.MAX_VALUE;
    private int rightMostPosition = -1;            
    private boolean ambient = false;    // an ambient set is one that is rendered as a potential merge / adopt candidate 
    private boolean permanent = false;
    
    /** Creates a new MarkableSet for attributeValue _attributeValue (e.g. set_4), with ordering set to _ordered. */
    public MarkableSet(String _attributeValue, boolean _ordered, int _lineWidth, Color _color, int _lineStyle, MarkableRelation relation)
    {
        attributeValue = _attributeValue;
        ordered = _ordered;
        // Init appropriate list 
        if (ordered)  { orderedSet = new ArrayList<Markable>(); }
        else          { unorderedSet = new HashSet<Markable>(); }

        /** Set rendering attributes. */
        lineWidth = _lineWidth;
        markableRelation = relation;
    }
   
    public final void setIsPermanent(boolean _permanent)
    {
        permanent = _permanent;
    }
    
    public final boolean getIsPermanent()
    {
        return permanent;
    }
        
    public final void setAmbient(boolean status)
    {
        this.ambient = status;
    }
    
    public final boolean isAmbient()
    {
        return ambient;
    }
    
    /** Returns the value of the MARKABLE_SET-type MarkableRelation that this set belongs to, e.g. 'set_4'.*/
    public final String getAttributeValue()
    {
        return this.attributeValue;
    }
    
    public final void removeMarkable(Markable _markable)
    {
        if (!ordered)
        {
            unorderedSet.remove(_markable);
        }
        else
        {
            orderedSet.remove(_markable);
        }
        size--;
    }
    
    /** Adds a Markable to this MarkableSet. If this.ordered, the Markable will be inserted at the correct (document order) 
        position in this.orderedSet, otherwise it is just added to this.unorderedSet. Document order position is determined on 
        the basis of the displayStartPosition of the first fragment in the Markable. */
    public final void addMarkable(Markable _markable)
    {       
        if (_markable.getLeftmostDisplayPosition() < this.leftMostPosition)
        {
            this.leftMostPosition = _markable.getLeftmostDisplayPosition();
        }
        if (_markable.getRightmostDisplayPosition() > this.rightMostPosition)
        {
            this.rightMostPosition = _markable.getRightmostDisplayPosition();
        }
        
        boolean added = false;
        if (!ordered)
        {
            // Since this set is unordered, simply put Markable somewhere
            unorderedSet.add(_markable);
        }
        else
        {            
            // This set is ordered, so insert Markable at correct position
            if (size==0)
            {
                // This is still empty, so just add // This is unnecessary !!!
                orderedSet.add(_markable);
            }
            else
            {
                Markable markableAtCurrentPos=null;
                // Position is determined from displayStartPosition of first fragment
                // If two DEs have the same displayStartPosition, the shorter is inserted before the longer
                for (int o=0;o<size;o++)
                {
                    markableAtCurrentPos = (Markable) orderedSet.get(o);
                    if (_markable.getDisplayStartPositions()[0] < markableAtCurrentPos.getDisplayStartPositions()[0])
                    {
                        // If the addee starts earlier, add it before current one
                        orderedSet.add(o,_markable);
                        added = true;
                        break;
                    }
                    else if (_markable.getDisplayStartPositions()[0] == markableAtCurrentPos.getDisplayStartPositions()[0])
                    {
                        // Addee and current start at the same position
                        if (_markable.getSize() < markableAtCurrentPos.getSize())
                        {
                            // If addee is shorter, add it before current one
                            orderedSet.add(o,_markable);
                            added = true;
                            break;
                        }
                    }
                }
                if (!added) orderedSet.add(_markable);
            }
        }    
        // Increment size of this set
        size++;
    }
    
    public final void updateLinePoints()
    {
        updateLinePoints(false);
    }
    
    /** This method updates the cached XY-coordinates of the top left corners of each Markable in this MarkableSet. The method is called
        after display changes. */
    public final void updateLinePoints(boolean recalcExtent)
    {    	    	
        Point currentPoint=null;
        X_points = new int[size];
        Y_points = new int[size];

        // Store four ints for each elem in size