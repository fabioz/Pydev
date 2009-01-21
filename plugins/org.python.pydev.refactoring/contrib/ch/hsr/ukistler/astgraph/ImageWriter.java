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
