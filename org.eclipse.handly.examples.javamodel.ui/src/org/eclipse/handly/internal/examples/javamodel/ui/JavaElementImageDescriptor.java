/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ondrej Ilcik (Codasip) - Handly example adaptation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * A {@link JavaElementImageDescriptor} consists of a base image and several adornments. The adornments
 * are computed according to the flags either passed during creation or set via the method
 *{@link #setAdornments(int)}.
 */
public class JavaElementImageDescriptor
    extends CompositeImageDescriptor
{
    public static final Point SMALL_SIZE = new Point(16, 16);

    /** Flag to render the abstract adornment. */
    public final static int ABSTRACT = 0x001;

    /** Flag to render the final adornment. */
    public final static int FINAL = 0x002;

    /** Flag to render the synchronized adornment. */
    public final static int SYNCHRONIZED = 0x004;

    /** Flag to render the static adornment. */
    public final static int STATIC = 0x008;

    /** Flag to render the 'implements' adornment. */
    public final static int IMPLEMENTS = 0x100;

    /** Flag to render the 'constructor' adornment. */
    public final static int CONSTRUCTOR = 0x200;

    /** Flag to render the 'volatile' adornment. */
    public final static int VOLATILE = 0x800;

    /** Flag to render the 'transient' adornment. */
    public final static int TRANSIENT = 0x1000;

    /** Flag to render the 'native' adornment. */
    public final static int NATIVE = 0x4000;

    private ImageDescriptor fBaseImage;
    private int fFlags;

    /**
     * Creates a new JavaElementImageDescriptor.
     *
     * @param baseImage an image descriptor used as the base image
     * @param flags flags indicating which adornments are to be rendered. See {@link #setAdornments(int)}
     * 	for valid values.
     * @param size the size of the resulting image
     */
    public JavaElementImageDescriptor(ImageDescriptor baseImage, int flags)
    {
        fBaseImage = baseImage;
        Assert.isNotNull(fBaseImage);
        fFlags = flags;
        Assert.isTrue(fFlags >= 0);
    }

    /**
     * Sets the descriptors adornments. Valid values are: {@link #ABSTRACT}, {@link #FINAL},
     * {@link #SYNCHRONIZED}, {@link #STATIC}, {@link #IMPLEMENTS}, {@link #CONSTRUCTOR},
     * {@link #VOLATILE}, {@link #TRANSIENT}, {@link #NATIVE}, or any combination of those.
     *
     * @param adornments the image descriptors adornments
     */
    public void setAdornments(int adornments)
    {
        Assert.isTrue(adornments >= 0);
        fFlags = adornments;
    }

    /**
     * Returns the current adornments.
     *
     * @return the current adornments
     */
    public int getAdronments()
    {
        return fFlags;
    }

    /**
     * Sets the size of the image created by calling {@link #createImage()}.
     *
     * @param size the size of the image returned from calling {@link #createImage()}
     */
    public void setImageSize(Point size)
    {
        Assert.isNotNull(size);
        Assert.isTrue(size.x >= 0 && size.y >= 0);
    }

    @Override
    protected Point getSize()
    {
        return SMALL_SIZE;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null
            || !JavaElementImageDescriptor.class.equals(object.getClass()))
            return false;

        JavaElementImageDescriptor other = (JavaElementImageDescriptor)object;
        return (fBaseImage.equals(other.fBaseImage) && fFlags == other.fFlags);
    }

    @Override
    public int hashCode()
    {
        return fBaseImage.hashCode() | fFlags;
    }

    @Override
    protected void drawCompositeImage(int width, int height)
    {
        ImageData bg = getImageData(fBaseImage);
        drawImage(bg, 0, 0);
        drawTopRight();
        drawBottomRight();
    }

    private ImageData getImageData(ImageDescriptor descriptor)
    {
        ImageData data = descriptor.getImageData(); // see bug 51965: getImageData can return null
        if (data == null)
        {
            data = DEFAULT_IMAGE_DATA;
            Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                "Image data not available: " + descriptor.toString())); //$NON-NLS-1$
        }
        return data;
    }

    private void addTopRightImage(ImageDescriptor desc, Point pos)
    {
        ImageData data = getImageData(desc);
        int x = pos.x - data.width;
        if (x >= 0)
        {
            drawImage(data, x, pos.y);
            pos.x = x;
        }
    }

    private void addBottomRightImage(ImageDescriptor desc, Point pos)
    {
        ImageData data = getImageData(desc);
        int x = pos.x - data.width;
        int y = pos.y - data.height;
        if (x >= 0 && y >= 0)
        {
            drawImage(data, x, y);
            pos.x = x;
        }
    }

    private void drawTopRight()
    {
        Point pos = new Point(getSize().x, 0);
        if ((fFlags & ABSTRACT) != 0)
        {
            addTopRightImage(Activator.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID, IJavaImages.IMG_OVR_ABSTRACT), pos);
        }
        if ((fFlags & CONSTRUCTOR) != 0)
        {
            addTopRightImage(Activator.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID, IJavaImages.IMG_OVR_CONSTRUCTOR), pos);
        }
        if ((fFlags & FINAL) != 0)
        {
            addTopRightImage(Activator.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID, IJavaImages.IMG_OVR_FINAL), pos);
        }
        if ((fFlags & VOLATILE) != 0)
        {
            addTopRightImage(Activator.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID, IJavaImages.IMG_OVR_VOLATILE), pos);
        }
        if ((fFlags & STATIC) != 0)
        {
            addTopRightImage(Activator.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID, IJavaImages.IMG_OVR_STATIC), pos);
        }
        if ((fFlags & NATIVE) != 0)
        {
            addTopRightImage(Activator.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID, IJavaImages.IMG_OVR_NATIVE), pos);
        }
    }

    private void drawBottomRight()
    {
        Point size = getSize();
        Point pos = new Point(size.x, size.y);

        int flags = fFlags;

        int syncAndImpl = SYNCHRONIZED | IMPLEMENTS;
        if ((flags & syncAndImpl) == syncAndImpl)
        { // both flags set: merged overlay image
            addBottomRightImage(
                Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                    IJavaImages.IMG_OVR_SYNCH_AND_IMPLEMENTS),
                pos);
            flags &= ~syncAndImpl; // clear to not render again
        }
        if ((flags & IMPLEMENTS) != 0)
        {
            addBottomRightImage(Activator.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID, IJavaImages.IMG_OVR_IMPLEMENTS), pos);
        }
        if ((flags & SYNCHRONIZED) != 0)
        {
            addBottomRightImage(Activator.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID, IJavaImages.IMG_OVR_SYNCH), pos);
        }

        // fields:
        if ((flags & TRANSIENT) != 0)
        {
            addBottomRightImage(Activator.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID, IJavaImages.IMG_OVR_TRANSIENT), pos);
        }
    }
}
