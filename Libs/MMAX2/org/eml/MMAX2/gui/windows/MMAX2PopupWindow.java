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

package org.eml.MMAX2.gui.windows;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
public class MMAX2PopupWindow extends javax.swing.JPopupMenu
{
    public MMAX2PopupWindow(final String string) 
    {    	
    	super();
    	BufferedImage before = null;
    	try {
    	    before = ImageIO.read(new File(string));
    	} catch (IOException e) {
    	}

//    	System.out.println(before.getWidth());
    	
    	double SCALE = 0.1;
    	BufferedImage after = new BufferedImage((int)(SCALE * before.getWidth(null)), (int) (SCA