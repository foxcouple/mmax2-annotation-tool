
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
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.eml.MMAX2.api.MarkablePointerAPI;
import org.eml.MMAX2.gui.document.MMAX2Document;
import org.eml.MMAX2.utils.MMAX2Constants;
import org.eml.MMAX2.utils.MMAX2Utils;

public class MarkablePointer implements Renderable, MarkablePointerAPI
{
    private Markable sourceMarkable;
    private ArrayList<Markable> targetMarkables;
    private int lineWidth;
    private int size=0;
    private int leftMostPosition;
    private int rightMostPosition;
    private int X_origin=0;
    private int Y_origin=0;
    private int[] X_points=null;
    private int[] Y_points=null;

    Point[][] rects=null;
   
    private MarkableRelation markableRelation=null;        
    private int maxSize=-1;
    
    private boolean ambient = false;
    private boolean dashed=false;
    
    private boolean permanent = false;
    private int flagDisplayLevel=0;
    private float[] dash1;
    private float[] dash2;
    
    /** Creates new MarkablePointer */
    public MarkablePointer(Markable _sourceMarkable, int _lineWidth, Color _color, int _lineStyle, int _maxSize, MarkableRelation _relation, boolean _dashed) 
    {
        dash1 = new float[2];
        dash1[0] =10;
        dash1[1] =10;

        dash2 = new float[2];
        dash2[0] =4;
        dash2[1] =4;
        
        markableRelation = _relation;
        sourceMarkable = _sourceMarkable;
        lineWidth = _lineWidth;
        dashed = _dashed;
        leftMostPosition = sourceMarkable.getLeftmostDisplayPosition();
        rightMostPosition = sourceMarkable.getRightmostDisplayPosition();        
        targetMarkables = new ArrayList();                
        maxSize = _maxSize;
    }

    public final void setIsPermanent(boolean _permanent)
    {
        permanent = _permanent;
    }
    
    public final boolean getIsPermanent()
    {
        return permanent;
    }
    
    public final boolean hasMaxSize()
    {
        if (maxSize==-1)
        {
            return false;
        }
        else
        {
            return size==maxSize;
        }
    }
    
    public final String getTargetSpan()
    {
        String span = "";        
        Markable currentTarget = null;
        if (targetMarkables.size()==1)
        {
            currentTarget = ((Markable)targetMarkables.get(0));
            // If source and this target are from the same level, use only id
            if (currentTarget.getMarkableLevelName().equals(getSourceMarkable().getMarkableLevelName()))
            {
                span = currentTarget.getID();
            }
            else
            {
                // Prepend level name to target markable id
                span=currentTarget.getMarkableLevelName()+":"+currentTarget.getID();
            }
        }
        else if (targetMarkables.size()>1)
        {
            // Iterate over all target in this pointer set
            for (int z=0;z<targetMarkables.size();z++)
            {
                // Get current target
                currentTarget = ((Markable)targetMarkables.get(z));
                if (z==0)
                {
                    // If source and this target are from the ame level, use only id
                    if (currentTarget.getMarkableLevelName().equals(getSourceMarkable().getMarkableLevelName()))
                    {
                        span = currentTarget.getID();
                    }
                    else
                    {
                        // Prepend level name to target markable id
                        span=currentTarget.getMarkableLevelName()+":"+currentTarget.getID();
                    }
                }
                else
                {
                    // If source and this target are from the ame level, use only id
                    if (currentTarget.getMarkableLevelName().equals(getSourceMarkable().getMarkableLevelName()))
                    {
                        span = span+";"+currentTarget.getID();
                    }
                    else
                    {
                        // Prepend level name to target markable id
                        span=span+";"+currentTarget.getMarkableLevelName()+":"+currentTarget.getID();
                    }
                }
            }
        }        
        return span;
    }
    
    public final void setAmbient(boolean status)
    {
        this.ambient = status;
    }
    
    public final boolean isAmbient()
    {
        return ambient;
    }
       

    public final int getSize()
    {
        return size;
    }  

   
    ///

    public final MarkableRelation getMarkableRelation()
    {
        return markableRelation;
    }
        
    public final Markable[] getTargetMarkables()
    {
    	return (Markable[]) targetMarkables.toArray(new Markable[0]);
    }
    
    public final Markable getSourceMarkable()
    {
        return sourceMarkable;
    }
    
    public final boolean isSourceMarkable(Markable potentialSourceMarkable)
    {
        return sourceMarkable==potentialSourceMarkable;
    }
    
    public final boolean isTargetMarkable(Markable potentialTargetMarkable)
    {
        return targetMarkables.contains(potentialTargetMarkable);
    }

    public boolean containsMarkable(Markable markable) 
    {
        boolean result = false;
        if (markable == this.sourceMarkable)
        {
            result = true;
        }
        else if (this.targetMarkables.contains(markable))
        {
            result=true;
        }
        return result;
    }


    
    
    
    
    
    public final void removeTargetMarkable(Markable removee)
    {
        targetMarkables.remove(removee);
        size--;
    }
    
    
    
    public final void removeMeFromMarkableRelation()
    {
        markableRelation.removeMarkablePointer(this);
        size=0;
    }
     
    
    public final String toString()
    {
        if (size == 1)
        {
            return getSourceMarkable().toString()+" ["+size+" target]";
        }
        else
        {
            return getSourceMarkable().toString()+" ["+size+" targets]";
        }
    }
    
    