/**
 * Copyright (c) 2014 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.tree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.python.pydev.core.log.Log;
import org.python.pydev.tree.EnabledTreeDragReorder.DragData;

class TreeItemDragDataTransfer extends ByteArrayTransfer {

    private static final TreeItemDragDataTransfer instance = new TreeItemDragDataTransfer();

    private static final String TYPE_NAME = "tree-item-transfer-format" + System.currentTimeMillis() + ":"
            + instance.hashCode();

    private static final int TYPEID = registerType(TYPE_NAME);

    private TreeItemDragDataTransfer() {
    }

    public static TreeItemDragDataTransfer getInstance() {
        return instance;
    }

    @Override
    protected int[] getTypeIds() {
        return new int[] { TYPEID };
    }

    @Override
    protected String[] getTypeNames() {
        return new String[] { TYPE_NAME };
    }

    @Override
    protected void javaToNative(Object object, TransferData transferData) {

        DragData dragData = (DragData) object;

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);

        byte[] bytes = null;

        if (dragData != null) {
            try {
                out.writeUTF(dragData.text);
                out.writeUTF(dragData.image);
                out.close();
                bytes = byteOut.toByteArray();
            } catch (IOException e) {
                Log.log(e);
            }

            if (bytes != null) {
                super.javaToNative(bytes, transferData);
            }
        }
    }

    @Override
    protected Object nativeToJava(TransferData transferData) {
        byte[] bytes = (byte[]) super.nativeToJava(transferData);
        if (bytes != null) {
            DataInputStream in = new DataInputStream(
                    new ByteArrayInputStream(bytes));

            try {
                String text = in.readUTF();
                String image = in.readUTF();
                return new DragData(text, image);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

}
