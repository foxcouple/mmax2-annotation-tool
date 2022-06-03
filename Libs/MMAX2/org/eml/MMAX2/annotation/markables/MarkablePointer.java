
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