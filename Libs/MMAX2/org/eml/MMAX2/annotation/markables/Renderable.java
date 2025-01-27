
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
import java.awt.Graphics2D;

import org.eml.MMAX2.gui.document.MMAX2Document;

public interface Renderable 
{
    public void select(Graphics2D graphics, MMAX2Document doc, Markable currentlySelectedMarkable);
    public void unselect(MMAX2Document doc);
    public void refresh(Graphics2D graphics);
    public void updateLinePoints();
    public boolean isAmbient();
    public boolean containsMarkable(Markable markable);
    public void setIsPermanent(boolean status);
    public boolean getIsPermanent();
    public void setFlagLevel(int level);
    public MarkableRelation getMarkableRelation();

}
