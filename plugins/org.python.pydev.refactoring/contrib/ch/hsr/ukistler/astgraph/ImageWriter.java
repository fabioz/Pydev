/******************************************************************************
* Copyright (C) 2006-2009  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package ch.hsr.ukistler.astgraph;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;

import javax.imageio.ImageIO;

public class ImageWriter implements Runnable {

    private BufferedImage img;

    private String fileName;

    public ImageWriter(BufferedImage img, String fileName) throws Throwable {
        if (img == null)
            throw new Exception("Image cannot be retrieved");
        this.img = img;
        this.fileName = fileName;
    }

    public void run() {
        FileOutputStream out;
        try {
            out = new FileOutputStream(new File(fileName));
            ImageIO.write(img, "png", out);
            out.flush();
            out.close();
        } catch (Throwable e) {

        }
    }

}
